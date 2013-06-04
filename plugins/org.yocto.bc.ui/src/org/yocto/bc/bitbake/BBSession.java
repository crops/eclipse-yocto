/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer, 2013 Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.WorkbenchJob;

import org.yocto.bc.ui.model.IModelElement;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.YoctoCommand;

/**
 * BBSession encapsulates a global bitbake configuration and is the primary interface
 * for actions against a BitBake installation.
 * 
 * @author kgilmer
 *
 */
public class BBSession implements IBBSessionListener, IModelElement, Map {
	public static final int TYPE_VARIABLE_ASSIGNMENT = 1;
	public static final int TYPE_UNKNOWN = 2;
	public static final int TYPE_STATEMENT = 3;
	public static final int TYPE_FLAG = 4;
	
	public static final String BB_ENV_FILE = "bitbake.env";
	public static final String BUILDDIR_INDICATORS [] = {
		File.separatorChar + "conf" + File.separatorChar + "local.conf",
		File.separatorChar + "conf" + File.separatorChar + "bblayers.conf",
	};

	protected final ProjectInfo pinfo;
	protected final ShellSession shell;
	protected Map<?,?> properties = null;
	protected List <URI> depends = null;
	protected boolean initialized = false;
	protected boolean errorOccured = false;
	protected MessageConsole sessionConsole;
	private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
	private final Lock rlock = rwlock.readLock();
	private final Lock wlock = rwlock.writeLock();
	protected String parsingCmd;
	private boolean silent = false;
	private String errorLines = "";

	public BBSession(ShellSession ssession, URI projectRoot) throws IOException {
		shell = ssession;
		this.pinfo = new ProjectInfo();
		pinfo.setLocationURI(projectRoot);
		pinfo.setInitScriptPath(ProjectInfoHelper.getInitScriptPath(projectRoot));
		this.parsingCmd = "DISABLE_SANITY_CHECKS=\"1\" bitbake -e >& " + BB_ENV_FILE;
	}

	public BBSession(ShellSession ssession, URI projectRoot, boolean silent) throws IOException {
		this(ssession, projectRoot);
		this.silent = silent;
	}
	
	private Collection adapttoIPath(List<File> asList, IProject project) {

		List pathList = new ArrayList();

		for (Iterator i = asList.iterator(); i.hasNext();) {
			File f = (File) i.next();
			IFile ff = project.getFile(stripLeading(f.toString(), project.getLocationURI().getPath()));
			if (ff.exists()) {
				pathList.add(ff);
			}
		}

		return pathList;
	}
	
	private String appendAll(String[] elems, int st) {
		StringBuffer sb = new StringBuffer();

		for (int i = st; i < elems.length; ++i) {
			sb.append(elems[i]);
		}

		return sb.toString();
	}
	
	private int charCount(String trimmed, char c) {
		int i = 0;
		int p = 0;

		while ((p = trimmed.indexOf(c, p)) > -1) {
			i++;
			p++;
		}

		return i;
	}
	
	public void clear() {
		throw new RuntimeException("BB configuration is read-only.");
	}

	public boolean containsKey(Object arg0) {
		try {
			checkValidAndLock(true);
			return properties.containsKey(arg0);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally {
			rlock.unlock();
		}
	}

	public boolean containsValue(Object arg0) {
		try {
			checkValidAndLock(true);
			return properties.containsValue(arg0);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally {
			rlock.unlock();
		}
	}

	public Set entrySet() {
		try {
			checkValidAndLock(true);
			return properties.entrySet();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			rlock.unlock();
		}
	}

	@Override
	public boolean equals(Object arg0) {
		try {
			checkValidAndLock(true);
			return properties.equals(arg0);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally {
			rlock.unlock();
		}
	}

	public ShellSession getShell() {
		return shell;
	}

	public URI getProjInfoRoot() {
		return pinfo.getOriginalURI();
	}

	/**
	 * Recursively generate list of Recipe files from a root directory.
	 * 
	 * @param rootDir
	 * @param recipes
	 * @param fileExtension
	 * @param project
	 */
	private void findRecipes(File rootDir, List recipes, final String fileExtension, IProject project) {
		File[] children = rootDir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(fileExtension);
			}

		});

		if (children != null && children.length > 0) {
			recipes.addAll(adapttoIPath(Arrays.asList(children), project));
		}

		File[] childDirs = rootDir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}

		});

		if (childDirs != null && childDirs.length > 0) {
			for (int i = 0; i < childDirs.length; ++i) {
				findRecipes(childDirs[i], recipes, fileExtension, project);
			}
		}
	}

	private Collection findRecipes(List paths, IProject project) {
		List recipes = new ArrayList();

		for (Iterator i = paths.iterator(); i.hasNext();) {
			String rawPath = (String) i.next();
			String[] elems = rawPath.split("\\*/\\*");

			if (elems.length == 2) {

				File rootDir = new File(elems[0]);

				findRecipes(rootDir, recipes, elems[1], project);
			}
		}

		return recipes;
	}

	public Object get(Object arg0) {
		try {
			checkValidAndLock(true);
			return properties.get(arg0);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			rlock.unlock();
		}
	}

	private List getBitBakeKeywords() {
		return Arrays.asList(BBLanguageHelper.BITBAKE_KEYWORDS);
	}

	/**
	 * @return A MessageConsole for this BB session.
	 */
	public MessageConsole getConsole() {
		if (sessionConsole == null) {
			String cName = ProjectInfoHelper.getProjectName(pinfo.getOriginalURI()) + " Console";
			IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = conMan.getConsoles();
			for (int i = 0; i < existing.length; i++)
				if (cName.equals(existing[i].getName())) {
					sessionConsole = (MessageConsole) existing[i];
					break;
				}
			if (sessionConsole == null) {
				sessionConsole = new MessageConsole(cName, null);
				conMan.addConsoles(new IConsole[] { sessionConsole });
			}
		}
		
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(sessionConsole);
		
		return sessionConsole;
	}

	private int getLineType(String line) {

		if (line.contains("=")) {
			return TYPE_VARIABLE_ASSIGNMENT;
		}

		for (Iterator i = getBitBakeKeywords().iterator(); i.hasNext();) {
			if (line.startsWith((String) i.next())) {
				return TYPE_STATEMENT;
			}
		}

		if (line.contains(":")) {
			return TYPE_FLAG;
		}

		return TYPE_UNKNOWN;
	}

	public Collection getRecipeFiles(IProject project) {
		try {
			checkValidAndLock(true);
			if (!initialized) {
				throw new RuntimeException(this.getClass().getName() + " is not initialized.");
			}
			String bbfiles = (String) this.properties.get("BBFILES");
			List paths = parseBBFiles(bbfiles);
			return findRecipes(paths, project);
		} catch (Exception e) {
			return null;
		}
		finally {
			rlock.unlock();
		}
	}

	@Override
	public int hashCode() {
		try {
			checkValidAndLock(true);
			return properties.hashCode();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}finally {
			rlock.unlock();
		}
	}

	protected void checkExecuteError(String result, boolean hasErrors) {
		URI recipeURI = getDefaultDepends();
		String text = "Parsing " + ((recipeURI != null) ? ("recipe " + recipeURI) : "base configurations");
		if (hasErrors) {
			text = text + " ERROR!\n" + result;
		}else {
				text = text + " SUCCESS.\n";
		}
		if(!silent) {
			displayInConsole(text, -1, false);
		}
	}

	protected void displayInConsole(final String result, final int code, boolean clear) {
		MessageConsole  console = getConsole();
		final MessageConsoleStream info = console.newMessageStream();
		if(clear)
			console.clearConsole();
		new WorkbenchJob("Display parsing result") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if(code != 0) {
					info.setColor(JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR));
				}
				try {
					info.println(result);
					info.close();
				}catch (Exception e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private void checkValidAndLock(boolean rdlck) throws Exception {
		if(rdlck)
			rlock.lock();
		else
			wlock.lock();
		if(!initialized) {
			//upgrade lock manually
			if(rdlck) {
				rlock.unlock();
				wlock.lock();
			}
			try {
				if(!initialized) { //recheck
					boolean hasErrors = false;
					String result = shell.execute(parsingCmd, hasErrors);

					properties = parseBBEnvironment(result);

					if (properties.size() == 0) { // there was an error in sourcing bitbake environment
						shell.printError(errorLines);
						errorOccured = true;
					} else {
						errorLines = "";
						errorOccured = false;
						initialized = true;
					}
					RemoteHelper.handleRunCommandRemote(this.pinfo.getConnection(), new YoctoCommand("rm -rf " + result + BB_ENV_FILE, result, ""), new NullProgressMonitor());
				}
			} finally {
				//downgrade lock
				if(rdlck) {
					rlock.lock();
					wlock.unlock();
				}
			}
		}
		//not release lock
	}

	public void initialize() throws Exception {
		try {
			checkValidAndLock(false);
		}finally {
			wlock.unlock();
		}
	}

	private boolean isBlockEnd(String trimmed) {
		return charCount(trimmed, '}') > charCount(trimmed, '{');
		// return trimmed.indexOf('}') > -1 && trimmed.indexOf('{') == -1;
	}

	private boolean isBlockStart(String trimmed) {
		return charCount(trimmed, '{') > charCount(trimmed, '}');
		// return trimmed.indexOf('{') > -1 && trimmed.indexOf('}') == -1;
	}

	public boolean isEmpty() {
		try {
			checkValidAndLock(true);
			return properties.isEmpty();
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}finally {
			rlock.unlock();
		}
	}
	
	public Set keySet() {
		try {
			checkValidAndLock(true);
			return properties.keySet();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			rlock.unlock();
		}
	}

	protected void parse(String bbOutfilePath, Map outMap) throws Exception {
		IHost connection = shell.getProjectInfo().getConnection();
		InputStream is = RemoteHelper.getRemoteInputStream(connection, bbOutfilePath, BB_ENV_FILE, new NullProgressMonitor());
		RemoteHelper.getRemoteHostFile(connection, bbOutfilePath + BB_ENV_FILE, new NullProgressMonitor());
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		boolean inLine = false;
		StringBuffer sb = null;
		Stack blockStack = new Stack();

		while ((line = reader.readLine()) != null) {
			String trimmed = line.trim();
			if (trimmed.length() == 0 || line.startsWith("#")) {
				// weed out the blank and comment lines
				continue;
			}
			// Now we look for block start ends, and ignore all code within
			// blocks.
			if (isBlockStart(trimmed)) {
				blockStack.push(trimmed);
			} else if (isBlockEnd(trimmed)) {
				blockStack.pop();

			}

			if (!blockStack.isEmpty()) {
				// we are in a code block, continue until we break into global
				// scope.
				continue;
			}
			if (trimmed.endsWith("\\")) {
				if (!inLine) {
					inLine = true;
					sb = new StringBuffer(trimmed.substring(0, trimmed.length() - 1));
				} else {
					sb.append(trimmed.substring(0, trimmed.length() - 1));
				}
				// Only parse the line when we have the complete contents.
				continue;
			} else if (inLine) {
				inLine = false;
				line = sb.toString();
			}

			parseLine(line, outMap);
		}
	}
	
	private void parseAdditiveAssignment(String line, String operator, Map mo) throws Exception {
		String[] elems = splitAssignment(line, "\\+=");

		if (elems.length != 2) {
			throw new Exception("Unable to parse additive variable assignment in line: " + line);
		}

		if (!mo.containsKey(elems[0])) {
			mo.put(elems[0].trim(), elems[1]);
		} else {
			String existing = (String) mo.get(elems[0]);
			if (operator.equals("+=")) {
				mo.put(elems[0], existing + elems[1]);
			} else {
				mo.put(elems[0], elems[1] + existing);
			}
		}
	}

	protected URI getDefaultDepends() {
		return null;
	}
	
	protected Map parseBBEnvironment(String bbOutFilePath) throws Exception {
		Map env = new Hashtable();
		this.depends = new ArrayList<URI>();

		parse(bbOutFilePath, env);

		String included = (String) env.get("BBINCLUDED");
		if(getDefaultDepends() != null) {
			this.depends.add(getDefaultDepends());
		}
		if(included != null) {
			String[] includedSplitted = included.split(" ");
			for (String incl : includedSplitted){
				this.depends.add(new URI(incl));
			}
		}

		return env;
	}


	private List parseBBFiles(String bbfiles) {
		return Arrays.asList(bbfiles.split(" "));
	}
	
	//Map delegate methods 

	private void parseConditionalAssignment(String line, Map mo) throws Exception {
		String[] elems = splitAssignment(line, "\\?=");

		if (elems.length != 2) {
			throw new Exception("Unable to parse conditional variable assignment in line: " + line);
		}

		if (!mo.containsKey(elems[0].trim())) {
			mo.put(elems[0].trim(), elems[1].trim());
		}
	}

	private void parseImmediateAssignment(String line, String delimiter, Map mo) throws Exception {
		String[] elems = splitAssignment(line, delimiter);

		mo.put(elems[0], substitute(elems[1], mo));
	}

	private void parseKeyValue(String line, String delimiter, Map mo) throws Exception {
		String[] elems = splitAssignment(line, delimiter);

		mo.put(elems[0], elems[1]);
	}

	private void parseLine(String line, Map mo) throws Exception {

		switch (getLineType(line)) {
		case TYPE_VARIABLE_ASSIGNMENT:
			parseVariableAssignment(line, mo);
			break;
		case TYPE_STATEMENT:
		case TYPE_FLAG:
			// for now ignore statements
			break;
		case TYPE_UNKNOWN:
			// we'll gloss over unknown lines as well;
			break;
		default:
			throw new Exception("Unable to parse line: " + line);
		}
	}

	private void parseVariableAssignment(String line, Map mo) throws Exception {
		if (line.contains("?=")) {
			parseConditionalAssignment(line, mo);
		} else if (line.contains("+=")) {
			parseAdditiveAssignment(line, "+=", mo);
		} else if (line.contains("=+")) {
			parseAdditiveAssignment(line, "=+", mo);
		} else if (line.contains(":=")) {
			parseImmediateAssignment(line, ":=", mo);
		} else {
			parseKeyValue(line, "=", mo);
		}

	}

	private List parseVars(String line) {
		List l = new ArrayList();

		int i = 0;

		while ((i = line.indexOf("${", i)) > -1) {
			int i2 = line.indexOf("}", i);

			l.add(line.subSequence(i + 2, i2));
			i++;
		}

		return l;
	}

	public Object put(Object arg0, Object arg1) {
		throw new RuntimeException("BB configuration is read-only.");
	}

	public void putAll(Map arg0) {
		throw new RuntimeException("BB configuration is read-only.");
	}

	public Object remove(Object arg0) {
		throw new RuntimeException("BB configuration is read-only.");
	}

	private String removeQuotes(String line) {
		line = line.trim();

		if (line.startsWith("\"")) {
			line = line.substring(1);
		}

		if (line.endsWith("\"")) {
			line = line.substring(0, line.length() - 1);
		}

		return line;
	}

	public int size() {
		try {
			checkValidAndLock(true);
			return properties.size();
		}catch (Exception e) {
			e.printStackTrace();
			return 0;
		}finally {
			rlock.unlock();
		}
	}

	private String[] splitAssignment(String line, String seperator) throws Exception {
		String[] elems = line.split(seperator);

		if (elems.length < 2) {
			throw new Exception("Unable to parse assignment in line: " + line);
		} else if (elems.length == 2) {

			elems[0] = elems[0].trim(); // Clean up trailing or leading spaces.
			if (elems[0].startsWith("export ")) {
				elems[0] = elems[0].substring("export ".length()).trim();
			}
			elems[1] = removeQuotes(elems[1]); // Evaluate variables

			return elems;
		} else {
			String[] retVal = new String[2];

			retVal[0] = elems[0];
			if (retVal[0].startsWith("export ")) {
				retVal[0] = retVal[0].substring("export ".length()).trim();
			}
			retVal[1] = appendAll(elems, 1);

			return retVal;
		}
	}

	private String stripLeading(String target, String leading) {
		if (target.startsWith(leading)) {
			target = target.substring(leading.length());
		}

		return target;
	}

	/**
	 * Return a string with variable substitutions in place.
	 * 
	 * @param expression
	 * @return Input string with any substitutions from this file.
	 */
	public String substitute(String expression, Map mo) {

		List vars = parseVars(expression);

		for (Iterator i = vars.iterator(); i.hasNext();) {
			String varName = (String) i.next();
			String varToken = "${" + varName + "}";

			if (mo.containsKey(varName)) {
				expression = expression.replace(varToken, (String) mo.get(varName));
			} else if (System.getProperty(varName) != null) {
				expression = expression.replace(varToken, System.getProperty(varName));
			} else if (varName.toUpperCase().equals("HOME")) {
				expression = expression.replace(varToken, System.getProperty("user.home"));
			}
		}

		return expression;
	}

	public Collection values() {
		try {
			checkValidAndLock(true);
			return properties.values();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			rlock.unlock();
		}
	}

	public void changeNotified(IResource[] added, IResource[] removed, IResource[] changed) {
		wlock.lock();
		try {
			if (initialized && (removed != null || changed != null)) {
				for(int i=0;removed != null && i<removed.length;i++) {
					if (this.depends.contains(removed[i].getLocationURI())) {
						initialized = false;
						return;
					}
				}
				for(int i=0;changed != null && i<changed.length;i++) {
					if (this.depends.contains(changed[i].getLocation().toString())) {
						initialized = false;
						return;
					}
				}
			}
		}
		finally {
			wlock.unlock();
		}
	}
}

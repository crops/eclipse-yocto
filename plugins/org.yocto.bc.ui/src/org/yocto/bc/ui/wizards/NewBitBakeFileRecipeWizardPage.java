/*****************************************************************************
 * Copyright (c) 2013 Ken Gilmer, Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Jessica Zhang (Intel) - Extend to support auto-fill base on src_uri value
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.ui.wizards;

import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.remote.utils.ProcessStreamBuffer;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.YoctoCommand;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

public class NewBitBakeFileRecipeWizardPage extends WizardPage {
	private Text containerText;
	private Text fileText;
	
	private Text descriptionText;
	private Text licenseText;
	private Text checksumText;
	private Text homepageText;
	private Text authorText;
	private Text sectionText;
	private Text txtSrcURI;
	private Text md5sumText;
	private Text sha256sumText;
	private Button btnPopulate;

	private BitbakeRecipeUIElement element;
	
	private ISelection selection;
	private URI metaDirLoc;
	private ArrayList inheritance;
	private final IHost connection;

	private String tempFolderPath;
	private String srcFileNameExt;
	private String srcFileName;

	public static final String TEMP_FOLDER_NAME = "temp";
	public static final String TAR_BZ2_EXT = ".tar.bz2";
	public static final String TAR_GZ_EXT = ".tar.gz";
	public static final String HTTP = "http";
	public static final String FTP ="ftp";
	public static final String BB_RECIPE_EXT =".bb";
	private static final String COPYING_FILE = "COPYING";

	private static final String CMAKE_LIST = "cmakelists.txt";
	private static final String CMAKE = "cmake";
	private static final String SETUP_SCRIPT = "setup.py";
	private static final String DISUTILS = "disutils";
	private static final String CONFIGURE_IN = "configure.in";
	private static final String CONFIGURE_AC = "configure.ac";
	private static final String AUTOTOOLS = "autotools";

	private static final String md5Pattern = "^[0-9a-f]{32}$";
	protected static final String sha256Pattern = "^[0-9a-f]{64}$";

	private static final String MIRRORS_FILE = "mirrors.bbclass";
	private static final String CLASSES_FOLDER = "classes";

	private HashMap<String, String> mirrorTable;
	private URI extractDir;

	protected ProcessStreamBuffer md5Buffer;
	protected ProcessStreamBuffer sha256Buffer;
	protected ProcessStreamBuffer md5CopyingBuffer;

	public NewBitBakeFileRecipeWizardPage(ISelection selection, IHost connection) {
		super("wizardPage");
		setTitle("BitBake Recipe");
		setDescription("Create a new BitBake recipe.");
		this.selection = selection;
		this.connection = connection;
		element = new BitbakeRecipeUIElement();
		inheritance = new ArrayList();
	}

	public void createControl(Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText("Recipe &Directory:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button buttonBrowse = new Button(container, SWT.PUSH);
		buttonBrowse.setText("Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(container, containerText);
			}
		});
		
		label = new Label(container, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		label = new Label(container, SWT.NULL);
		label.setText("SRC_&URI:");

		txtSrcURI = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		txtSrcURI.setLayoutData(gd);
		txtSrcURI.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		btnPopulate = new Button(container, SWT.PUSH);
		btnPopulate.setText("Populate...");
		btnPopulate.setEnabled(false);
		btnPopulate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handlePopulate();
			}
		});

		createField(container, "&Recipe Name:", (fileText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "SRC_URI[&md5sum]:", (md5sumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "SRC_URI[&sha256sum]:", (sha256sumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "License File &Checksum:", (checksumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&Package Description:", (descriptionText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&License:", (licenseText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		
		createField(container, "&Homepage:", (homepageText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "Package &Author:", (authorText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&Section:", (sectionText = new Text(container, SWT.BORDER | SWT.SINGLE)));

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void createField(Composite container, String title, Text control) {
		Label label = new Label(container, SWT.NONE);
		label.setText(title);
		label.moveAbove(control);
	
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
		control.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}

		});
	}

	private void dialogChanged() {
		String containerName = containerText.getText();
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(containerName));
		String fileName = fileText.getText();

		if (containerName.length() == 0) {
			updateStatus("Directory must be specified");
			return;
		}
	
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		
		IProject project = container.getProject();
		ProjectInfo projInfo = null;
		try {
			projInfo = Activator.getProjInfo(project.getLocationURI());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		metaDirLoc = RemoteHelper.createNewURI(projInfo.getOriginalURI(), "meta");
	
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.contains(" ")) {
			updateStatus("File name must be valid with no space in it");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}

		if (descriptionText.getText().length() == 0) {
			updateStatus("Recipe must have a description");
			return;
		}
		
		if (licenseText.getText().length() == 0) {
			updateStatus("Recipe must have a license");
			return;
		}

		if (txtSrcURI.getText().length() == 0) {
			updateStatus("SRC_URI can't be empty");
		}
		
		updateStatus(null);
	}

	public BitbakeRecipeUIElement populateUIElement() {
		element.setAuthor(authorText.getText());
		element.setChecksum(checksumText.getText());
		element.setContainer(containerText.getText());
		element.setDescription(descriptionText.getText());
		element.setFile(fileText.getText());
		element.setHomePage(homepageText.getText());
		element.setLicense(licenseText.getText());
		element.setMd5sum(md5sumText.getText());
		element.setSection(sectionText.getText());
		element.setSha256sum(sha256sumText.getText());
		element.setSrcuri(txtSrcURI.getText());
		element.setInheritance(inheritance);
		element.setMetaDir(metaDirLoc);
		
		return element;
	}
	
	private void handleBrowse(final Composite parent, final Text text) {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select project directory");
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				text.setText(((Path) result[0]).toString());
			}
		}
	}
	
	private void handlePopulate() {
		try {
			IProgressMonitor monitor = new NullProgressMonitor();
			URI srcURI = new URI(txtSrcURI.getText().trim());
			String scheme = srcURI.getScheme();
			this.srcFileNameExt = getSrcFileName(true);
			this.srcFileName = getSrcFileName(false);
			if ((scheme.equals(HTTP) || scheme.equals(FTP))
					&& (srcFileNameExt.endsWith(TAR_GZ_EXT) || srcFileNameExt.endsWith(TAR_BZ2_EXT))) {
				handleRemotePopulate(srcURI, monitor);
			} else {
				String packageName = srcFileName.replace("-", "_");
				fileText.setText(packageName + BB_RECIPE_EXT);

				handleLocalPopulate(srcURI, monitor);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String getSrcFileName(boolean withExt){
		URI srcURI;
		try {
			srcURI = new URI(txtSrcURI.getText().trim());
			String path = srcURI.getPath();
			String fileName = path.substring(path.lastIndexOf("/") + 1);
			if (withExt)
				return fileName;
			else {
				if (fileName.endsWith(TAR_BZ2_EXT)) {
					return fileName.substring(0, fileName.indexOf(TAR_BZ2_EXT));
				} else if(fileName.endsWith(TAR_GZ_EXT)){
					return fileName.substring(0, fileName.indexOf(TAR_GZ_EXT));
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return "";
	}
	private void handleRemotePopulate(final URI srcURI, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		populateRecipeName();

		this.getContainer().run(true, true, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask("Populating recipe fields ... ", 100);
				try {
					String metaDirLocPath = metaDirLoc.getPath();
					monitor.subTask("Cleaning environment");
					YoctoCommand rmYCmd = new YoctoCommand("rm -rf " + TEMP_FOLDER_NAME, metaDirLocPath, "");
					RemoteHelper.handleRunCommandRemote(connection, rmYCmd, new SubProgressMonitor(monitor, 5));

					YoctoCommand mkdirYCmd = new YoctoCommand( "mkdir " + TEMP_FOLDER_NAME, metaDirLocPath, "");
					RemoteHelper.handleRunCommandRemote(connection, mkdirYCmd, new SubProgressMonitor(monitor, 5));

					updateTempFolderPath();
					monitor.worked(10);

					monitor.subTask("Downloading package sources");

					updateTempFolderPath();

					YoctoCommand wgetYCmd = new YoctoCommand("wget " + srcURI.toURL(), tempFolderPath, "");
					RemoteHelper.handleRunCommandRemote(connection, wgetYCmd, new SubProgressMonitor(monitor, 40));

					monitor.worked(50);

					monitor.subTask("Compute package checksums");
					String md5Cmd = "md5sum " + srcFileNameExt;
					YoctoCommand md5YCmd = new YoctoCommand(md5Cmd, tempFolderPath, "");

					RemoteHelper.handleRunCommandRemote(connection, md5YCmd, new SubProgressMonitor(monitor, 10));
					md5Buffer =  md5YCmd.getProcessBuffer();

					monitor.worked(60);

					String sha256Cmd = "sha256sum " + srcFileNameExt;
					YoctoCommand sha256YCmd = new YoctoCommand(sha256Cmd, tempFolderPath, "");
					RemoteHelper.handleRunCommandRemote(connection, sha256YCmd, new SubProgressMonitor(monitor, 10));
					sha256Buffer = sha256YCmd.getProcessBuffer();

					monitor.worked(70);

					monitor.subTask("Extracting package");
					extractDir = extractPackage(srcURI, new SubProgressMonitor(monitor, 0));
					monitor.worked(80);

					YoctoCommand licenseChecksumCmd = populateLicenseFileChecksum(extractDir, new SubProgressMonitor(monitor, 10));
					md5CopyingBuffer = 	licenseChecksumCmd.getProcessBuffer();

					monitor.subTask("Creating mirror lookup table");
					mirrorTable = createMirrorLookupTable(new SubProgressMonitor(monitor, 10));

					monitor.worked(90);
					monitor.done();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		updateSrcUri(mirrorTable, srcURI);
		populateInheritance(extractDir, monitor);

		String md5Val = md5Buffer.getOutputLineContaining(srcFileNameExt, md5Pattern);
		md5sumText.setText(Pattern.matches(md5Pattern,  md5Val) ? md5Val : "");
		String sha256Val = sha256Buffer.getOutputLineContaining(srcFileNameExt, sha256Pattern);
		sha256sumText.setText(Pattern.matches(sha256Pattern,  sha256Val) ? sha256Val : "");
		String checkSumVal =  md5CopyingBuffer.getOutputLineContaining(COPYING_FILE, md5Pattern);
		checksumText.setText(RemoteHelper.createNewURI(extractDir, COPYING_FILE).toString() + ";md5=" + (Pattern.matches(md5Pattern,  checkSumVal) ? checkSumVal : ""));
	}
	
	private void updateTempFolderPath(){
		this.tempFolderPath = getMetaFolderPath() + TEMP_FOLDER_NAME + "/";
	}

	private String getMetaFolderPath(){
		String sep = metaDirLoc.getPath().endsWith("/")? "" : "/";
		return metaDirLoc.getPath() + sep;
	}

	private void handleLocalPopulate(URI srcURI, IProgressMonitor monitor) {
		populateLicenseFileChecksum(srcURI, monitor);
		populateInheritance(srcURI, monitor);
	}

	private URI extractPackage(URI srcURI, IProgressMonitor monitor) {
		try {
			String path = srcFileNameExt;
			String tarCmd = "tar ";
			if (path.endsWith(TAR_BZ2_EXT)) {
				tarCmd += "-zxvf ";
			} else if(path.endsWith(TAR_GZ_EXT)){
				tarCmd += "-xvf ";
			}

			RemoteHelper.handleRunCommandRemote(connection, new YoctoCommand(tarCmd + path, tempFolderPath, ""), monitor);

			return RemoteHelper.createNewURI(metaDirLoc, TEMP_FOLDER_NAME + "/" + srcFileName);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void populateInheritance(URI extractDir, IProgressMonitor monitor) {
		IHostFile[] hostFiles = RemoteHelper.getRemoteDirContent(connection, metaDirLoc.getPath(), "", IFileService.FILE_TYPE_FILES, monitor);
		if (hostFiles == null)
			return;

		for (IHostFile file: hostFiles) {
			String fileName = file.getName();
			if (fileName.equalsIgnoreCase(CMAKE_LIST)){
				inheritance.add(CMAKE);
			} else if (fileName.equalsIgnoreCase(SETUP_SCRIPT)) {
				inheritance.add(DISUTILS);
			} else if (fileName.equalsIgnoreCase(CONFIGURE_AC) || file.getName().equalsIgnoreCase(CONFIGURE_IN)) {
				inheritance.add(AUTOTOOLS);
			}
		}
	}
	
	private YoctoCommand populateLicenseFileChecksum(URI extractDir, IProgressMonitor monitor) {
		if (extractDir == null)
			throw new RuntimeException("Something went wrong during source extraction!");

		try {
			YoctoCommand catCmd = new YoctoCommand("md5sum " + COPYING_FILE, extractDir.getPath(), "");
			RemoteHelper.handleRunCommandRemote(connection, catCmd, monitor);
			return catCmd;
		} catch (Exception e) {
			throw new RuntimeException("Unable to process file for MD5 calculation", e);
		}

	}

	private HashMap<String, String> createMirrorLookupTable(IProgressMonitor monitor) throws Exception {
		HashMap<String, String> mirrorMap = new HashMap<String, String>();

		YoctoCommand cmd = new YoctoCommand("cat " + MIRRORS_FILE, getMetaFolderPath() + CLASSES_FOLDER, "");
		RemoteHelper.handleRunCommandRemote(connection, cmd, monitor);

		if (!cmd.getProcessBuffer().hasErrors()){
			String delims = "[\\t]+";
			List<String> outputLines = cmd.getProcessBuffer().getOutputLines();
			for (String outLine : outputLines) {
				String[] tokens = outLine.split(delims);
				if (tokens.length < 2)
					continue;
				String endingStr = " \\n \\";
				int idx = tokens[1].lastIndexOf(endingStr);
				String key = tokens[1].substring(0, idx);
				mirrorMap.put(key, tokens[0]);
			}
		}
		return mirrorMap;
	}
	
	private void populateRecipeName() {
		String fileName = fileText.getText();
		if (!fileName.isEmpty())
			return;

		String recipeFile = srcFileName.replace("-", "_");
		recipeFile += BB_RECIPE_EXT;
		if (recipeFile != null)
			fileText.setText(recipeFile);
	}
	
	private void updateSrcUri(HashMap<String, String> mirrorsMap, URI srcUri) {
		Set<String> mirrors = mirrorsMap.keySet();
		Iterator<String> iter = mirrors.iterator();
		String mirror_key = null;
		String srcURL = srcUri.toString();

		while (iter.hasNext()) {
			String value = iter.next();
			if (srcURL.startsWith(value)) {
				mirror_key = value;
				break;
			}
		}

		if (mirror_key != null) {
			String replace_string = mirrorsMap.get(mirror_key);
			if (replace_string != null)
				srcURL = replace_string + srcURL.substring(mirror_key.length());
		}
		int idx = srcURL.lastIndexOf("-");
		String new_src_uri = srcURL.substring(0, idx)+"-${PV}" + TAR_GZ_EXT;
		txtSrcURI.setText(new_src_uri);
	}
	
	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}

package org.yocto.bc.ui.wizards.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;

import org.yocto.bc.bitbake.ICommandResponseHandler;
import org.yocto.bc.bitbake.ShellSession;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizard;

import org.yocto.bc.ui.wizards.newproject.BBConfigurationInitializeOperation;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;

/**
 * A wizard for installing a fresh copy of an OE system.
 * 
 * @author kgilmer
 * 
 * A Wizard for creating a fresh Yocto bitbake project and new poky build tree from git
 * 
 * @modified jzhang
 * 
 */
public class InstallWizard extends FiniteStateWizard implements
		IWorkbenchWizard {

	static final String KEY_PINFO = "KEY_PINFO";
	protected static final String OPTION_MAP = "OPTION_MAP";
	protected static final String INSTALL_SCRIPT = "INSTALL_SCRIPT";
	protected static final String INSTALL_DIRECTORY = "Install Directory";
	protected static final String INIT_SCRIPT = "Init Script";

	protected static final String PROJECT_NAME = "Project Name";
	protected static final String DEFAULT_INIT_SCRIPT = "oe-init-build-env";
	protected static final String DEFAULT_INSTALL_DIR = "~/yocto";
	
	protected static final String GIT_CLONE = "Git Clone";

	private Map model;
	private MessageConsole myConsole;

	public InstallWizard() {
		this.model = new Hashtable();
		model.put(INSTALL_DIRECTORY, DEFAULT_INSTALL_DIR);
		model.put(INIT_SCRIPT, DEFAULT_INIT_SCRIPT);
		
		setWindowTitle("Yocto BitBake Commander");
		setNeedsProgressMonitor(false);
		
		myConsole = findConsole("Yocto Console");
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		try {
			IConsoleView view = (IConsoleView) page.showView(id);
			view.display(myConsole);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	public InstallWizard(IStructuredSelection selection) {
		model = new Hashtable();
	}

	/*
	 * @Override public IWizardPage getNextPage(IWizardPage page) { if (page
	 * instanceof WelcomePage) { if (model.containsKey(WelcomePage.ACTION_USE))
	 * { return bbcProjectPage; } } else if (page instanceof ProgressPage) {
	 * return bitbakePage; }
	 * 
	 * if (super.getNextPage(page) != null) { System.out.println("next page: " +
	 * super.getNextPage(page).getClass().getName()); } else {
	 * System.out.println("end page"); }
	 * 
	 * return super.getNextPage(page); }
	 * 
	 * @Override public boolean canFinish() { System.out.println("can finish: "
	 * + super.canFinish()); return super.canFinish(); }
	 */
	@Override
	public void addPages() {
		addPage(new OptionsPage(model));
	}

	@Override
	public Map getModel() {
		return model;
	}

	@Override
	public boolean performFinish() {
		BCCommandResponseHandler cmdOut = new BCCommandResponseHandler(
				myConsole);
		
		WizardPage page = (WizardPage) getPage("Options");
		page.setPageComplete(true);
		Map options = (Map) model;
		String install_dir = "";
		if (options.containsKey(INSTALL_DIRECTORY)) {
			install_dir = (String) options.get(INSTALL_DIRECTORY);
		}

		if (((Boolean)options.get(GIT_CLONE)).booleanValue()) {
			String git_clone_cmd = "git clone git://git.pokylinux.org/poky.git "
				+ install_dir;
			cmdOut.printCmd(git_clone_cmd);
			executeCommand(cmdOut, git_clone_cmd);
		}
	
		if (!cmdOut.hasError()) {
			
			String initPath = install_dir + "/"
					+ (String) options.get(INIT_SCRIPT);
			String prjName = (String) options.get(PROJECT_NAME);
			ProjectInfo pinfo = new ProjectInfo();
			pinfo.setInitScriptPath(initPath);
			pinfo.setLocation(install_dir);
			pinfo.setName(prjName);
			
			try {
				ConsoleWriter cw = new ConsoleWriter();
				this.getContainer().run(false, false,
						new BBConfigurationInitializeOperation(pinfo, cw));
				
				myConsole.newMessageStream().println(cw.getContents());
			} catch (Exception e) {
				Activator
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								IStatus.ERROR, e.getMessage(), e));
				this.getContainer()
						.getCurrentPage()
						.setDescription(
								"Failed to create project: " + e.getMessage());
				return false;
			}
			
			model.put(InstallWizard.KEY_PINFO, pinfo);
			
			Activator.putProjInfo(pinfo.getRootPath(), pinfo);
			try {
				getContainer().run(false, false,
						new CreateBBCProjectOperation(pinfo));
			} catch (Exception e) {
				Activator
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
								IStatus.ERROR, e.getMessage(), e));
				this.getContainer()
						.getCurrentPage()
						.setDescription(
								"Failed to create project: " + e.getMessage());
				return false;
			}
			return true;
		}
		
		return false;
	}

	private void executeCommand(BCCommandResponseHandler cmdOut, String cmd) {

		try {
			ShellSession shell = new ShellSession(ShellSession.SHELL_TYPE_BASH,
					null, null, null);

			shell.execute(cmd, cmdOut);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	private class BCCommandResponseHandler implements ICommandResponseHandler {
		private MessageConsoleStream myConsoleStream;
		private Boolean errorOccured = false;

		public BCCommandResponseHandler(MessageConsole console) {
			try {
				this.myConsoleStream = console.newMessageStream();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void printDialog(String msg) {
			try {
				myConsoleStream.println(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Boolean hasError() {
			return errorOccured;
		}

		public void response(String line, boolean isError) {
			try {
				if (isError) {
					myConsoleStream.println(line);
					errorOccured = true;
				} else {
					myConsoleStream.println(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void printCmd(String cmd) {
			try {
				myConsoleStream.println(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class ConsoleWriter extends Writer {

		private StringBuffer sb;

		public ConsoleWriter() {
			sb = new StringBuffer();
		}

		@Override
		public void close() throws IOException {
		}

		public String getContents() {
			return sb.toString();
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			// txtConsole.getText().concat(new String(cbuf));
			sb.append(cbuf);
		}

		@Override
		public void write(String str) throws IOException {
			sb.append(str);
		}

	}

}

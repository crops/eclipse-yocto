package org.yocto.bc.ui.wizards.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
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
//import org.eclipse.cdt.core.CommandLauncher;
//import org.eclipse.cdt.core.ConsoleOutputStream;
//import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
//import org.eclipse.cdt.core.resources.IConsole;
//import org.eclipse.cdt.core.CCorePlugin;

import org.yocto.bc.bitbake.ICommandResponseHandler;
import org.yocto.bc.bitbake.ShellSession;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizard;
//import org.yocto.bc.ui.wizards.install.InstallJob.UICommandResponseHandler;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;

/**
 * A wizard for installing a fresh copy of an OE system.
 * 
 * @author kgilmer
 * 
 */
public class InstallWizard extends FiniteStateWizard implements IWorkbenchWizard {

	static final String KEY_PINFO = "KEY_PINFO";
	protected static final String OPTION_MAP = "OPTION_MAP";
	protected static final String INSTALL_SCRIPT = "INSTALL_SCRIPT";
	protected static final String INSTALL_DIRECTORY = "Install Directory";
	protected static final String INIT_SCRIPT = "Init Script";
	protected static final String DEFAULT_INIT_SCRIPT = "poky-init-build-env";
	protected static final String DEFAULT_INSTALL_DIR = "~/yocto";
	//protected static final String INSTALL_SCRIPT = "INSTALL_SCRIPT";
	//protected static final String INSTALL_DIRECTORY = "Install Directory";
	//protected static final String INSTALL_SCRIPT_FILE = "scripts/poky_install.sh";
	private Map model;
	private MessageConsole myConsole;

	public InstallWizard() {
		this.model = new Hashtable();
		model.put(INSTALL_DIRECTORY, DEFAULT_INSTALL_DIR);
		model.put(INIT_SCRIPT, DEFAULT_INIT_SCRIPT);
		//try {
		//	model.put(INSTALL_SCRIPT, InstallScriptHelper.loadFile("scripts/poky_install.sh"));
		//} catch (IOException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
		setWindowTitle("Yocto BitBake Commander");
		setNeedsProgressMonitor(false);
		//setDefaultPageImageDescriptor(Activator.getImageDescriptor("icons/OE_logo_96.png"));
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
    	//no console found, so create a new one
    	MessageConsole myConsole = new MessageConsole(name, null);
    	conMan.addConsoles(new IConsole[]{myConsole});
    	return myConsole;
    }

	public InstallWizard(IStructuredSelection selection) {
		model = new Hashtable();
	}

	/*@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof WelcomePage) {
			if (model.containsKey(WelcomePage.ACTION_USE)) {
				return bbcProjectPage;
			}
		} else if (page instanceof ProgressPage) {
			return bitbakePage;
		}
		
		if (super.getNextPage(page) != null) {
			System.out.println("next page: " + super.getNextPage(page).getClass().getName());
		} else {
			System.out.println("end page");
		}

		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		System.out.println("can finish: " + super.canFinish());
		return super.canFinish();
	}
*/
	@Override
	public void addPages() {
	//	flavorPage = new FlavorPage(model);
	//	bitbakePage = new BitbakePage(model);
	//	bbcProjectPage = new BBCProjectPage(model);
		//addPage(new WelcomePage(model));
		//JZaddPage(new FlavorPage(model));
		addPage(new OptionsPage(model));
		//addPage(new ProgressPage(model));
		//addPage(bbcProjectPage);
		//addPage(new BitbakePage(model));
	}

	@Override
	public Map getModel() {
		return model;
	}

	@Override
	public boolean performFinish() {
		BCCommandResponseHandler cmdOut = new BCCommandResponseHandler(myConsole);
		//Map options = (Map)model.get(OptionsPage.OPTION_MAP);
		Map options = (Map)model;
		String install_dir;
		if (options.containsKey(INSTALL_DIRECTORY)) {
			install_dir = (String)options.get(INSTALL_DIRECTORY);
			System.out.println(install_dir);
		}
		
		executeCommand(cmdOut);
		WizardPage page = (WizardPage) getPage("Options");
		page.setPageComplete(true);
		
		if (!cmdOut.hasError()) {
		ProjectInfo pinfo = (ProjectInfo) model.get(KEY_PINFO);
		Activator.putProjInfo(pinfo.getRootPath(), pinfo);
		try {
			getContainer().run(false, false, new CreateBBCProjectOperation(pinfo));
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription("Failed to create project: " + e.getMessage());
			return false;
		}
		}
		return true;
	}

	private void executeCommand(BCCommandResponseHandler cmdOut) {
		
		try {
			ShellSession shell = new ShellSession(ShellSession.SHELL_TYPE_BASH, null, null, null);
			//Map options = (Map)model.get(OptionsPage.OPTION_MAP);
			Map options = (Map)model;
			//shell.execute("bash ~/workspace/plugins/org.yocto.bc.ui/scripts/poky_install.sh ~/poky-test", cmdOut);
			shell.execute("bash /scripts/poky_install.sh ~/poky-test", cmdOut);
			/*
			while ((line = reader.readLine()) != null && !errorOccurred) {
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#")) {
					line = substitute(line, vars);
					cmdOut.printCmd(line);
					shell.execute(line, cmdOut);
				} else if (line.startsWith("#")) {
					cmdOut.printDialog(line.substring(1).trim());
				}
			}

			if (errorOccurred) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to install OpenEmbedded");
			}*/
		} catch (IOException e) {
			e.printStackTrace();
			//return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to install OpenEmbedded", e);
		}
		//return Status.OK_STATUS;
	}
		
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
	
	private class BCCommandResponseHandler implements ICommandResponseHandler {
		private MessageConsoleStream myConsoleStream;
		private  Boolean errorOccured = false;

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
}

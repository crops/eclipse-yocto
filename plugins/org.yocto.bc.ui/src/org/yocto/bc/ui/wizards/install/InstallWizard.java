/*******************************************************************************
 * Copyright (c) 2013 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 * Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.ui.wizards.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizard;
import org.yocto.bc.ui.wizards.newproject.BBConfigurationInitializeOperation;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;
import org.yocto.remote.utils.ICommandResponseHandler;

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
	public static final String VALIDATION_FILE = DEFAULT_INIT_SCRIPT;

	private Map<String, Object> model;
	private MessageConsole myConsole;

	public InstallWizard() {
		this.model = new Hashtable<String, Object>();
		model.put(INSTALL_DIRECTORY, DEFAULT_INSTALL_DIR);
		model.put(INIT_SCRIPT, DEFAULT_INIT_SCRIPT);
		
		setWindowTitle("Yocto Project BitBake Commander");
		setNeedsProgressMonitor(true);
		myConsole = findConsole("Yocto Project Console");
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
		model = new Hashtable<String, Object>();
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
	public Map<String, Object> getModel() {
		return model;
	}

	@Override
	public boolean performFinish() {
		BCCommandResponseHandler cmdOut = new BCCommandResponseHandler(
				myConsole);

		WizardPage page = (WizardPage) getPage("Options");
		page.setPageComplete(true);
		Map<String, Object> options = (Map<String, Object>) model;
		String install_dir = "";
		if (options.containsKey(INSTALL_DIRECTORY)) {
			install_dir = (String) options.get(INSTALL_DIRECTORY);
		}

		try {
			if (!cmdOut.hasError()) {

				String initPath = install_dir + "/"
						+ (String) options.get(INIT_SCRIPT);
				String prjName = (String) options.get(PROJECT_NAME);
				ProjectInfo pinfo = new ProjectInfo();
				pinfo.setInitScriptPath(initPath);
				pinfo.setLocation(install_dir);
				pinfo.setName(prjName);

				ConsoleWriter cw = new ConsoleWriter();
				this.getContainer().run(false, false,
						new BBConfigurationInitializeOperation(pinfo, cw));

				myConsole.newMessageStream().println(cw.getContents());

				model.put(InstallWizard.KEY_PINFO, pinfo);
				Activator.putProjInfo(pinfo.getRootPath(), pinfo);

				this.getContainer().run(false, false,
						new CreateBBCProjectOperation(pinfo));
				return true;
			}
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription(
							"Failed to create project: " + e.getMessage());
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	private interface ICalculatePercentage {
		public float calWorkloadDone(String info) throws IllegalArgumentException;
	}

	@SuppressWarnings("unused")
	private class LongtimeRunningTask implements IRunnableWithProgress {
		private String []cmdArray;
		private String []envp;
		private File dir;
		private ICommandResponseHandler handler;
		private Process p;
		private String taskName;
		static public final int TOTALWORKLOAD=100;
		private int reported_workload;
		ICalculatePercentage cal;

		public LongtimeRunningTask(String taskName,
				String []cmdArray, String []envp, File dir,
				ICommandResponseHandler handler,
				ICalculatePercentage calculator) {
			this.taskName=taskName;
			this.cmdArray=cmdArray;
			this.envp=envp;
			this.dir=dir;
			this.handler=handler;
			this.p=null;
			this.cal=calculator;
		}

		private void reportProgress(IProgressMonitor monitor,String info) {
			if(cal == null) {
				monitor.worked(1);
			}else {
				float percentage;
				try {
					percentage=cal.calWorkloadDone(info);
				} catch (IllegalArgumentException e) {
					//can't get percentage
					return;
				}
				int delta=(int) (TOTALWORKLOAD * percentage - reported_workload);
				if( delta > 0 ) {
					monitor.worked(delta);
					reported_workload += delta;
				}
			}
		}

		synchronized public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {

			boolean cancel=false;
			reported_workload=0;

			try {
				monitor.beginTask(taskName, TOTALWORKLOAD);

				p=Runtime.getRuntime().exec(cmdArray,envp,dir);
				BufferedReader inbr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader errbr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String info;
				while (!cancel) {
					if(monitor.isCanceled())
					{
						cancel=true;
						throw new InterruptedException("User Cancelled");
					}

					info=null;
					//reading stderr
					while (errbr.ready()) {
						info=errbr.readLine();
						//some application using stderr to print out information
						handler.response(info, false);
					}
					//reading stdout
					while (inbr.ready()) {
						info=inbr.readLine();
						handler.response(info, false);
					}

					//report progress
					if(info!=null)
						reportProgress(monitor,info);

					//check if exit
					try {
						int exitValue=p.exitValue();
						if (exitValue != 0) {
							handler.response(
									taskName + " failed with the return value " + new Integer(exitValue).toString(),
									true);
						}
						break;
					}catch (IllegalThreadStateException e) {
					}

					Thread.sleep(500);
				}
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
				if (p != null ) {
					p.destroy();
				}
			}
		}
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

		@SuppressWarnings("unused")
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

		@SuppressWarnings("unused")
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

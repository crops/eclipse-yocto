/*******************************************************************************
 * Copyright (c) 2011 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.ui.wizards.importProject;

import java.io.IOException;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizard;

import org.yocto.bc.ui.wizards.newproject.BBConfigurationInitializeOperation;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;

public class ImportYoctoProjectWizard extends FiniteStateWizard  implements IImportWizard {
	protected final static String KEY_OEROOT = "OEROOT";
	public static final String KEY_NAME = "NAME";
	public static final String KEY_LOCATION = "LOCATION";
	public static final String KEY_INITPATH = "INITPATH";
	protected static final String KEY_PINFO = "PINFO";
	
	private Map<String, Object> projectModel;
	private MessageConsole myConsole;
	
	public ImportYoctoProjectWizard() {
		projectModel = new Hashtable<String, Object>();
	}
	
	public Map<String, Object> getModel() {
		return projectModel;
	}
	
	@Override
	public void addPages() {
		addPage(new BBCProjectPage(projectModel));
		//addPage(new ConsolePage(projectModel));
	}

	
	public boolean performFinish() {
		ProjectInfo pinfo = new ProjectInfo();
		pinfo.setInitScriptPath((String) projectModel.get(ImportYoctoProjectWizard.KEY_INITPATH));
		pinfo.setLocation((String) projectModel.get(ImportYoctoProjectWizard.KEY_LOCATION));
		pinfo.setName((String) projectModel.get(ImportYoctoProjectWizard.KEY_NAME));
		
		try {
			ConsoleWriter cw = new ConsoleWriter();
			this.getContainer().run(false, false, new BBConfigurationInitializeOperation(pinfo, cw));
			myConsole.newMessageStream().println(cw.getContents());
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription("Failed to create project: " + e.getMessage());
			//valid = false;
			//setPageComplete(valid);
			return false;
		} 
		
		//valid = true;
		projectModel.put(ImportYoctoProjectWizard.KEY_PINFO, pinfo);
		//setPageComplete(valid);
		//ProjectInfo pinfo = (ProjectInfo) projectModel.get(KEY_PINFO);
		Activator.putProjInfo(pinfo.getRootPath(), pinfo);
		try {
			getContainer().run(false, false, new CreateBBCProjectOperation(pinfo));
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription("Failed to create project: " + e.getMessage());
			return false;
		} 
		
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.setNeedsProgressMonitor(true);
		setWindowTitle("BitBake Commander Project");
		
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
				//txtConsole.getText().concat(new String(cbuf));
				sb.append(cbuf);
			}
			
			@Override
			public void write(String str) throws IOException {
				sb.append(str);
			}
			
		}
	
}

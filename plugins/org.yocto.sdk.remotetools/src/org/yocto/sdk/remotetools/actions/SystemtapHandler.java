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
package org.yocto.sdk.remotetools.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;
import org.yocto.sdk.remotetools.RSEHelper;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.terminals.ui.TerminalServiceHelper;
import org.eclipse.rse.internal.terminals.ui.views.RSETerminalConnector;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewTab;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewer;
import org.eclipse.rse.internal.terminals.ui.views.TerminalsUI;
import org.eclipse.rse.services.terminals.ITerminalShell;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.elements.TerminalElement;
import org.eclipse.rse.ui.SystemBasePlugin;

public class SystemtapHandler extends TerminalHandler {
	//protected SystemtapSettingDialog setting;
	protected String changeTerm="export TERM=vt100;";
	protected IWorkbenchWindow window;
	
	protected String remote_KO_file;
	protected static String remote_KO_file_loc = "/tmp/";
	protected static String stap_cmd = "staprun ";
	
	protected void preProcess() {
		IHost host = setting.getHost();
		String KO_value = ((SystemtapSettingDialog)setting).getKernelModule();
		Path KO_file_path = new Path(KO_value);
		remote_KO_file=remote_KO_file_loc+KO_file_path.lastSegment();
		
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		SystemtapModel op=new SystemtapModel(host,KO_value,window.getShell().getDisplay());
		try {
			op.preProcess(new NullProgressMonitor());
			//progressService.busyCursorWhile(op);
		}catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(window.getShell(),
					"Systemtap",
					e.getMessage());
		}
	}
	
	protected String getInitCmd() {
		return stap_cmd+remote_KO_file+"\r";
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		setting=new SystemtapSettingDialog(
				window.getShell()
				);
	}

}

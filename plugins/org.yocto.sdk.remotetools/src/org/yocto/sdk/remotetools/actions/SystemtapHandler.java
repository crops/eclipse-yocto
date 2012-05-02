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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.yocto.sdk.remotetools.RSEHelper;

public class SystemtapHandler extends TerminalHandler {
	//protected SystemtapSettingDialog setting;
	protected String changeTerm="export TERM=vt100;";
	protected IWorkbenchWindow window;
	
	protected String remote_KO_file;
	protected static String remote_KO_file_loc = "/tmp/";
	protected static String stap_cmd = "staprun ";
	
	protected boolean preProcess(final ITerminalServiceSubSystem terminalSubSystem) {
		IHost host = setting.getHost();
		String KO_value = ((SystemtapSettingDialog)setting).getKernelModule();
		Path KO_file_path = new Path(KO_value);
		remote_KO_file=remote_KO_file_loc+KO_file_path.lastSegment();
		
		if (terminalSubSystem != null) {
			if (super.preProcess(terminalSubSystem)) {
		
				SystemtapModel op=new SystemtapModel(host,KO_value,window.getShell().getDisplay());
				try {
					op.preProcess(new NullProgressMonitor());
					return true;
					//progressService.busyCursorWhile(op);
				}catch (Exception e) {
					e.printStackTrace();
					MessageDialog.openError(window.getShell(),
							"Systemtap",
							e.getMessage());
				}
			}
		}
		return false;
	}
	
	protected String getInitCmd() {
		return stap_cmd+remote_KO_file+"\r";
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SystemtapSettingDialog(
				shell
				);
	}

}

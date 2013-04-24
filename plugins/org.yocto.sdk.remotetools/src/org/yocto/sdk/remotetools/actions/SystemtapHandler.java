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
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;

public class SystemtapHandler extends AbstractHandler {
	protected SystemtapSettingDialog setting;
	protected String changeTerm="export TERM=vt100;";
	protected IWorkbenchWindow window;
	protected Shell shell;
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SystemtapSettingDialog(
				shell, "Systemtap"
				);
		
		setting.open();
		String metadata_location = ((SystemtapSettingDialog)setting).getMetadataLocation();
		String remote_host = ((SystemtapSettingDialog)setting).getRemoteHost();
		String user_id = ((SystemtapSettingDialog)setting).getUserID();
		String systemtap_script = ((SystemtapSettingDialog)setting).getSystemtapScript();
		String systemtap_args = ((SystemtapSettingDialog)setting).getSystemtapArgs();

		if(setting.open() == BaseSettingDialog.OK) {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			SystemtapModel op = new SystemtapModel(metadata_location,remote_host, user_id, systemtap_script,
					systemtap_args,window.getShell().getDisplay());
			try {
				progressService.busyCursorWhile(op);
			}catch (InterruptedException e) {
				//user cancelled
			}catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(window.getShell(),
										"Systemtap",
										e.getMessage());
			}
		}
		return false;
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SystemtapSettingDialog(
				shell, "Systemtap"
				);
	}

}

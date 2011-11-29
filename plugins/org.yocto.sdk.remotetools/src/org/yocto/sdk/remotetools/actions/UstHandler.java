/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;

import org.yocto.sdk.remotetools.Messages;

public class UstHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if(UstModel.checkAvail()!=true) {
			return null;
		}
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		UstSettingDialog setting=new UstSettingDialog(
				window.getShell()
				);
		
		if(setting.open()==BaseSettingDialog.OK) {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			String project = setting.getProject();
			if (project == null) {
				MessageDialog.openError(window.getShell(), "Lttng-ust", Messages.ErrorUstProject);
				return null;
			}
			UstModel op=new UstModel(setting.getHost(),setting.getApplication(),setting.getArgument(), project, window);
			try {
				progressService.busyCursorWhile(op);
			}catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(window.getShell(),
						"Ust",
						e.getMessage());
			}
		}
		return null;
	}
}

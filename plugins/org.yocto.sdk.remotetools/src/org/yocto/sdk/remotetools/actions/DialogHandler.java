/*******************************************************************************
 * Copyright (c) 2013 Intel Corporation.
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
import org.eclipse.rse.core.model.IHost;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

abstract public class DialogHandler extends TerminalHandler {

	protected BaseSettingDialog setting;

	abstract protected String getDialogTitle();

	protected void initialize(ExecutionEvent event) throws ExecutionException{
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting = new SimpleSettingDialog(shell, getDialogTitle(), getConnnectionName());
	}

	@Override
	public void execute(ExecutionEvent event) throws ExecutionException {
		initialize(event);

		if(setting.open() == BaseSettingDialog.OK) {
			IHost currentHost = setting.getHost();
			execute(currentHost);
		}
	}
}

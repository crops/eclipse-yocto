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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class PerfHandler extends TerminalHandler {

	private static String initCmd="cd; perf\r";
	
	protected String getInitCmd() {
		return initCmd;
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SimpleSettingDialog(
				shell,
				"Perf",
				IBaseConstants.CONNECTION_NAME_LATENCYTOP
				);
	}

}

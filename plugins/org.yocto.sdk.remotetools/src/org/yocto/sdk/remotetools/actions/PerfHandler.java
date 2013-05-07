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


public class PerfHandler extends TerminalHandler {

	private static String initCmd="cd; perf\r";
	
	@Override
	protected String getInitCmd() {
		return initCmd;
	}
	@Override
	protected String getConnnectionName() {
		return IBaseConstants.CONNECTION_NAME_PERF;
	}
	@Override
	protected String getDialogTitle() {
		return IBaseConstants.DIALOG_TITLE_PERF;
	}
}

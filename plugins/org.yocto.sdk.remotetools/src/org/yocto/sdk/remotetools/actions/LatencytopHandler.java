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


public class LatencytopHandler extends DialogHandler {
	
	protected String changeTerm = "export TERM=xterm;";
	private static String initCmd="export PATH=$PATH:/usr/local/sbin:/usr/sbin:/sbin; cd; latencytop\r";
	
	@Override
	protected String getInitCmd() {
		return initCmd;
	}
	@Override
	protected String changeTerm() {
		return changeTerm;
	}
	@Override
	protected String getConnnectionName() {
		return IBaseConstants.CONNECTION_NAME_LATENCYTOP;
	}
	@Override
	protected String getDialogTitle() {
		return IBaseConstants.DIALOG_TITLE_LATENCYTOP;
	}
}

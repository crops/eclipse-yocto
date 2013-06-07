/*******************************************************************************
 * Copyright (c) 2013 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ioana Grigoropol(Intel) - initial API and implementation
 *******************************************************************************/
package org.yocto.remote.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.services.shells.IHostShell;

public class CommandOutputProcessor extends OutputProcessor {

	public CommandOutputProcessor(IProgressMonitor monitor,
			IHostShell hostShell, CommandResponseHandler cmdHandler, String task) {
		super(monitor, hostShell, cmdHandler, task);
	}

	@Override
	protected boolean isErrChStop(char ch) {
		return (ch == '\n');
	}

	@Override
	protected boolean isOutChStop(char ch) {
		return (ch == '\n');
	}

	@Override
	protected void processOutputBufferLine(char ch, String str) {
		processBuffer.addOutputLine(str);
	}

	@Override
	protected void processErrorBufferLine(char ch, String str) {
		processBuffer.addErrorLine(str);
	}

}

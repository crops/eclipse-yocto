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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.shells.IHostShell;

public class CommandRunnable implements Runnable{
	private IHostShell hostShell;
	private final IHost connection;
	private final YoctoCommand cmd;
	private final IProgressMonitor monitor;
	private final CommandResponseHandler cmdHandler;

	CommandRunnable(IHost connection, YoctoCommand cmd, IProgressMonitor monitor){
		this.connection = connection;
		this.cmdHandler = RemoteHelper.getCommandHandler(connection);
		this.cmd = cmd;
		this.monitor = monitor;
		this.hostShell = null;
	}
	@Override
	public void run() {
		try {
			hostShell = RemoteHelper.runCommandRemote(connection, cmd, monitor);
			cmd.setProcessBuffer(RemoteHelper.processOutput(monitor, hostShell, cmdHandler));
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

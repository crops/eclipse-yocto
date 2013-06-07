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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.Lock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.internal.services.local.shells.LocalHostShell;
import org.eclipse.rse.internal.services.shells.TerminalServiceHostShell;
import org.eclipse.rse.services.shells.HostShellProcessAdapter;
import org.eclipse.rse.services.shells.IHostShell;

public abstract class OutputProcessor{
	private static final int ERROR_BUFFER = 1;
	private static final int OUTPUT_BUFFER = 2;
	protected String task;
	protected ProcessStreamBuffer processBuffer;
	protected IHostShell hostShell;
	protected CommandResponseHandler cmdHandler;
	protected IProgressMonitor monitor;

	public OutputProcessor(IProgressMonitor monitor, IHostShell hostShell, CommandResponseHandler cmdHandler, String task){
		this.monitor = monitor;
		this.hostShell = hostShell;
		this.processBuffer = new ProcessStreamBuffer(hostShell instanceof TerminalServiceHostShell);
		this.cmdHandler = cmdHandler;
		this.task = task;
	}
	public ProcessStreamBuffer processOutput() throws Exception{
		if (hostShell == null)
			throw new Exception("An error has occured while trying to run remote command!");
		monitor.beginTask(this.task, RemoteHelper.TOTALWORKLOAD);
		Lock lock = null;
		if (hostShell instanceof LocalHostShell) {
			lock = ((LocalHostShell)hostShell).getLock();
			lock.lock();
		}
		BufferedReader inbr = null;
		BufferedReader errbr = null;

		if (hostShell instanceof LocalHostShell) {
			inbr = ((LocalHostShell)hostShell).getReader(false);
			errbr = ((LocalHostShell)hostShell).getReader(true);
		} else {
			Process p = new HostShellProcessAdapter(hostShell);
			inbr = new BufferedReader(new InputStreamReader(p.getInputStream()));
			errbr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		}
		boolean cancel = false;
		while (!cancel) {
			if(monitor.isCanceled()) {
				cancel = true;
				if (lock != null)
					lock.unlock();
				throw new InterruptedException("User Cancelled");
			}
			processBuffer(errbr, ERROR_BUFFER);
			processBuffer(inbr, OUTPUT_BUFFER);
			cancel = true;
		}
		if (lock != null)
			lock.unlock();
		return processBuffer;
	}
	protected abstract boolean isErrChStop(char ch);
	protected abstract boolean isOutChStop(char ch);
	protected boolean isChStop(char ch, int type){
		if (type == ERROR_BUFFER)
			return isErrChStop(ch);
		else if(type == OUTPUT_BUFFER)
			return isOutChStop(ch);
		return false;
	}
	protected abstract void processOutputBufferLine(char ch, String str);
	protected abstract void processErrorBufferLine(char ch, String str);
	protected void processBufferLine(String str, char ch, int type){
		if (type == ERROR_BUFFER)
			processErrorBufferLine(ch, str);
		else if(type == OUTPUT_BUFFER)
			processOutputBufferLine(ch, str);
	}
	protected void processBuffer(BufferedReader br, int type) throws IOException{
		StringBuffer buffer = new StringBuffer();
		int c;
		if (br != null)
		while ((c = br.read()) != -1) {
			char ch = (char) c;
			buffer.append(ch);
			if (isChStop(ch, type)){
				String str = buffer.toString();
				processBufferLine(str, ch, type);
				System.out.println(str);
				if (str.trim().equals(RemoteHelper.TERMINATOR)) {
					break;
				}
				cmdHandler.response(str, false);
				buffer.delete(0, buffer.length());
			}
		}
	}
}
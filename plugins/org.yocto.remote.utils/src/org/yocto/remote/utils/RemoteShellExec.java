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
package org.yocto.remote.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;

public class RemoteShellExec {

	public static final int
    STATE_NULL = 0,
    STATE_RUNNING = 1,
    STATE_EXITED = 2;

	private final String command;
	private final IHost host;

	private InputStream fInStream;
    private OutputStream fOutStream;
    private InputStream fErrStream;
    private Process remoteShellProcess;

	private int exitCode = 0;
	private int status = STATE_NULL;

	private final String RETURN_VALUE_TAG = "org.yocto.sdk.remotetools.RVTAG";
	private final String RETURN_VALUE_CMD = ";echo \"" + RETURN_VALUE_TAG + "$?\"";

	public RemoteShellExec(IHost host, String command) {
		assert(host != null);
		this.host = host;
		this.command = command;
	}

	public int getStatus() {
		return status;
	}

	public int getExitCode() {
		return exitCode;
	}

	private void reset() {
		fInStream = null;
		fOutStream = null;
		fErrStream = null;

		remoteShellProcess = null;
		exitCode = 0;
		status = STATE_NULL;
	}

	public InputStream getInputStream() {
        return fInStream;
    }

    public OutputStream getOutputStream() {
        return fOutStream;
    }

    public InputStream getErrStream() {
        return fErrStream;
    }

	public synchronized void start(String prelaunchCmd, String argument, IProgressMonitor monitor) throws Exception {
		if(status == STATE_RUNNING)
				return;

		reset();
		argument = (argument == null ? RETURN_VALUE_CMD : argument + RETURN_VALUE_CMD);
		remoteShellProcess = RemoteHelper.remoteShellExec(this.host, prelaunchCmd, this.command, argument, monitor);
		fInStream = remoteShellProcess.getInputStream();
		fOutStream = remoteShellProcess.getOutputStream();
		fErrStream = remoteShellProcess.getErrorStream();
		status = STATE_RUNNING;
	}

	 public synchronized void terminate() throws Exception {
		 if(status != STATE_RUNNING || remoteShellProcess != null)
			 return;

		 remoteShellProcess.destroy();
		 reset();
	 }

	 public int waitFor(IProgressMonitor monitor) throws InterruptedException {
		 while(status == STATE_RUNNING) {
			 if(monitor != null) {
	    			if(monitor.isCanceled()) {
	    				throw new InterruptedException("User Cancelled");
	    			}
 			 }

			 try {
				 remoteShellProcess.waitFor();
			 }catch(InterruptedException e){
				 //get the return value
				 try {
					 if(fInStream.available() != 0) {
						 BufferedReader in = new BufferedReader(new InputStreamReader(fInStream));
						 String thisline;
						 int idx;
						 while((thisline = in.readLine()) != null) {
							    if(thisline.indexOf(RETURN_VALUE_CMD) == -1) {
									idx = thisline.indexOf(RETURN_VALUE_TAG);
									if(idx != -1) {
										try {
											exitCode=(new Integer(thisline.substring(idx+RETURN_VALUE_TAG.length()))).intValue();
										}catch(NumberFormatException e2) {
										}
										break;
									}
							    }
						 }
					 }
				 }catch(IOException e1) {
					 //do nothing
				 }
			 }finally {
				 status=STATE_EXITED;
			 }
		 }
		 return exitCode;
	 }
}


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
package org.yocto.sdk.remotetools.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.rse.core.model.IHost;
import org.yocto.sdk.remotetools.RSEHelper;

public class RemoteApplication {
	
	public static final int
    STATE_NULL = 0,
    STATE_RUNNING = 1,
    STATE_EXITED = 2;
	
	private String directory;
	private String command;
	private String []environment;
	private IHost target;
	
	private InputStream fInStream;
    private OutputStream fOutStream;
    private InputStream fErrStream;
    private Process remoteShellProcess;
	
	private int exit_code=0;
	private int status=STATE_NULL;
	
	private String RETURN_VALUE_TAG = "org.yocto.sdk.remotetools.RVTAG";
	private String RETURN_VALUE_CMD = ";echo \"" + RETURN_VALUE_TAG + "$?\"";
	
	public RemoteApplication(IHost target,
			String directory,
			String command,
			String[] environment) {
		assert(target!=null);
		this.target = target;
		this.directory=directory;
		this.command=command;
		this.environment=environment;
	}
	
	public int getStatus()
	{
		return status;
	}
	
	public int getExitCode()
	{
		return exit_code;
	}
	
	private void reset() {
		fInStream=null;
		fOutStream=null;
		fErrStream=null;
		
		remoteShellProcess=null;
		exit_code=0;
		status=STATE_NULL;
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
		if(status==STATE_RUNNING)
				return;
	
		reset();
		remoteShellProcess = RSEHelper.remoteShellExec(this.target, prelaunchCmd, this.command, argument==null?RETURN_VALUE_CMD:argument+RETURN_VALUE_CMD, monitor);
		fInStream = remoteShellProcess.getInputStream();
		fOutStream = remoteShellProcess.getOutputStream();
		fErrStream = remoteShellProcess.getErrorStream();
		status=STATE_RUNNING;
	}
	
	 public synchronized void terminate() throws Exception {
		 
		 if(status != STATE_RUNNING || remoteShellProcess != null)
			 return;
		 
		 remoteShellProcess.destroy();
		 reset();
	 }
	 
	 public int waitFor(IProgressMonitor monitor) throws InterruptedException {
		 while(status==STATE_RUNNING) {
			 if(monitor!=null) {
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
						 BufferedReader in=new BufferedReader(new InputStreamReader(fInStream));
						 String thisline;
						 int idx;
						 while((thisline=in.readLine()) != null) {
							    if(thisline.indexOf(RETURN_VALUE_CMD)==-1) {
									idx=thisline.indexOf(RETURN_VALUE_TAG);
									if(idx != -1) {
										try {
											exit_code=(new Integer(thisline.substring(idx+RETURN_VALUE_TAG.length()))).intValue();
										}catch(NumberFormatException e2) {
											//
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
		 return exit_code;
	 }
}


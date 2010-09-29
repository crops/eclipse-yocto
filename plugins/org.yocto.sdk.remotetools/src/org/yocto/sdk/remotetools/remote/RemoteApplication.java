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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tm.tcf.protocol.IToken;
import org.eclipse.tm.tcf.services.IStreams;
import org.eclipse.tm.tcf.services.IProcesses;
import org.eclipse.tm.tcf.util.TCFTask;

public class RemoteApplication {

	public static final int
    STATE_NULL = 0,
    STATE_RUNNING = 1,
    STATE_EXITED = 2;
	
	private String directory;
	private String file;
	
	private IProcesses process;
	private IStreams stream;
	private InputStream fInStream;
    private OutputStream fOutStream;
    private InputStream fErrStream;
	private IProcesses.ProcessContext context;
	private int exit_code=0;
	private int status=STATE_NULL;
	private IProcesses.ProcessesListener listener; 
	private Exception res= new Exception();
	
	private class ProcListener implements IProcesses.ProcessesListener
	{
		public void exited(String process_id, int exit_code)
		{
			if(!context.getID().equals(process_id))
				return;
			process.removeListener(listener);
			synchronized (RemoteApplication.this.res) {
				RemoteApplication.this.exit_code=exit_code;
				RemoteApplication.this.status=STATE_EXITED;
				res.notifyAll();
			}
		}
	};
	
	public RemoteApplication(RemoteTarget target,
			String directory,
			String file) {
		assert(target!=null && file!=null && !file.isEmpty());
		process=target.getProcessesService();
		stream=target.getStreamsService();
		this.directory=directory;
		this.file=file;
	}
	
	private void reset() {
		fInStream=null;
		fOutStream=null;
		fErrStream=null;
		
		context=null;
		exit_code=0;
		status=STATE_NULL;
	}
	
	public int getStatus()
	{
		return status;
	}
	
	public int getExitCode()
	{
		return exit_code;
	}
	
	public synchronized void start( final String[] command_line, 
            final Map<String,String> environment) throws Exception
	{
		synchronized(res) {
			if(status==STATE_RUNNING)
				return;
		}
		try {
			reset();
			new TCFTask <Object>() {
				public void run() {
					listener = new ProcListener();
					process.addListener(listener);
					process.start(directory, 
								file, 
								command_line, 
								environment, 
								false, 
								new IProcesses.DoneStart() {
						public void doneStart(IToken token, Exception error, 
								IProcesses.ProcessContext process) {
							if (error != null) {
	                            error(error);
	                            return;
	                        }
							synchronized (res) {
								RemoteApplication.this.context=process;
								RemoteApplication.this.status=STATE_RUNNING;
							}
							done(this);
						}
					});
				}
			}.get();
		}catch (Exception e) {
			 e.printStackTrace();
			 throw e;
		}
		
		
		String in_id=(String)context.getProperties().get(IProcesses.PROP_STDOUT_ID);
		if(in_id!=null && !in_id.isEmpty()) {
			fInStream=new RemoteInputStream(stream,in_id);
		}
		String out_id=(String)context.getProperties().get(IProcesses.PROP_STDIN_ID);
		if(out_id!=null && !out_id.isEmpty()) {
			fOutStream=new RemoteOutputStream(stream,out_id);
		}
		String err_id=(String)context.getProperties().get(IProcesses.PROP_STDERR_ID);
		if(err_id!=null && !err_id.isEmpty() && !err_id.equals(in_id)) {
			fErrStream=new RemoteInputStream(stream,err_id);
		}
			
		return;
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
    
    public synchronized void terminate() throws Exception {
    	
    	if(fInStream!=null) { 
			fInStream.close();
			fInStream=null;
		}
		if(fOutStream!=null) {
			fOutStream.close();
			fOutStream=null;
		}
		if(fErrStream!=null) {
			fErrStream.close();
			fOutStream=null;
		}
		
    	synchronized (res) {
    		if(RemoteApplication.this.status!=STATE_RUNNING || 
    				RemoteApplication.this.context==null)
			{
				return;
			}
    	}
    	
    	new TCFTask <Object> () {
    		public void run () {
				RemoteApplication.this.context.terminate(new IProcesses.DoneCommand() {
					public void doneCommand(IToken token, Exception error) {
						if(error!=null) error(error);
						else done(this);
					}
				});
    		}
    	}.get();
    }
    
    public int waitFor(IProgressMonitor monitor) throws InterruptedException {
    	
    	synchronized (res) {
    		while (status==STATE_RUNNING) {
    			if(monitor!=null) {
	    			if(monitor.isCanceled())
	    				throw new InterruptedException("User Cancelled");
    			}
    			res.wait(500);
    		}
    		return exit_code;
    	}
    }
}

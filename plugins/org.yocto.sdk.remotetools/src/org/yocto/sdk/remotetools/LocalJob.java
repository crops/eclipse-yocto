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

package org.yocto.sdk.remotetools;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTException;

public class LocalJob extends Job {
	
	public static final String LOCAL_JOB_FAMILY = "localJobFamily";
	private String[] cmdarray;
	private String[] envp;
	private File dir;
	private int exitValue;
	private Exception exception;
	private IWorkbenchWindow window;
	
	public LocalJob(String name, String[] cmdarray, String[] envp, File dir, IWorkbenchWindow window) {
		super(name);
		this.cmdarray=cmdarray;
		this.envp=envp;
		this.dir=dir;
		this.window=window;
		this.exitValue=0;
		this.exception=null;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Process p=null;
		boolean cancel=false;
		
		try {
			//start process
			p=Runtime.getRuntime().exec(cmdarray,envp,dir);
			
			//wait for completion
			while (!cancel) {
	
				if(monitor.isCanceled()) 
					cancel=true;
				
				try {
					exitValue=p.exitValue();
					break;
				}catch (IllegalThreadStateException e) {
				}
				
				Thread.sleep(500);
			}
			
		}catch (IOException e) {
			exception=e;	
		}catch (InterruptedException e){
			cancel=true;
		}finally {
			if(p!=null)
				p.destroy();
		}
		try {
			if(exitValue!=0 || exception!=null) {
				window.getWorkbench().getDisplay().syncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(window.getShell(),
								Messages.LocalJob_Title,
								Messages.ErrorLocalJob + ": " + getName() + 
								(exitValue!=0 ? "\n\tExit value: " + new Integer(exitValue).toString() : new String("")) + 
								(exception!=null ? "\n\t" + exception.getMessage() : new String (""))
								);
					}
				});
			}
		}catch (SWTException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return (cancel!=true) ? Status.OK_STATUS : Status.CANCEL_STATUS;
	}
	
	public boolean belongsTo(Object family) {
         return family == LOCAL_JOB_FAMILY;
    }
}

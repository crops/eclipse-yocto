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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.model.IHost;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.RemoteShellExec;

abstract public class BaseModel implements IRunnableWithProgress {
	protected IHost host;
	protected String taskName;
	protected String localScript;
	protected String remoteExec;
	protected String localFile;
	protected String remoteFile;

	private static final int WORKLOAD = 100;

	private static final int INIT_PERCENT = 5;
	private static final int PRE_PROCESS_PERCENT = 30;
	private static final int PROCESS_PERCENT = 30;
	private static final int POST_PROCESS_PERCENT = 30;
	private static final int CLEAN_PERCENT = 5;

	private static final String RUN_MSG = "Running task: ";
	private static final String INIT_MSG = "Initializing ";
	private static final String PRE_PROCESS_MSG = "Preparing ";
	private static final String PROCESS_MSG = "Processing ";
	private static final String POST_PROCESS_MSG = "Finishing ";
	private static final String CLEAN_MSG = "Cleaning ";
	private static final String DOTS = "...";
	private static final String FAILED_ERR_MSG = " failed with exit code ";

	public void preProcess(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException{
		//upload script to remote
		try {
			RemoteHelper.putRemoteFileInPlugin(host, localScript, remoteExec, monitor);
		}catch (InterruptedException e){
			throw e;
		}catch (InvocationTargetException e) {
			throw e;
		}catch (Exception e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
	}
	public void postProcess(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException{}

	public void setHost (IHost host) {
		this.host = host;
	}
	abstract public void process(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException;
	
	public BaseModel(IHost host, String taskName, String localScript, String remoteExec) {
		this.host = host;
		this.taskName = taskName;
		this.localScript = localScript;
		this.remoteExec = remoteExec;
	}
	protected void init(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	}
	
	protected void clean(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
    InterruptedException {
	
		try {
			monitor.beginTask(RUN_MSG + taskName, WORKLOAD);

			monitor.subTask(INIT_MSG + taskName + DOTS);
			init(SubMonitor.convert(monitor, INIT_PERCENT));

			monitor.subTask(PRE_PROCESS_MSG + taskName + DOTS);
			preProcess(SubMonitor.convert(monitor, PRE_PROCESS_PERCENT));

			monitor.subTask(PROCESS_MSG + taskName + DOTS);
			process(SubMonitor.convert(monitor, PROCESS_PERCENT));

			monitor.subTask(POST_PROCESS_MSG + taskName + DOTS);
			postProcess(SubMonitor.convert(monitor, POST_PROCESS_PERCENT));
		} catch (InterruptedException e){
			throw new InterruptedException("User cancelled!");
		} catch (InvocationTargetException e) {
			throw e;
		} finally {
			monitor.subTask(CLEAN_MSG + taskName + DOTS);
			clean(SubMonitor.convert(monitor, CLEAN_PERCENT));
			monitor.done();
		}
	}

	protected void getDataFile(IProgressMonitor monitor) throws Exception {
		RemoteHelper.getRemoteFile( host, localFile, remoteFile, monitor);
	}
	protected void runRemoteShellExec(IProgressMonitor monitor, String args, boolean cancelable) throws Exception {
		try {
			RemoteShellExec exec = new RemoteShellExec(host, remoteExec);
			exec.start(null, args, monitor);
			monitor.worked(1);
			checkTerminate(exec.getInputStream());
			int exit_code = exec.waitFor(cancelable ? monitor : null);
			exec.terminate();
			if(exit_code != 0)
				throw new Exception(taskName + FAILED_ERR_MSG + new Integer(exit_code).toString());
		} finally {
			monitor.done();
		}
	}
	protected void checkTerminate(InputStream inputStream) throws IOException {}
}

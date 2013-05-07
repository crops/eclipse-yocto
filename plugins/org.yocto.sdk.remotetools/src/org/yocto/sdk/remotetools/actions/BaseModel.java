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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.model.IHost;
import org.yocto.sdk.remotetools.RSEHelper;

abstract public class BaseModel implements IRunnableWithProgress {
	protected IHost host;
	protected String taskName;
	protected String localScript;
	protected String remoteExec;

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

	public void preProcess(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException{
		//upload script to remote
		try {
			RSEHelper.putRemoteFileInPlugin(host, localScript, remoteExec, monitor);
		}catch (InterruptedException e){
			throw e;
		}catch (InvocationTargetException e) {
			throw e;
		}catch (Exception e) {
			throw new InvocationTargetException(e, e.getMessage());
		}
	}
	public void postProcess(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException{}

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
			init(new SubProgressMonitor(monitor, INIT_PERCENT));

			monitor.subTask(PRE_PROCESS_MSG + taskName + DOTS);
			preProcess(new SubProgressMonitor(monitor, PRE_PROCESS_PERCENT));

			monitor.subTask(PROCESS_MSG + taskName + DOTS);
			process(new SubProgressMonitor(monitor, PROCESS_PERCENT));

			monitor.subTask(POST_PROCESS_MSG + taskName + DOTS);
			postProcess(new SubProgressMonitor(monitor, POST_PROCESS_PERCENT));
		} catch (InterruptedException e){
			throw new InterruptedException("User cancelled!");
		} catch (InvocationTargetException e) {
			throw e;
		} finally {
			monitor.subTask(CLEAN_MSG + taskName + DOTS);
			clean(new SubProgressMonitor(monitor, CLEAN_PERCENT));
			monitor.done();
		}
	}

}

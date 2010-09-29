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

import org.yocto.sdk.remotetools.remote.RemoteTarget;

abstract public class BaseModel implements IRunnableWithProgress {
	
	protected IHost rseConnection;
	protected RemoteTarget target;

	abstract public void preProcess(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException;
	abstract public void postProcess(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException;
	abstract public void process(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException;
	
	public BaseModel(IHost rseConnection) {
		this.rseConnection=rseConnection;
		target=null;
	}
	
	protected void init(IProgressMonitor monitor) throws InvocationTargetException {
		if(rseConnection==null) {
			throw new InvocationTargetException(new Exception("NULL rse connection"),"NULL rse connection");
		}
		
		target=new RemoteTarget(rseConnection.getAliasName());
		try {
			target.connect(monitor);
		}catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}
	}
	
	protected void uninit(IProgressMonitor monitor) throws InvocationTargetException {
		if(target!=null) {
			try {
				if (target.isConnected()) {
					//always disconnect
					target.disconnect(null);
				}
			}catch (Exception e) {
				throw new InvocationTargetException(e,e.getMessage());
			}
		}
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
    InterruptedException {
	
		try {
			monitor.beginTask("", 100);
			init(new SubProgressMonitor(monitor,5));
			if(monitor.isCanceled())
				throw new InterruptedException("User canncelled");
			preProcess(new SubProgressMonitor(monitor,30));
			if(monitor.isCanceled())
				throw new InterruptedException("User canncelled");
			process(new SubProgressMonitor(monitor,30));
			if(monitor.isCanceled())
				throw new InterruptedException("User canncelled");
			postProcess(new SubProgressMonitor(monitor,30));
		}catch (InterruptedException e){
			throw e;
		}catch (InvocationTargetException e) {
			throw e;
		}finally {
			uninit(new SubProgressMonitor(monitor,5));
			monitor.done();
		}
	}

}

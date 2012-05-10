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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.yocto.sdk.remotetools.RSEHelper;
import org.yocto.sdk.remotetools.remote.RemoteApplication;
import org.yocto.sdk.remotetools.views.BaseFileView;

public class PowertopModel extends BaseModel {
	
	final private String REMOTE_EXEC="/tmp/yocto_tool.sh";
	final private String LOCAL_SCRIPT="resources/yocto_tool.sh";
	final private String REMOTE_FILE_PREFIX="/tmp/yocto-powertop-";
	final private String LOCAL_FILE_SUFFIX=".local";
	private Float time;
	private boolean showpid;
	Display display;
	
	String localfile;
	String remotefile;
	
	public PowertopModel(IHost host, Float time,boolean showpid,Display display) {
		super(host);
		this.time=time;
		this.showpid=showpid;
		this.display=display;
	}
	
	@Override
	public void preProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		//upload script to remote
		try {
			RSEHelper.putRemoteFileInPlugin(
					rseConnection, 
					LOCAL_SCRIPT, 
					REMOTE_EXEC,
					monitor);
		}catch (InterruptedException e){
			throw e;
		}catch (InvocationTargetException e) {
			throw e;
		}catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}

	}

	@Override
	public void postProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			new File(localfile).delete();
		}catch (Exception e) {
			
		}
	}
	
	private void generateData(IProgressMonitor monitor) throws Exception {
		int exit_code;
		RemoteApplication app=new RemoteApplication(rseConnection,null,REMOTE_EXEC,null);
		String currentDate=new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Calendar.getInstance().getTime()).toString();
		remotefile=new String(REMOTE_FILE_PREFIX + currentDate);
		localfile=new String(remotefile + LOCAL_FILE_SUFFIX);
		
		ArrayList <String> param= new ArrayList <String>();
		param.add(REMOTE_EXEC);
		param.add("start");
		param.add("-l");
		param.add(remotefile);
		param.add("powertop");
		param.add("-d");
		param.add("-t");
		param.add(time.toString());
		if(showpid)
			param.add("-p");
		String args = "start -l " + remotefile + " powertop -d -t " + time.toString();
		if(showpid)
			args += " -p";
		
		try {
			monitor.beginTask("Starting powertop", 2);
			//starting oprofile-server
			app.start(null,args,monitor);
			monitor.worked(1);
			exit_code=app.waitFor(monitor);
			app.terminate();
			if(exit_code!=0) {
				throw new Exception("Starting powertop failed with exit code " + new Integer(exit_code).toString());
			}
		}finally {
			monitor.done();
		}
	}

	private void getDataFile(IProgressMonitor monitor) throws Exception {
				
		RSEHelper.getRemoteFile(
				rseConnection, 
				localfile,
				remotefile, 
				monitor);
	}
	
	
	@Override
	public void process(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		
		monitor.beginTask("Running powertop", 100);		
		try {
			//running powertop
			monitor.subTask("Generating powertop data file remotely");
			generateData(new SubProgressMonitor(monitor,30));
			//download datafile
			monitor.subTask("Downloading powertop data file");
			getDataFile(new SubProgressMonitor(monitor,30));
			//show it in the powertop view
			display.syncExec(new Runnable() {
				public void run() {
					BaseFileView  view;
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						view=(BaseFileView) page.showView("org.yocto.sdk.remotetools.views.PowerTopView");
					}catch (PartInitException e) {
						e.printStackTrace();
						return;
					}
					view.setInput(localfile);
					page.bringToTop(view);
				}
			});
			
		}catch (InterruptedException e){
			throw e;
		}catch (InvocationTargetException e) {
			throw e;
		}catch (Exception e){
			throw new InvocationTargetException(e, e.getMessage());
		}finally {
			monitor.done();
		}

	}

}

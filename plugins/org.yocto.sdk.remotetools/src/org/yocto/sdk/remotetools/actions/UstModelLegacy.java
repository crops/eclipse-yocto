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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.ui.IWorkbenchWindow;

import org.yocto.sdk.remotetools.CommonHelper;
import org.yocto.sdk.remotetools.LocalJob;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.RSEHelper;
import org.yocto.sdk.remotetools.remote.RemoteApplication;

public class UstModelLegacy extends BaseModel {
	
	static final private String REMOTE_EXEC="/tmp/yocto_ust.sh";
	static final private String LOCAL_SCRIPT="resources/yocto_ust.sh";
	
	static final private String LOCAL_FILE_SUFFIX=".local.tar";
	static final private String REMOTE_FILE_SUFFIX=".tar";
	static final private String LOCAL_EXEC="lttv-gui";
	public static final String TRACE_FOLDER_NAME = "Traces";
	static final private String DATAFILE_PREFIX = "ustfile:";
		
	private String argument;
	private String application;
	private String prj_name;
	
	private String localfile;
	
	private IWorkbenchWindow window;
	
	public UstModelLegacy(IHost host, String app,String arg, String project, IWorkbenchWindow window) {
		super(host);
		application=app;
		argument=arg;
		prj_name = project;
		this.window=window;
	}

	@Override
	public void preProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		///upload script to remote
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
			//NOT delete the directory since lttv-gui is running asynchronously
			//new File(localfile.substring(0,localfile.length()-LOCAL_FILE_SUFFIX.length())).delete();
		}catch (Exception e) {
			
		}
	}
	
	private String generateData(IProgressMonitor monitor) throws Exception {
		int exit_code;
		RemoteApplication app=new RemoteApplication(rseConnection,null,REMOTE_EXEC,null);
		String tempArgs=new String();
		if(application!=null)
			tempArgs=tempArgs.concat(application + " ");
		if(argument!=null)
			tempArgs=tempArgs.concat(argument);
		String remoteDataFile=null;
		
		try {
			String temp;
			int idx;
			monitor.beginTask("Starting usttrace", 2);
			//starting usttrace
			app.start(null,tempArgs,monitor);
			monitor.worked(1);
			BufferedReader in=new BufferedReader(new InputStreamReader(app.getInputStream()));
			while((temp=in.readLine())!=null) {
				idx=temp.indexOf(DATAFILE_PREFIX);
				if(idx!=-1) {
					remoteDataFile=temp.substring(idx + DATAFILE_PREFIX.length());
					break;
				}
			}
			exit_code=app.waitFor(monitor);
			app.terminate();
			if(exit_code!=0) {
				throw new Exception("Starting usttrace failed with exit code " + new Integer(exit_code).toString());
			}
		}finally {
			monitor.done();
		}
		if(remoteDataFile==null)
			throw new Exception("Ust: null remote data file");
		return remoteDataFile;
	}
	
	private void getDataFile(IProgressMonitor monitor,String datafile) throws Exception {
		
		if(datafile.endsWith(REMOTE_FILE_SUFFIX)==false)
			throw new Exception("Wrong ust data file "+datafile);
		
		localfile=new String(datafile.substring(0,datafile.length()-4) + LOCAL_FILE_SUFFIX);
		
		RSEHelper.getRemoteFile(
				rseConnection, 
				localfile,
				datafile, 
				monitor);
	}

	private void importToProject(IProgressMonitor monitor) throws Exception {
		ProcessBuilder pb = new ProcessBuilder("tar", "fx", localfile);
		pb.directory(new File("/tmp"));
		Process p=pb.start();
		if(p.waitFor()!=0)
			throw new Exception("extract ust data files failed");
		
		String traceName = localfile.substring(0,localfile.length()-LOCAL_FILE_SUFFIX.length());
		
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
		IPath full_path = wsroot.getFullPath();
		IProject project = wsroot.getProject(prj_name);
		IFolder traceFolder = project.getFolder(TRACE_FOLDER_NAME);
		if (!traceFolder.exists()) {
			throw new Exception("Can't find file trace folder in selected project.");
		}
		
		traceFolder.createLink(new Path(traceName), IResource.REPLACE, monitor);
	}
	
	private String[] generateViewerParam() throws Exception {
		String viewerParam=new String(LOCAL_EXEC);
		int i;
		
		ProcessBuilder pb = new ProcessBuilder("tar", "fx", localfile);
		pb.directory(new File("/tmp"));
		Process p=pb.start();
		if(p.waitFor()!=0)
			throw new Exception("extract ust data files failed");
		File f=new File(localfile.substring(0,localfile.length()-LOCAL_FILE_SUFFIX.length()));
		File []subdir=f.listFiles();
		
		for (i=0;i<subdir.length;i++) {
			if(subdir[i].isDirectory()) {
				viewerParam=viewerParam.concat(" -t " + subdir[i].getAbsolutePath());
			}
		}
		
		return viewerParam.split(" ");
	}

	@Override
	public void process(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// TODO Auto-generated method stub
		
		String datafile;
		
		monitor.beginTask("Running ust", 100);		
		try {
			//running usttrace
			monitor.subTask("Generating user space lttng data file remotely");
			datafile=generateData(new SubProgressMonitor(monitor,30));
			
			//download datafile to local
			monitor.subTask("Downloading user space lttng data file");
			getDataFile(new SubProgressMonitor(monitor,30),datafile);
			
			//extract datafile and import to lttng project
			importToProject(new SubProgressMonitor(monitor,30));
			//extract datafile, prepare cmd array
			/* JZ - we're using Eclipse built-in Lttng-viewer now
			String []cmdarray=generateViewerParam();
			if(cmdarray==null) {
				throw new Exception("Ust: empty ust data");
			}
			monitor.worked(30);
			
			monitor.subTask("lttv-gui is running locally");
			//start lttv-gui asynchronously
			new LocalJob("lttv-gui",cmdarray,null,null,window).schedule();
			*/			
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
	
	static public boolean checkAvail() {
		boolean ret=CommonHelper.isExecAvail(LOCAL_EXEC);
		
		if(ret==false) {
			CommonHelper.showErrorDialog("User Mode Lttng", null,Messages.ErrorLttvGui);
		}
		return ret;
	}

}

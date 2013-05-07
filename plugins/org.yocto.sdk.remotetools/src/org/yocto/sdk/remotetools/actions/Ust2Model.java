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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.ui.IWorkbenchWindow;
import org.yocto.sdk.remotetools.remote.RemoteApplication;

public class Ust2Model extends BaseModel {
	
	private static final String REMOTE_EXEC = "/tmp/ust_tar.sh";
	private static final String LOCAL_SCRIPT = "resources/ust_tar.sh";
	
	private static final String LOCAL_FILE_SUFFIX = ".local.tar";
	private static final String REMOTE_FILE_SUFFIX = ".tar";
	private static final String LOCAL_EXEC = "lttv-gui";
	private static final String TRACE_FOLDER_NAME = "Traces";
	private static final String DATAFILE_PREFIX = "ustfile:";

	private static final String TASK_NAME = "ust2trace command";
		
	private String trace_loc;
	
	private String prj_name;
	
	private IWorkbenchWindow window;
	
	public Ust2Model(IHost host, String trace, String project, IWorkbenchWindow window) {
		super(host, TASK_NAME, LOCAL_SCRIPT, REMOTE_EXEC);
		trace_loc = trace;
		
		prj_name = project;
		this.window = window;
	}
	
	private void generateData(IProgressMonitor monitor) throws Exception {
		int exit_code;
		RemoteApplication app = new RemoteApplication(host, null, remoteExec, null);
		
		try {
			String temp;
			int idx;
			monitor.beginTask("Getting remote ust2 trace", 2);
			//starting usttrace
			app.start(null,trace_loc,monitor);
			monitor.worked(1);
			BufferedReader in=new BufferedReader(new InputStreamReader(app.getInputStream()));
			while((temp=in.readLine())!=null) {
				idx=temp.indexOf(DATAFILE_PREFIX);
				if(idx!=-1) {
					remoteFile = temp.substring(idx + DATAFILE_PREFIX.length());
					break;
				}
			}
			exit_code=app.waitFor(monitor);
			app.terminate();
			if(exit_code!=0) {
				throw new Exception("Getting remote ust2 trace failed with exit code " + new Integer(exit_code).toString());
			}
		}finally {
			monitor.done();
		}
		if(remoteFile == null)
			throw new Exception("Ust: null remote data file");
		if(remoteFile.endsWith(REMOTE_FILE_SUFFIX)==false)
			throw new Exception("Wrong ust data file " + remoteFile);
		
		localFile = new String(remoteFile.substring(0, remoteFile.length()-4) + LOCAL_FILE_SUFFIX);
	}

	private void importToProject(IProgressMonitor monitor) throws Exception {
		ProcessBuilder pb = new ProcessBuilder("tar", "fx", localFile);
		pb.directory(new File("/tmp"));
		Process p=pb.start();
		if(p.waitFor()!=0)
			throw new Exception("extract ust data files failed");
		
		String traceName = localFile.substring(0,localFile.length()-LOCAL_FILE_SUFFIX.length());
		
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
		IPath full_path = wsroot.getFullPath();
		IProject project = wsroot.getProject(prj_name);
		IFolder traceFolder = project.getFolder(TRACE_FOLDER_NAME);
		if (!traceFolder.exists()) {
			throw new Exception("Can't find file trace folder in selected project.");
		}
		
		String trace_str = traceName.substring(0, traceName.indexOf('-'));
		traceFolder.createLink(new Path(trace_str), IResource.REPLACE, monitor);
		for (IResource resource:traceFolder.members()) {
			String extension = resource.getFileExtension();
			if (extension != null)
				continue;
			else {
				//traceFolder.setPersistentProperty(TmfCommonConstants.TRACETYPE, "org.eclipse.linuxtools.tmf.ui.type.ctf");
				//resource.setPersistentProperty(TmfCommonConstants.TRACETYPE, "org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace");
				//traceFolder.setPersistentProperty(TmfCommonConstants.TRACEICON, "icons/obj16/garland16.png");
				//traceFolder.touch(null);
			}
		}
	}
	
	private String[] generateViewerParam() throws Exception {
		String viewerParam=new String(LOCAL_EXEC);
		int i;
		
		ProcessBuilder pb = new ProcessBuilder("tar", "fx", localFile);
		pb.directory(new File("/tmp"));
		Process p=pb.start();
		if(p.waitFor()!=0)
			throw new Exception("extract ust data files failed");
		File f=new File(localFile.substring(0,localFile.length()-LOCAL_FILE_SUFFIX.length()));
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
			//preparing remote trace
			
			monitor.subTask("Preparing user space lttng data file remotely");
			generateData(new SubProgressMonitor(monitor,30));
			
			//download datafile to local
			monitor.subTask("Downloading user space lttng data file");
			getDataFile(new SubProgressMonitor(monitor,30));
			
			//extract datafile and import to lttng project
			importToProject(new SubProgressMonitor(monitor,30));
				
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

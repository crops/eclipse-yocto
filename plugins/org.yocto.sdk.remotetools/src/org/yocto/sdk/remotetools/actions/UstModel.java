package org.yocto.sdk.remotetools.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.yocto.sdk.remotetools.CommonHelper;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.RSEHelper;
import org.yocto.sdk.remotetools.remote.RemoteApplication;

public class UstModel extends BaseModel {
	
	static final private String REMOTE_EXEC="/tmp/yocto_ust.sh";
	static final private String LOCAL_SCRIPT="resources/yocto_ust.sh";
	
	static final private String LOCAL_FILE_SUFFIX=".local.tar";
	static final private String REMOTE_FILE_SUFFIX=".tar";
	static final private String LOCAL_EXEC="lttv-gui";
		
	private String argument;
	private String application;
	
	private String localfile;
	
	public UstModel(IHost host, String app,String arg) {
		super(host);
		application=app;
		argument=arg;
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
		}catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}

	}

	@Override
	public void postProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			new File(localfile).delete();
			new File(localfile.substring(0,localfile.length()-LOCAL_FILE_SUFFIX.length())).delete();
		}catch (Exception e) {
			
		}
	}
	
	private String generateData(IProgressMonitor monitor) throws Exception {
		int exit_code;
		RemoteApplication app=new RemoteApplication(target,null,REMOTE_EXEC);
		String tempArgs=new String(REMOTE_EXEC+ " ");
		if(application!=null)
			tempArgs=tempArgs.concat(application + " ");
		if(argument!=null)
			tempArgs=tempArgs.concat(argument);
		String []args=tempArgs.split(" ");
		String remoteDataFile=null;
		
		try {
			monitor.beginTask("Starting usttrace", 2);
			//starting usttrace
			app.start(args,null);
			monitor.worked(1);
			BufferedReader in=new BufferedReader(new InputStreamReader(app.getInputStream()));
			remoteDataFile=in.readLine();
			System.out.println(remoteDataFile);
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
			
			//extract datafile, prepare cmd array
			String []cmdarray=generateViewerParam();
			if(cmdarray==null) {
				throw new Exception("Ust: empty ust data");
			}
			monitor.worked(30);
			
			monitor.subTask("lttv-gui is running locally");
			Process p=Runtime.getRuntime().exec(cmdarray,null,null);
			while (!monitor.isCanceled()) {
				try {
					p.exitValue();
					break;
				}catch (IllegalThreadStateException e) {
				}
				Thread.sleep(500);				
			}
			p.destroy();
			
		}catch (InterruptedException e) {
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

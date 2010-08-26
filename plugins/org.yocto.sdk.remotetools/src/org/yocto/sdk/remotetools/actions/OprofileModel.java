package org.yocto.sdk.remotetools.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.jface.preference.IPreferenceStore;

import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.preferences.PreferenceConstants;
import org.yocto.sdk.remotetools.remote.RemoteApplication;
import org.yocto.sdk.remotetools.RSEHelper;

public class OprofileModel extends BaseModel {
	
	final private String REMOTE_EXEC="/tmp/yocto_tool.sh";
	final private String LOCAL_SCRIPT="resources/yocto_tool.sh";
	
	public OprofileModel(IHost host) {
		super(host);
		
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
		}catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}
	}

	@Override
	public void postProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {

	}
	
	private void startServer(IProgressMonitor monitor) throws Exception {
		int exit_code;
		RemoteApplication app=new RemoteApplication(target,null,REMOTE_EXEC);
		String[] args={
				REMOTE_EXEC,
				"start",
				"-d",
				"oprofile-server"
		};
		
		try {
			monitor.beginTask("Starting oprofile-server", 2);
			//starting oprofile-server
			app.start(args,null);
			monitor.worked(1);

			exit_code=app.waitFor(monitor);
			app.terminate();
			if(exit_code!=0) {
				throw new Exception("Starting oprofile-server failed with exit code " + new Integer(exit_code).toString());
			}
		}finally {
			monitor.done();
		}
	}
	
	private void stopServer(IProgressMonitor monitor) throws Exception {
		
		RemoteApplication app=new RemoteApplication(target,null,REMOTE_EXEC);
		String[] args={
				REMOTE_EXEC,
				"stop",
				"-d",
				"oprofile-server"
		};
		try {
			monitor.beginTask("Stopping oprofile-server", 2);
			app.start(args,null);
			monitor.worked(1);
			//no cancel for stop server
			app.waitFor(null);
			app.terminate();
		}finally {
			monitor.done();
		}
	}

	private String getSearchPath() {
		String search=null;
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		String env_script=store.getString(PreferenceConstants.TOOLCHAIN_ROOT) +
							"/" + 
							"environment-setup-" +
							store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);
		File env_script_file = new File(env_script);
		try {
			if (env_script_file.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(env_script_file));
				try {
					String line = null;
					while ((line = input.readLine()) != null) {
						if (line.contains(" PATH=")) {
							search = line.substring(line.indexOf('=')+1);
							search = search.substring(0,search.length()-1);
						}
					}
				}finally {
					input.close();
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
			search=null;
		}
		return search;
	}

	@Override
	public void process(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		
		monitor.beginTask("Running oprofile", 100);		
		
		try {
			try {
		
			//start oprofile-server
			startServer(new SubProgressMonitor(monitor,30));
			
			//start local oprofile-viewer
			String searchPath=getSearchPath();
			
			Process p=Runtime.getRuntime().exec(
					(searchPath!=null) ? 
						new String[] {"oprofile-viewer","-h",target.getRemoteHostName(),"-s",searchPath} : 
						new String[] {"oprofile-viewer","-h",target.getRemoteHostName()},
					null,
					null);
			
			//wait for oprofile-viewer to finish
			monitor.worked(40);
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
			}finally {
				//stop oprofile-server
				stopServer(new SubProgressMonitor(monitor,30));
			}
		}catch (InterruptedException e) {
			throw e;
		}catch (Exception e){
			throw new InvocationTargetException(e, e.getMessage());
		}finally {
			monitor.done();
		}
	}

}

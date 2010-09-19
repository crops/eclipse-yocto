package org.yocto.sdk.remotetools.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.jface.preference.IPreferenceStore;

import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.preferences.PreferenceConstants;
import org.yocto.sdk.remotetools.remote.RemoteApplication;
import org.yocto.sdk.remotetools.CommonHelper;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.RSEHelper;

public class OprofileModel extends BaseModel {
	
	static final private String REMOTE_EXEC="/tmp/yocto_tool.sh";
	static final private String LOCAL_SCRIPT="resources/yocto_tool.sh";
	static final private String LOCAL_EXEC="oprofile-viewer";
	
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
	
	private String getSearchPath()
	{
		try
		{
			String search=null;
			IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
			String env_script=store.getString(PreferenceConstants.TOOLCHAIN_ROOT) +
								"/" + 
								"environment-setup-" +
								store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);
			File file = new File(env_script);

			if (file.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(file));

				try
				{
					String line = null;

					while ((line = input.readLine()) != null)
					{
						if (!line.startsWith("export"))
							continue;
						String sKey = line.substring("export".length() + 1, line.indexOf('='));
						if(!sKey.equals("PATH"))
							continue;
						String sValue = line.substring(line.indexOf('=') + 1);
						if (sValue.startsWith("\"") && sValue.endsWith("\""))
							sValue = sValue.substring(sValue.indexOf('"') + 1, sValue.lastIndexOf('"'));
						/* If PATH ending with $PATH, we need to join with current system path */
						if (sKey.equalsIgnoreCase("PATH")) {
							if (sValue.lastIndexOf("$PATH") >= 0)
								sValue = sValue.substring(0, sValue.lastIndexOf("$PATH")) + System.getenv("PATH");
						}
						search=sValue;
						//System.out.printf("get env key %s value %s\n", sKey, sValue);
					}

				}
				finally {
					input.close();
				}
			}

			return search;

		} 
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}

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
						new String[] {LOCAL_EXEC,"-h",target.getRemoteHostName(),"-s",searchPath} : 
						new String[] {LOCAL_EXEC,"-h",target.getRemoteHostName()},
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
	
	static public boolean checkAvail() {
		boolean ret=CommonHelper.isExecAvail(LOCAL_EXEC);
		
		if(ret==false) {
			CommonHelper.showErrorDialog("Oprofile", null,Messages.ErrorOprofileViewer);
		}
		return ret;
	}

}

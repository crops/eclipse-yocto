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
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.ui.IWorkbenchWindow;
import org.yocto.remote.utils.CommonHelper;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.preferences.PreferenceConstants;
import org.yocto.sdk.remotetools.LocalJob;
import org.yocto.sdk.remotetools.Messages;

public class OprofileModel extends BaseModel {
	
	private static final String REMOTE_EXEC = "/tmp/yocto_tool.sh";
	private static final String LOCAL_SCRIPT = "resources/yocto_tool.sh";

	private static final String LOCAL_EXEC = "oprofile-viewer";

	private static final String TASK_NAME = "oprofile command";

	private IWorkbenchWindow window;
	public OprofileModel(IHost host, IWorkbenchWindow window) {
		super(host, TASK_NAME, LOCAL_SCRIPT, REMOTE_EXEC);
		this.window = window;
	}
	
	private void startServer(IProgressMonitor monitor) throws Exception {
		String args="start -d oprofile-server";
		runRemoteShellExec(monitor, args, true);
	}
	
	private void stopServer(IProgressMonitor monitor) throws Exception {
		String args = "stop -d oprofile-server";
		runRemoteShellExec(monitor, args, false);
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
		
		boolean stopServer=true;
		
		try {
			try {
			monitor.beginTask("Starting oprofile", 100);	
			//start oprofile-server
			monitor.subTask("Starting oprofile-server");
			startServer(SubMonitor.convert(monitor,80));
			
			//start local oprofile-viewer
			monitor.subTask("oprofile-viewer is running locally");
			String searchPath=getSearchPath();
			
			new LocalJob("oprofile-viewer",
					(searchPath!=null) ? 
						new String[] {LOCAL_EXEC,"-h",host.getHostName(),"-s",searchPath} :
						new String[] {LOCAL_EXEC,"-h",host.getHostName()},
					null,
					null,
					window).schedule();
			//we can't stop server because the oprofile-viewer is running asynchronously
			stopServer=false;
			
			}catch (InterruptedException e) {
				throw e;
			}finally {
				//stop oprofile-server
				if(stopServer) {
					monitor.subTask("Stopping oprofile-viewer");
					stopServer(SubMonitor.convert(monitor,30));
				}
			}
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
			CommonHelper.showErrorDialog("Oprofile", null,Messages.ErrorOprofileViewer);
		}else {
			ret=CommonHelper.isExecAvail("opreport");
			if(ret==false) {
				CommonHelper.showErrorDialog("Oprofile", null,Messages.ErrorOprofile);
			}
		}
		return ret;
	}

}

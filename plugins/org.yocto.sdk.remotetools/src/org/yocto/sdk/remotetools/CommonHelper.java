package org.yocto.sdk.remotetools;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

public class CommonHelper {
/*	
	static protected String oprofileuiViewerPath;
	static protected String lttvGuiPath;
	
	static protected String getExecPath(String exec) {
		String path=null;	
		try {
			Process p=Runtime.getRuntime().exec(new String[] {"which",exec});
			p.waitFor();
			if(p.exitValue()==0) {
				path=new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return path;
	}
	
	static public boolean isOprofileViewerAvail() {
		if(oprofileuiViewerPath==null) {
			oprofileuiViewerPath=getExecPath("oprofile-viewer");
		}
		
		return oprofileuiViewerPath==null ? false: true;
	}
	
	static public boolean isLttvGuiAvail() {
		if(lttvGuiPath==null) {
			lttvGuiPath=getExecPath("lttv-gui");
		}
		
		return lttvGuiPath==null ? false: true;
	}
*/
	static public boolean isExecAvail(String exec) {
		boolean ret=false;	
		try {
			Process p=Runtime.getRuntime().exec(new String[] {"which",exec});
			p.waitFor();
			if(p.exitValue()==0) {
				ret=true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void showErrorDialog(final String dialogTitle, final String errorMessage, final String reason) {
		//needs to be run in the ui thread otherwise swt throws invalid thread access 
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, dialogTitle, errorMessage, new Status(IStatus.ERROR,Activator.PLUGIN_ID,reason));
			}
		});

	}
	
}

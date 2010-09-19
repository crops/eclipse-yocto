package org.yocto.sdk.remotetools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

public class CommonHelper {

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

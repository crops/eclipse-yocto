package org.yocto.sdk.remotetools.wizards.bsp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates a progress monitor dialog that will run in the background a BSPThread and display a custom message
 * @author ioana.grigoropol
 *
 */
public class BSPProgressDialog extends ProgressMonitorDialog{
	String displayMessage;
	BSPThread getterThread;
	Shell shell;


	public BSPProgressDialog(Shell parent, BSPThread getterThread, String displayMessage) {
		super(parent);
		this.shell = parent;
		this.getterThread = getterThread;
		this.displayMessage = displayMessage;
	}

	public void run(boolean showProgressDialog){
		try {
			if (showProgressDialog)
				super.run(true, true, new IRunnableWithProgress(){
					@Override
					public void run(IProgressMonitor monitor) {
						monitor.beginTask(displayMessage + " ...", 100);
						getterThread.run();
						monitor.done();
					}
				});
			else
				getterThread.run();
		} catch (Exception e) {
			getterThread.getBspAction().setMessage(e.getMessage());
		}
	}

	public BSPAction getBspAction() {
		return getterThread.getBspAction();
	}
}

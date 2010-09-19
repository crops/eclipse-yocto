package org.yocto.sdk.remotetools.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.IProgressService;

public class OprofileHandler extends AbstractHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		if(OprofileModel.checkAvail()!=true) {
			return null;
		}
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		SimpleSettingDialog setting=new SimpleSettingDialog(
				window.getShell(),
				"Oprofile",
				IBaseConstants.CONNECTION_NAME_OPROFILE
				);
		
		if(setting.open()==BaseSettingDialog.OK) {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			OprofileModel op=new OprofileModel(setting.getHost());
			try {
				progressService.busyCursorWhile(op);
			}catch (Exception e) {
				e.printStackTrace();
				MessageDialog.openError(window.getShell(),
						"Oprofile",
						e.getMessage());
			}
		}
		return null;
	}
	
}

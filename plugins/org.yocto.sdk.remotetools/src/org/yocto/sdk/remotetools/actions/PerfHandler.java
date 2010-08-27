package org.yocto.sdk.remotetools.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class PerfHandler extends TerminalHandler {

	private static String initCmd="cd; perf\r";
	
	protected String getInitCmd() {
		return initCmd;
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		setting=new SimpleSettingDialog(
				window.getShell(),
				"Latencytop",
				IBaseConstants.CONNECTION_NAME_LATENCYTOP
				);
	}

}

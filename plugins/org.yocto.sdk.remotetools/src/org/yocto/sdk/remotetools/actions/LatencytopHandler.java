package org.yocto.sdk.remotetools.actions;

import org.yocto.sdk.remotetools.RSEHelper;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.terminals.ui.TerminalServiceHelper;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewer;
import org.eclipse.rse.internal.terminals.ui.views.TerminalsUI;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.elements.TerminalElement;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class LatencytopHandler extends AbstractHandler {
	
	private static String initCmd="cd; sudo latencytop\r";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		SimpleSettingDialog setting=new SimpleSettingDialog(
				window.getShell(),
				"Latencytop",
				IBaseConstants.CONNECTION_NAME_LATENCYTOP
				);
		if(setting.open()==BaseSettingDialog.OK) {
			IHost currentHost = setting.getHost();
			ITerminalServiceSubSystem terminalSubSystem = RSEHelper.getTerminalSubSystem(currentHost);
			
			if (terminalSubSystem != null) {
				TerminalsUI terminalsUI = TerminalsUI.getInstance();
				TerminalViewer viewer = terminalsUI.activateTerminalsView();
				if (!terminalSubSystem.isConnected()) {
					try {
						terminalSubSystem.connect(new NullProgressMonitor(), false);
					} catch (OperationCanceledException e) {
						// user canceled, return silently
						return null;
					} catch (Exception e) {
						SystemBasePlugin.logError(e.getLocalizedMessage(), e);
					}
				}
				if (terminalSubSystem.isConnected()) {
					CTabItem tab = viewer.getTabFolder().createTabItem(
							terminalSubSystem.getHost(), initCmd);
					TerminalElement element = TerminalServiceHelper
							.createTerminalElement(tab, terminalSubSystem);
					terminalSubSystem.addChild(element);
	
				}
			}
		}
		return null;
	}

}

/*******************************************************************************
 * Copyright (c) 2011 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.remotetools.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.terminals.ui.TerminalServiceHelper;
import org.eclipse.rse.internal.terminals.ui.views.RSETerminalConnector;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewTab;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewer;
import org.eclipse.rse.internal.terminals.ui.views.TerminalsUI;
import org.eclipse.rse.services.terminals.ITerminalShell;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.elements.TerminalElement;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.yocto.remote.utils.RemoteHelper;

@SuppressWarnings("restriction")
public class SystemtapHandler extends AbstractHandler {
	protected SystemtapSettingDialog setting;
	protected String changeTerm="export TERM=vt100;";
	protected String localConnection="LOCALHOST";
	protected IWorkbenchWindow window;
	protected Shell shell;
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SystemtapSettingDialog(
				shell, "Systemtap"
				);
		

		if(setting.open() == BaseSettingDialog.OK) {

			String metadata_location = ((SystemtapSettingDialog)setting).getMetadataLocation();
                        String builddir_location = ((SystemtapSettingDialog)setting).getBuilddirLocation();
			String remote_host = ((SystemtapSettingDialog)setting).getRemoteHost();
			String user_id = ((SystemtapSettingDialog)setting).getUserID();
			String systemtap_script = ((SystemtapSettingDialog)setting).getSystemtapScript();
			String systemtap_args = ((SystemtapSettingDialog)setting).getSystemtapArgs();
			RemoteHelper.waitForRSEInitCompletition();
			IHost host = RemoteHelper.getRemoteConnectionByName(localConnection);
			if (host == null)
				host = RemoteHelper.createLocalConnection();
			final ITerminalServiceSubSystem terminalSubSystem = RemoteHelper.getTerminalSubSystem(host);
			
			if (terminalSubSystem != null) {
				TerminalsUI terminalsUI = TerminalsUI.getInstance();
				TerminalViewer terminalViewer = terminalsUI.activateTerminalsView();
				SystemtapModel op = new SystemtapModel(metadata_location,builddir_location,remote_host, user_id, systemtap_script,
						systemtap_args,window.getShell().getDisplay());
				try {
					op.setHost(host);
					op.preProcess(new NullProgressMonitor());
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				CTabItem tab = terminalViewer.getTabFolder().createTabItem(
						host, changeTerm + "cd " + metadata_location+ ";" + "source oe-init-build-env;" + "crosstap " + user_id + "@" + remote_host + " " + systemtap_script +"\r");
				try {
					tab.addDisposeListener(new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							Object source = e.getSource();
							if (source instanceof CTabItem) {
								CTabItem currentItem = (CTabItem) source;
								ITerminalShell shell= getTerminalShellFromTab(currentItem);
								if(shell!=null) {
									shell.exit();
								}
							}
						}
					});
				} catch(Exception e) {
					e.printStackTrace();
				}
				TerminalElement element = TerminalServiceHelper.createTerminalElement(tab, terminalSubSystem);
				terminalSubSystem.addChild(element);
			}
						
		}
		return false;
	}
	
	protected ITerminalShell getTerminalShellFromTab(CTabItem item) {
		ITerminalShell terminalShell = null;
		ITerminalViewControl terminalViewControl = (ITerminalViewControl)item.getData(TerminalViewTab.DATA_KEY_CONTROL);
		ITerminalConnector terminalConnector = terminalViewControl.getTerminalConnector();
		if (terminalConnector instanceof RSETerminalConnector) {
			RSETerminalConnector rseTerminalConnector = (RSETerminalConnector) terminalConnector;
			terminalShell = rseTerminalConnector.getTerminalHostShell();
		}
		return terminalShell;
	}
	
	protected void initialize(ExecutionEvent event) throws ExecutionException {
		this.window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		shell = window.getShell();
		setting=new SystemtapSettingDialog(
				shell, "Systemtap"
				);
	}

}

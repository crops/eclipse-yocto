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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.terminals.ui.TerminalServiceHelper;
import org.eclipse.rse.internal.terminals.ui.views.RSETerminalConnector;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewTab;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewer;
import org.eclipse.rse.internal.terminals.ui.views.TerminalsUI;
import org.eclipse.rse.services.terminals.ITerminalShell;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.elements.TerminalElement;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.yocto.sdk.remotetools.RSEHelper;

abstract public class TerminalHandler extends AbstractHandler {
	
	protected BaseSettingDialog setting;
	
	protected String changeTerm="export TERM=vt100;";
	
	abstract protected String getInitCmd();
	abstract protected void initialize(ExecutionEvent event) throws ExecutionException;
	
	protected ITerminalShell getTerminalShellFromTab(CTabItem item) {
        ITerminalShell terminalShell = null;
        ITerminalViewControl terminalViewControl = (ITerminalViewControl) item
                .getData(TerminalViewTab.DATA_KEY_CONTROL);
        ITerminalConnector terminalConnector = terminalViewControl
                .getTerminalConnector();
        if (terminalConnector instanceof RSETerminalConnector) {
            RSETerminalConnector rseTerminalConnector = (RSETerminalConnector) terminalConnector;
            terminalShell = rseTerminalConnector.getTerminalHostShell();
        }
        return terminalShell;
    }

	protected void preProcess() {
		
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		initialize(event);
		
		if(setting.open()==BaseSettingDialog.OK) {
			IHost currentHost = setting.getHost();
			ITerminalServiceSubSystem terminalSubSystem = RSEHelper.getTerminalSubSystem(currentHost);
			preProcess();
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
							terminalSubSystem.getHost(), changeTerm + getInitCmd());
					//since RSETerminalConnector not exit the shell during the diconnection,
					//we have manually exit it here
					try {
						tab.addDisposeListener(new DisposeListener() {
							public void widgetDisposed(DisposeEvent e) {
								
								Object source = e.getSource();
								if (source instanceof CTabItem) {
									CTabItem currentItem = (CTabItem) source;
									ITerminalShell shell=getTerminalShellFromTab(currentItem);
									if(shell!=null) {
										shell.exit();
									}
								}
							}
						});
					}catch(Exception e) {
						e.printStackTrace();
					}
					TerminalElement element = TerminalServiceHelper
							.createTerminalElement(tab, terminalSubSystem);
					terminalSubSystem.addChild(element);
	
				}
			}
		}
		return null;
	}

}

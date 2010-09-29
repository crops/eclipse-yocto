/*******************************************************************************
 * Copyright (c) 2006, 2010 PalmSource, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ewa Matejska          (PalmSource) - initial API and implementation
 * Martin Oberhuber      (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * Martin Oberhuber      (Wind River) - [196934] hide disabled system types in remotecdt combo
 * Yu-Fen Kuo            (MontaVista) - [190613] Fix NPE in Remotecdt when RSEUIPlugin has not been loaded
 * Martin Oberhuber      (Wind River) - [cleanup] Avoid using SystemStartHere in production code
 * Johann Draschwandtner (Wind River) - [231827][remotecdt]Auto-compute default for Remote path
 * Johann Draschwandtner (Wind River) - [233057][remotecdt]Fix button enablement
 * Anna Dushistova       (MontaVista) - [181517][usability] Specify commands to be run before remote application launch
 * Anna Dushistova       (MontaVista) - [223728] [remotecdt] connection combo is not populated until RSE is activated
 * Anna Dushistova       (MontaVista) - [267951] [remotecdt] Support systemTypes without files subsystem
 * Lianhao Lu			 (Intel)      - Modified for internal use
 *******************************************************************************/

package org.yocto.sdk.remotetools.actions;

import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.SWTFactory;
import org.yocto.sdk.remotetools.RSEHelper;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.ui.actions.SystemNewConnectionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class BaseSettingDialog extends Dialog {

	protected String title;
	protected String curConn; //current rse connection name
	protected IHost conn;//rse IHost
	
	protected Button newRemoteConnectionButton;
	protected Label connectionLabel;
	protected Combo connectionCombo;
	protected SystemNewConnectionAction action = null;
	
	
	protected BaseSettingDialog(Shell parentShell,String title, String connection) {
		super(parentShell);
		this.title=title;
		this.curConn=connection;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite)super.createDialogArea(parent);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		
		/* The RSE Connection dropdown with New button. */
		SWTFactory.createVerticalSpacer(comp, 1);
		createRemoteConnectionGroup(comp);
		
		return comp;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateCurConn();
	}

	protected void createRemoteConnectionGroup(Composite parent) {
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 6;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);

		connectionLabel = new Label(projComp, SWT.NONE);
		connectionLabel.setText(Messages.BaseSettingDialog_Connection);
		gd = new GridData();
		gd.horizontalSpan = 1;
		connectionLabel.setLayoutData(gd);
		
		connectionCombo = new Combo(projComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		connectionCombo.setLayoutData(gd);
		connectionCombo.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				//setDirty(true);
				//updateLaunchConfigurationDialog();
				//useDefaultsFromConnection();
				updateCurConn();
			}
		});

		newRemoteConnectionButton = SWTFactory.createPushButton(projComp,
				Messages.BaseSettingDialog_New, null);
		newRemoteConnectionButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent evt) {
				handleNewRemoteConnectionSelected();
				//updateLaunchConfigurationDialog();
				updateConnectionPulldown();
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 1;
		newRemoteConnectionButton.setLayoutData(gd);

		updateConnectionPulldown();
	}
	
	protected boolean updateOkButton() {
		boolean ret=false;
		Button button=getButton(IDialogConstants.OK_ID);
		if(button!=null)
			button.setEnabled(false);
		IHost currentConnectionSelected = getCurrentConnection();
		if (currentConnectionSelected != null) {
			IRSESystemType sysType = currentConnectionSelected.getSystemType();
			if (sysType != null && sysType.isEnabled() && !sysType.isLocal()) {
				if(button!=null) {
					button.setEnabled(true);
					ret=true;
				}
			}
		}
		return ret;
	}
	
	protected void updateCurConn() {
		IHost currentConnectionSelected = getCurrentConnection();
		if (currentConnectionSelected != null) {
			IRSESystemType sysType = currentConnectionSelected.getSystemType();
			if (sysType != null && sysType.isEnabled() && !sysType.isLocal()) {
				curConn=currentConnectionSelected.getAliasName();
			}
		}
		updateOkButton();
	}
	
	protected IHost getCurrentConnection() {
		int currentSelection = connectionCombo.getSelectionIndex();
		String remoteConnection = currentSelection >= 0 ? connectionCombo
				.getItem(currentSelection) : null;
        return RSEHelper.getRemoteConnectionByName(remoteConnection);
    }
	
	protected void handleNewRemoteConnectionSelected() {
		if (action == null) {
			action = new SystemNewConnectionAction(getShell(),
					false, false, null);
		}

		try {
			action.run();
		} catch (Exception e) {
			// Ignore
		}
	}
	
	protected void updateConnectionPulldown() {
		int index=-1;
		if (!RSECorePlugin.isInitComplete(RSECorePlugin.INIT_MODEL))
			try {
				RSECorePlugin.waitForInitCompletion(RSECorePlugin.INIT_MODEL);
			} catch (InterruptedException e) {
				return;
			}
		// already initialized
		connectionCombo.removeAll();
		IHost[] connections = RSEHelper.getSuitableConnections();
		for (int i = 0; i < connections.length; i++) {
			IRSESystemType sysType = connections[i].getSystemType();
			if (sysType != null && sysType.isEnabled()) {
				connectionCombo.add(connections[i].getAliasName());
				if(connections[i].getAliasName().equals(curConn))
					index=i;
			}
		}
		
		if(index>=0) {
			connectionCombo.select(index);
		}else if (connections.length > 0) {
			connectionCombo.select(connections.length - 1);
		}
	
		//TODO
		//connectionCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT,true);
		connectionCombo.pack(true);
		connectionCombo.layout();
		connectionCombo.getParent().layout();
		
		updateCurConn();
	}

	@Override
	protected void okPressed() {
		conn=getCurrentConnection();
		super.okPressed();
	}
	
	public IHost getHost() {
		return conn;
	}
}

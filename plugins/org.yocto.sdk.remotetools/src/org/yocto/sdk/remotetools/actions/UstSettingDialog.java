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

import org.yocto.sdk.remotetools.Activator;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.SWTFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class UstSettingDialog extends BaseSettingDialog {
	
	static protected String TITLE="User Mode lttng";
	
	protected String argument;
	protected String application;
	protected Text argText;
	protected Text appText;
	
	protected UstSettingDialog(Shell parentShell, String title, String conn) {
		super(parentShell,title,conn);
	}
	
	public UstSettingDialog(Shell parentShell) {
		this(parentShell,
				TITLE,
				Activator.getDefault().getDialogSettings().get(IBaseConstants.CONNECTION_NAME_UST)
				);
	}
	
	public String getArgument() {
		return argument;
	}
	
	public String getApplication() {
		return application;
	}
	
	@Override
	protected void okPressed() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
	    // store the value of the generate sections checkbox
		if(getCurrentConnection()==null) {
			settings.put(IBaseConstants.CONNECTION_NAME_UST,
					(String)null);
		}else {
			settings.put(IBaseConstants.CONNECTION_NAME_UST, 
					getCurrentConnection().getAliasName());
		}
		application=appText.getText();
		argument=argText.getText();
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp=(Composite)super.createDialogArea(parent);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		
		/*argument*/
		SWTFactory.createVerticalSpacer(comp, 1);
		createArgument(comp);
		
		updateOkButton();
		return comp;
	}
	
	protected void createArgument(Composite parent)
	{
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 4;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		
		Label label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Usttrace_Application_Text);
		gd = new GridData();
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		
		appText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		appText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOkButton();
			}
		});
		if(application!=null)
			appText.setText(application);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		appText.setLayoutData(gd);
		
		label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Usttrace_Argument_Text);
		gd = new GridData();
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		
		argText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		if(argument!=null)
			argText.setText(argument);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		argText.setLayoutData(gd);
	}

	@Override
	protected boolean updateOkButton() {
		boolean ret=super.updateOkButton();
		if(ret==true) {
			if(appText.getText().isEmpty()) {
				Button button=getButton(IDialogConstants.OK_ID);
				if(button!=null)
					button.setEnabled(false);
				ret=false;
			}
		}
		return ret;
	}
}

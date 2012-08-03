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
import org.yocto.sdk.remotetools.RSEHelper;
import org.yocto.sdk.remotetools.SWTFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.linuxtools.tmf.core.TmfProjectNature;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;


public class Ust2SettingDialog extends UstSettingDialogBase {
	
	static protected String TITLE="Lttng2.0 User Tracing Import";
	
	protected String trace;
	protected Text traceText;
	
	protected Ust2SettingDialog(Shell parentShell, String title, String conn) {
		super(parentShell,title,conn);
	}
	
	public Ust2SettingDialog(Shell parentShell) {
		this(parentShell,
				TITLE,
				Activator.getDefault().getDialogSettings().get(IBaseConstants.CONNECTION_NAME_UST)
				);
	}
	
	public String getTrace() {
		return trace;
	}
	
	@Override
	protected void okPressed() {
		
		trace=traceText.getText();
		
		super.okPressed();
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
		label.setText(Messages.Usttrace_Trace_Loc_Text);
		gd = new GridData();
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		
		traceText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		traceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOkButton();
			}
		});
		if(trace!=null)
			traceText.setText(trace);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		traceText.setLayoutData(gd);
	}

	@Override
	protected boolean updateOkButton() {
		boolean ret=super.updateOkButton();
		if(ret==true) {
			if(traceText.getText().isEmpty() || !traceText.getText().endsWith("/ust")) {
				Button button=getButton(IDialogConstants.OK_ID);
				if(button!=null)
					button.setEnabled(false);
				ret=false;
			} 
		}
		return ret;
	}
}

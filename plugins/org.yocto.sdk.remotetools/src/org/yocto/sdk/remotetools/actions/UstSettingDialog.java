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
import org.eclipse.linuxtools.tmf.TmfProjectNature;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IProject;


public class UstSettingDialog extends BaseSettingDialog {
	
	static protected String TITLE="User Mode lttng";
	
	protected String argument;
	protected String application;
	protected Text argText;
	protected Text appText;
	
	protected Label projectLabel;
	protected Combo projectCombo;
	protected String curProject = null;
	
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
	
	public String getProject() {
		return curProject;
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
		createImportProjectGroup(comp);
		createArgument(comp);
		
		updateOkButton();
		return comp;
	}
	
	protected void createImportProjectGroup(Composite parent) {
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 6;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);

		projectLabel = new Label(projComp, SWT.NONE);
		projectLabel.setText(Messages.Import_to_Project);
		gd = new GridData();
		gd.horizontalSpan = 1;
		projectLabel.setLayoutData(gd);
		
		projectCombo = new Combo(projComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		projectCombo.setLayoutData(gd);
		projectCombo.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				//setDirty(true);
				//updateLaunchConfigurationDialog();
				//useDefaultsFromConnection();
				updateCurProject();
			}
		});

		/* JZ we're using Eclipse built-in lttng-viewer now
		newRemoteConnectionButton = SWTFactory.createPushButton(projComp,
				Messages.BaseSettingDialog_New, null);
		newRemoteConnectionButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent evt) {
				handleNewRemoteConnectionSelected();
				updateLaunchConfigurationDialog();
				updateConnectionPulldown();
			}
		});
		gd = new GridData();
		gd.horizontalSpan = 1;
		newRemoteConnectionButton.setLayoutData(gd);
		*/
		updateProjectPulldown();
	}
	
	protected void updateCurProject() {
		IProject currentProjectSelected = getCurrentProject();
		
		if (currentProjectSelected != null)
			curProject = currentProjectSelected.getName();
		/*
		if (currentConnectionSelected != null) {
			IRSESystemType sysType = currentConnectionSelected.getSystemType();
			if (sysType != null && sysType.isEnabled() && !sysType.isLocal()) {
				curConn=currentConnectionSelected.getAliasName();
			}
		} */
		updateOkButton();
	}
	
	protected IProject getCurrentProject() {
		if (projectCombo.getItemCount() == 0)
			return null;
		int currentSelection = projectCombo.getSelectionIndex();
		String importProject = currentSelection >= 0 ? projectCombo
				.getItem(currentSelection) : null;
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = wsroot.getProject(importProject);
        return project;
    }
	
	protected void updateProjectPulldown() {
		int index=-1;
		
		projectCombo.removeAll();
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = wsroot.getProjects();

        for (int i = 0; i < projects.length; ++i) {
        	try {
        		if (projects[i].hasNature(TmfProjectNature.ID)) {
        			String projName = projects[i].getName();
        			projectCombo.add(projName);
        			if (curProject != null) 
        				if (projName.matches(curProject))
                   			index = i;
        		}
        	} catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        	}
        }
		
		if(index>=0) {
			projectCombo.select(index);
		}else if (projectCombo.getItemCount()> 0) {
			projectCombo.select(projectCombo.getItemCount() - 1);
		}
	
		//TODO
		//connectionCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT,true);
		projectCombo.pack(true);
		projectCombo.layout();
		projectCombo.getParent().layout();
		
		updateCurProject();
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

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

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.yocto.remote.utils.CommonHelper;
import org.yocto.sdk.remotetools.Activator;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.SWTFactory;

public class SystemtapSettingDialog extends Dialog {

	static protected String TITLE="Systemtap Crosstap";
	protected String title;
	protected String metadata_location;
        protected String builddir_location;
	protected String systemtap_script;
	protected String user_id;
	protected String remote_host;
	protected String systemtap_args;
	protected boolean okPressed;
	protected Button metadataLocationBtn;
        protected Button builddirLocationBtn;
	protected Button systemtapScriptBtn;
	protected Text userIDText;
	protected Text remoteHostText;
	protected Text systemtapArgsText;
	protected Text systemtapScriptText;
	protected Text metadataLocationText;
        protected Text builddirLocationText;
	
	protected SystemtapSettingDialog(Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
		this.okPressed = false;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	public boolean isOKPressed() {
		return okPressed;
	}
	
	public String getSystemtapScript() {
		return systemtap_script;
	}
	
	public String getMetadataLocation() {
		return metadata_location;
	}

        public String getBuilddirLocation() {
                return builddir_location;
        }
	
	public String getRemoteHost() {
		return remote_host;
	}
	
	public String getUserID() {
		return user_id;
	}
	
	public String getSystemtapArgs() {
		return systemtap_args;
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp=(Composite)super.createDialogArea(parent);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		
		/*argument*/
		SWTFactory.createVerticalSpacer(comp, 1);
		createInternal(comp);
		
		return comp;
	}
	
	protected void createInternal(Composite parent)
	{
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 2;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projComp.setLayoutData(gd);
		
		Label label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Metadata_Location);
		Composite textContainer = new Composite(projComp, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		metadataLocationText = (Text)addTextControl(textContainer, metadata_location);
		metadataLocationBtn = addDirSelectButton(textContainer, metadataLocationText);
		
		label = new Label(projComp, SWT.NONE);
                label.setText(Messages.Builddir_Location);
                textContainer = new Composite(projComp, SWT.NONE);
                textContainer.setLayout(new GridLayout(2, false));
                textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                builddirLocationText = (Text)addTextControl(textContainer, builddir_location);
                builddirLocationBtn = addDirSelectButton(textContainer, builddirLocationText);

                label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Remote_User_ID);
		userIDText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		
		if(user_id!=null)
			userIDText.setText(user_id);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		userIDText.setLayoutData(gd);
		
		label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Remote_Host);
		
		remoteHostText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		if(remote_host != null)
			remoteHostText.setText(remote_host);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		remoteHostText.setLayoutData(gd);
		
		label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Systemtap_Script);
		textContainer = new Composite(projComp, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		systemtapScriptText = (Text)addTextControl(textContainer, systemtap_script);
		systemtapScriptBtn = addFileSelectButton(textContainer, systemtapScriptText);
		
		label = new Label(projComp, SWT.NONE);
		label.setText(Messages.Systemtap_Args);
		
		systemtapArgsText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		if(systemtap_args != null)
			systemtapArgsText.setText(systemtap_args);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		systemtapArgsText.setLayoutData(gd);
	}

	private Control addTextControl(final Composite parent, String value) {
		final Text text;

		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (value != null)
			text.setText(value);
		text.setSize(10, 150);
		return (Control)text;
	}

	private Button addDirSelectButton(final Composite parent, final Text text) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText("Browse");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName;
				
				dirName = new DirectoryDialog(parent.getShell()).open();
				if (dirName != null) {
					text.setText(dirName);
				}
			}
		});
		return button;
	}		

	private Button addFileSelectButton(final Composite parent, final Text text) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText("Browse");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String fileName;
				
				fileName = new FileDialog(parent.getShell()).open();
				if (fileName != null) {
					text.setText(fileName);
				}
			}
		});
		return button;
	}		
	
 	@Override
	protected void okPressed() {
		@SuppressWarnings("unused")
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		metadata_location = metadataLocationText.getText();
		if ( (metadata_location == null) || metadata_location.isEmpty()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "Please specify your metadata location!");
			return;
		} 
		File metadata_dir = new File(metadata_location);
		if (!metadata_dir.exists()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "The specified metadata location does not exist!");
			return;
		}
		if (!metadata_dir.isDirectory()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "The specified metadata location is not a directory!");
			return;
		}
                builddir_location = builddirLocationText.getText();
                if ( (builddir_location == null) || builddir_location.isEmpty()) {
                        CommonHelper.showErrorDialog("SystemTap Error", null, "Please specify your builddir location!");
                        return;
                }
                File builddir_dir = new File(builddir_location);
                if (!builddir_dir.exists()) {
                        CommonHelper.showErrorDialog("SystemTap Error", null, "The specified builddir location does not exist!");
                }
                if (!metadata_dir.isDirectory()) {
                        CommonHelper.showErrorDialog("SystemTap Error", null, "The specified builddir location is not a directory!");
                        return;
                }
		user_id = userIDText.getText();
		if ( (user_id == null) || user_id.isEmpty()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "Please specify remote user id!");
			return;
		}
		
		remote_host = remoteHostText.getText();
		if ( (remote_host == null) || remote_host.isEmpty()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "Please specify remote host IP!");
			return;
		}
		
		systemtap_script = systemtapScriptText.getText();
		if ( (systemtap_script == null) || systemtap_script.isEmpty()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "Please specify your systemtap script");
			return;
		}
	    File script_file = new File(systemtap_script);
	    if (!script_file.exists()) {
	    	CommonHelper.showErrorDialog("SystemTap Error", null, "The specified systemtap script does not exist!");
			return;
	    }
	    if (!script_file.isFile()) {
	    	CommonHelper.showErrorDialog("SystemTap Error", null, "The specified systemtap script is not a file!");
			return;
	    }
	    systemtap_args = systemtapArgsText.getText();
	    okPressed = true;
		super.okPressed();
	}
}

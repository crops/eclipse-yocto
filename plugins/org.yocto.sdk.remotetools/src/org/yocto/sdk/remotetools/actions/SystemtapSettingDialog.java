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

//import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.yocto.sdk.remotetools.Activator;
import org.yocto.sdk.remotetools.CommonHelper;
import org.yocto.sdk.remotetools.Messages;
import org.yocto.sdk.remotetools.SWTFactory;

public class SystemtapSettingDialog extends SimpleSettingDialog {

	static protected String TITLE="Systemtap";
	
	protected String KO_value="";
	protected Button kernelModuleBtn;
	protected Text kernelModuleText;
	
	protected SystemtapSettingDialog(Shell parentShell, String title, String conn) {
		super(parentShell,title,conn);
	}
	
	public SystemtapSettingDialog(Shell parentShell) {
		this(parentShell,
				TITLE,
				Activator.getDefault().getDialogSettings().get(IBaseConstants.CONNECTION_NAME_SYSTEMTAP)
				);
	}
	
	public String getKernelModule() {
		return KO_value;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp=(Composite)super.createDialogArea(parent);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);
		
		/*argument*/
		SWTFactory.createVerticalSpacer(comp, 1);
		createInternal(comp);
		
		updateOkButton();
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
		label.setText(Messages.Systemtap_KO_Text);
		Composite textContainer = new Composite(projComp, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		kernelModuleText = (Text)addTextControl(textContainer, KO_value);
		kernelModuleBtn = addFileSelectButton(textContainer, kernelModuleText);
	}

	private Control addTextControl(final Composite parent, String value) {
		final Text text;

		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		text.setSize(10, 150);
		return (Control)text;
		//return addControls((Control)text, key, value);
	}

	private Button addFileSelectButton(final Composite parent, final Text text) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		//button.setText(InputUIElement.BROWSELABEL);
		button.setText("Browser");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName;
				
				dirName = new FileDialog(parent.getShell()).open();
				if (dirName != null) {
					text.setText(dirName);
				}
			}
		});
		return button;
	}		

	@Override
	protected void okPressed() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
	    // store the value of the generate sections checkbox
		if(getCurrentConnection()==null) {
			settings.put(IBaseConstants.CONNECTION_NAME_SYSTEMTAP,
					(String)null);
		}else {
			settings.put(IBaseConstants.CONNECTION_NAME_SYSTEMTAP, 
					getCurrentConnection().getAliasName());
		}
	
		KO_value=kernelModuleText.getText();
		if ((KO_value == null) || KO_value.isEmpty()) {
			CommonHelper.showErrorDialog("SystemTap Error", null, "Missing kernel module!");
			return;
		}
		super.okPressed();
	}
}

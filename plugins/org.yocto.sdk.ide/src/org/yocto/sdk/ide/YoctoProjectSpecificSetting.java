/*******************************************************************************
 * Copyright (c) 2012 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.yocto.sdk.ide.preferences.YoctoSDKProjectPropertyPage;

public class YoctoProjectSpecificSetting {
	private static final String PROJECT_SPECIFIC_TITLE = "Preferences.Profile.ProjectSpecific.Title";
	private static final String PROJECT_SPECIFIC_GROUP_TITLE = "Preferences.Profile.ProjectSpecific.Group.Title";

	private YoctoProfileSetting yoctoConfigurationsSetting;
	private YoctoUISetting yoctoUISetting;

	private Button btnUseProjectSpecificSettingsCheckbox;
	private PreferencePage preferencePage;

	public YoctoProjectSpecificSetting(YoctoProfileSetting yoctoConfigurationsSetting,
						YoctoUISetting yoctoUISetting, PreferencePage preferencePage) {
		this.yoctoConfigurationsSetting = yoctoConfigurationsSetting;
		this.yoctoUISetting = yoctoUISetting;
		this.preferencePage = preferencePage;
	}

	public void createComposite(Composite composite) {
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);

		Group storeYoctoConfigurationsGroup = new Group (composite, SWT.NONE);
		layout = new GridLayout(2, false);
		storeYoctoConfigurationsGroup.setLayout(layout);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		storeYoctoConfigurationsGroup.setLayoutData(gd);
		storeYoctoConfigurationsGroup.setText(YoctoSDKMessages.getString(PROJECT_SPECIFIC_GROUP_TITLE));

		btnUseProjectSpecificSettingsCheckbox = new Button(storeYoctoConfigurationsGroup, SWT.CHECK);
		btnUseProjectSpecificSettingsCheckbox.setText(YoctoSDKMessages.getString(PROJECT_SPECIFIC_TITLE));
		btnUseProjectSpecificSettingsCheckbox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnUseProjectSpecificSettingsCheckbox.getSelection()){
					yoctoConfigurationsSetting.setUIFormEnabledState(false);
					yoctoUISetting.setUIFormEnabledState(true);

					if (preferencePage instanceof YoctoSDKProjectPropertyPage) {
						((YoctoSDKProjectPropertyPage) preferencePage).switchToProjectSpecificProfile();
					}
				} else {
					yoctoConfigurationsSetting.setUIFormEnabledState(true);
					yoctoConfigurationsSetting.setButtonsEnabledState(false);
					yoctoUISetting.setUIFormEnabledState(false);

					if (preferencePage instanceof YoctoSDKProjectPropertyPage) {
						((YoctoSDKProjectPropertyPage) preferencePage).switchToSelectedProfile();
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}

	public void setUseProjectSpecificSettings(boolean isUsingProjectSpecificSettings) {
		btnUseProjectSpecificSettingsCheckbox.setSelection(isUsingProjectSpecificSettings);
	}

	public boolean isUsingProjectSpecificSettings() {
		return btnUseProjectSpecificSettingsCheckbox.getSelection();
	}
}

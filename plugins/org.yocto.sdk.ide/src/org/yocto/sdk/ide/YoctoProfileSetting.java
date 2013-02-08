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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

public class YoctoProfileSetting {
	private static final String PROFILES_TITLE = "Preferences.Profiles.Title";
	private static final String NEW_PROFILE_TITLE = "Preferences.Profile.New.Title";
	private static final String RENAME_PROFILE_TITLE = "Preferences.Profile.Rename.Title";
	private static final String REMOVE_PROFILE_TITLE = "Preferences.Profile.Remove.Title";

	private Combo sdkConfigsCombo;
	private Button btnConfigRename;
	private Button btnConfigRemove;
	private Button btnConfigSaveAs;

	private YoctoProfileElement profileElement;
	private PreferencePage preferencePage;

	public YoctoProfileSetting(YoctoProfileElement profileElement, PreferencePage preferencePage) {
		this.profileElement = profileElement;
		this.preferencePage = preferencePage;
	}

	public void createComposite(Composite composite) {
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		Group storeYoctoConfigurationsGroup = new Group (composite, SWT.NONE);
		layout = new GridLayout(3, false);
		storeYoctoConfigurationsGroup.setLayout(layout);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		storeYoctoConfigurationsGroup.setLayoutData(gd);
		storeYoctoConfigurationsGroup.setText(YoctoSDKMessages.getString(PROFILES_TITLE));

		sdkConfigsCombo = new Combo(storeYoctoConfigurationsGroup, SWT.READ_ONLY);
		addConfigs(sdkConfigsCombo);
		sdkConfigsCombo.select(sdkConfigsCombo.indexOf(profileElement.getSelectedProfile()));
		sdkConfigsCombo.setLayout(new GridLayout(2, false));
		sdkConfigsCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));

		Listener selectionListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				Object source = event.widget;
				if (!(source instanceof Combo)) {
					return;
				}

				Combo sdkCombo = (Combo) source;
				if (sdkCombo.getSelectionIndex() < 0) {
					return;
				}

				String selectedItem = sdkCombo.getItem(sdkCombo.getSelectionIndex());
				profileElement.setSelectedProfile(selectedItem);
			}
		};

		sdkConfigsCombo.addListener(SWT.Selection, selectionListener);
		sdkConfigsCombo.addListener(SWT.Modify, selectionListener);

		createSaveAsProfileButton(storeYoctoConfigurationsGroup);
		createRenameButton(storeYoctoConfigurationsGroup);
		createRemoveButton(storeYoctoConfigurationsGroup);
	}

	private void createSaveAsProfileButton(Group storeYoctoConfigurationsGroup) {
		btnConfigSaveAs = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigSaveAs.setText(YoctoSDKMessages.getString(NEW_PROFILE_TITLE));
	}

	private void createRemoveButton(Group storeYoctoConfigurationsGroup) {
		btnConfigRemove = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigRemove.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false, 3, 1));
		btnConfigRemove.setText(YoctoSDKMessages.getString(REMOVE_PROFILE_TITLE));
	}

	private void createRenameButton(Group storeYoctoConfigurationsGroup) {
		btnConfigRename = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigRename.setText(YoctoSDKMessages.getString(RENAME_PROFILE_TITLE));
	}

	private void saveChangesOnCurrentProfile() {
		preferencePage.performOk();
	}

	private void addConfigs(Combo combo) {
		for (String profile : profileElement.getProfiles()) {
			combo.add(profile);
		}
	}

	public void setUIFormEnabledState(boolean isEnabled) {
		setButtonsEnabledState(isEnabled);
		sdkConfigsCombo.setEnabled(isEnabled);
	}

	public YoctoProfileElement getCurrentInput() {
		return profileElement;
	}

	public void setButtonsEnabledState(boolean isEnabled) {
		btnConfigRename.setEnabled(isEnabled);
		btnConfigRemove.setEnabled(isEnabled);
		btnConfigSaveAs.setEnabled(isEnabled);
	}
}

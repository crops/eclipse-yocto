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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.yocto.sdk.ide.preferences.PreferenceConstants;
import org.yocto.sdk.ide.preferences.ProfileNameInputValidator;
import org.yocto.sdk.ide.preferences.YoctoSDKPreferencePage;
import org.yocto.sdk.ide.preferences.YoctoSDKProjectPropertyPage;

public class YoctoProfileSetting {
	private static final String PROFILES_TITLE = "Preferences.Profiles.Title";
	private static final String NEW_PROFILE_TITLE = "Preferences.Profile.New.Title";
	private static final String RENAME_PROFILE_TITLE = "Preferences.Profile.Rename.Title";
	private static final String RENAME_DIALOG_TITLE = "Preferences.Profile.Rename.Dialog.Title";
	private static final String RENAME_DIALOG_MESSAGE = "Preferences.Profile.Rename.Dialog.Message";
	private static final String REMOVE_PROFILE_TITLE = "Preferences.Profile.Remove.Title";
	private static final String REMOVE_DIALOG_TITLE = "Preferences.Profile.Remove.Dialog.Title";
	private static final String REMOVE_DIALOG_MESSAGE = "Preferences.Profile.Remove.Dialog.Message";
	private static final String MODIFY_STANDARD_TITLE = "Preferences.Profile.Standard.Modification.Title";
	private static final String MODIFY_STANDARD_MESSAGE = "Preferences.Profile.Standard.Modification.Message";

	private Combo sdkConfigsCombo;
	private Button btnConfigRename;
	private Button btnConfigRemove;
	private Button btnConfigSaveAs;

	private YoctoProfileElement profileElement;
	private PreferencePage preferencePage;
	private final boolean editable;

	public YoctoProfileSetting(YoctoProfileElement profileElement, PreferencePage preferencePage, final boolean editable) {
		this.profileElement = profileElement;
		this.preferencePage = preferencePage;
		this.editable = editable;
	}

	public void createComposite(Composite composite) {
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		Group storeYoctoConfigurationsGroup = new Group (composite, SWT.NONE);
		layout = new GridLayout(1, false);
		if (isEditable()) {
			layout.numColumns = 3;
		}

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

				if (preferencePage instanceof YoctoSDKPreferencePage) {
					((YoctoSDKPreferencePage) preferencePage).switchProfile(selectedItem);
				} else if (preferencePage instanceof YoctoSDKProjectPropertyPage) {
					((YoctoSDKProjectPropertyPage) preferencePage).switchProfile(selectedItem);
				}
			}
		};

		sdkConfigsCombo.addListener(SWT.Selection, selectionListener);
		sdkConfigsCombo.addListener(SWT.Modify, selectionListener);

		if (isEditable()) {
			createSaveAsProfileButton(storeYoctoConfigurationsGroup);
			createRenameButton(storeYoctoConfigurationsGroup);
			createRemoveButton(storeYoctoConfigurationsGroup);
		}
	}

	private void createSaveAsProfileButton(Group storeYoctoConfigurationsGroup) {
		btnConfigSaveAs = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigSaveAs.setText(YoctoSDKMessages.getString(NEW_PROFILE_TITLE));
		btnConfigSaveAs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (preferencePage instanceof YoctoSDKPreferencePage) {
					((YoctoSDKPreferencePage) preferencePage).performSaveAs();
				}
			}
		});
	}

	private void createRemoveButton(Group storeYoctoConfigurationsGroup) {
		btnConfigRemove = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigRemove.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false, 3, 1));
		btnConfigRemove.setText(YoctoSDKMessages.getString(REMOVE_PROFILE_TITLE));
		btnConfigRemove.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				saveChangesOnCurrentProfile();
				int selectionIndex = sdkConfigsCombo.getSelectionIndex();
				String selectedItem = sdkConfigsCombo.getItem(selectionIndex);

				if (selectedItem.equals(PreferenceConstants.STANDARD_PROFILE_NAME)) {
					MessageDialog.openInformation(null,
													YoctoSDKMessages.getString(MODIFY_STANDARD_TITLE),
													YoctoSDKMessages.getString(MODIFY_STANDARD_MESSAGE));
					return;
				}

				boolean deleteConfirmed =
						MessageDialog.openConfirm(null,
													YoctoSDKMessages.getString(REMOVE_DIALOG_TITLE),
													YoctoSDKMessages.getFormattedString(REMOVE_DIALOG_MESSAGE, selectedItem));

				if (!deleteConfirmed) {
					return;
				}

				sdkConfigsCombo.select(0);
				sdkConfigsCombo.remove(selectionIndex);
				profileElement.remove(selectedItem);

				if (preferencePage instanceof YoctoSDKPreferencePage) {
					((YoctoSDKPreferencePage) preferencePage).deleteProfile(selectedItem);
				}
			}
		});
	}

	private void createRenameButton(Group storeYoctoConfigurationsGroup) {
		btnConfigRename = new Button(storeYoctoConfigurationsGroup, SWT.PUSH | SWT.LEAD);
		btnConfigRename.setText(YoctoSDKMessages.getString(RENAME_PROFILE_TITLE));
		btnConfigRename.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				saveChangesOnCurrentProfile();
				int selectedIndex = sdkConfigsCombo.getSelectionIndex();
				final String selectedItem = sdkConfigsCombo.getItem(selectedIndex);

				if (selectedItem.equals(PreferenceConstants.STANDARD_PROFILE_NAME)) {
					MessageDialog.openInformation(null,
							YoctoSDKMessages.getString(MODIFY_STANDARD_TITLE),
							YoctoSDKMessages.getString(MODIFY_STANDARD_MESSAGE));
					return;
				}

				InputDialog profileNameDialog =
						new InputDialog(null,
										YoctoSDKMessages.getString(RENAME_DIALOG_TITLE),
										YoctoSDKMessages.getString(RENAME_DIALOG_MESSAGE),
										null,
										new ProfileNameInputValidator(profileElement, selectedItem));

				int returnCode = profileNameDialog.open();
				if (returnCode == IDialogConstants.CANCEL_ID) {
					return;
				}

				String newProfileName = profileNameDialog.getValue();
				profileElement.rename(selectedItem, profileNameDialog.getValue());

				if (preferencePage instanceof YoctoSDKPreferencePage) {
					((YoctoSDKPreferencePage) preferencePage).renameProfile(selectedItem, newProfileName);
				}

				sdkConfigsCombo.setItem(selectedIndex, newProfileName);
				sdkConfigsCombo.select(selectedIndex);
			}
		});
	}

	private void saveChangesOnCurrentProfile() {
		preferencePage.performOk();
	}

	private void addConfigs(Combo combo) {
		for (String profile : profileElement.getProfiles()) {
			combo.add(profile);
		}
	}

	public void addProfile(String profileName) {
		int index = sdkConfigsCombo.getItemCount();
		sdkConfigsCombo.add(profileName, index);
		sdkConfigsCombo.select(index);
	}

	public void setUIFormEnabledState(boolean isEnabled) {
		setButtonsEnabledState(isEnabled);
		sdkConfigsCombo.setEnabled(isEnabled);
	}

	public YoctoProfileElement getCurrentInput() {
		return profileElement;
	}

	public void setButtonsEnabledState(boolean isEnabled) {
		if (isEditable()) {
			btnConfigRename.setEnabled(isEnabled);
			btnConfigRemove.setEnabled(isEnabled);
			btnConfigSaveAs.setEnabled(isEnabled);
		}
	}

	private boolean isEditable() {
		return editable;
	}
}

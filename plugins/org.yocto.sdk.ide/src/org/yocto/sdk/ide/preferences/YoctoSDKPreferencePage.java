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
package org.yocto.sdk.ide.preferences;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoProfileSetting;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.natures.YoctoSDKProjectNature;
import org.yocto.sdk.ide.utils.ProjectPreferenceUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

public class YoctoSDKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String NEW_DIALOG_TITLE = "Preferences.Profile.New.Dialog.Title";
	private static final String NEW_DIALOG_MESSAGE = "Preferences.Profile.New.Dialog.Message";
	private static final String UPDATE_DIALOG_TITLE = "Preferences.Profile.Update.Dialog.Title";
	private static final String UPDATE_DIALOG_MESSAGE = "Preferences.Profile.Update.Dialog.Message";
	private static final String REVALIDATION_MESSAGE = "Yocto.SDK.Revalidation.Message";

	private YoctoProfileSetting yoctoProfileSetting;
	private YoctoUISetting yoctoUISetting;

	private Listener changeListener;

	public YoctoSDKPreferencePage() {
		//super(GRID);
		IPreferenceStore defaultStore = YoctoSDKPlugin.getDefault().getPreferenceStore();
		String profiles = defaultStore.getString(PreferenceConstants.PROFILES);
		String selectedProfile = defaultStore.getString(PreferenceConstants.SELECTED_PROFILE);

		if (profiles.isEmpty()) {
			profiles = defaultStore.getDefaultString(PreferenceConstants.PROFILES);
			selectedProfile = defaultStore.getDefaultString(PreferenceConstants.SELECTED_PROFILE);
		}

		setPreferenceStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile));
		//setDescription(YoctoSDKMessages.getString(PREFERENCES_Yocto_CONFIG));
		YoctoUIElement elem = YoctoSDKUtils.getElemFromStore(getPreferenceStore());
		this.yoctoUISetting = new YoctoUISetting(elem);

		YoctoProfileElement profileElement = new YoctoProfileElement(profiles, selectedProfile);
		this.yoctoProfileSetting = new YoctoProfileSetting(profileElement, this, true);

		changeListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				setErrorMessage(null);
				setMessage(YoctoSDKMessages.getString(REVALIDATION_MESSAGE), INFORMATION);
			}
		};
	}

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		yoctoProfileSetting.createComposite(composite);
		yoctoUISetting.createComposite(composite);

		composite.addListener(SWT.Modify, changeListener);
		composite.addListener(SWT.Selection, changeListener);

		SDKCheckResults result = yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
		if (result != SDKCheckResults.SDK_PASS) {
		}

		Dialog.applyDialogFont(composite);
		return composite;
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		clearMessages();

		SDKCheckResults result = yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
		if (result != SDKCheckResults.SDK_PASS) {
			setErrorMessage(result.getMessage());
			return false;
		}

		YoctoUIElement savedElement = YoctoSDKUtils.getElemFromStore(getPreferenceStore());
		YoctoUIElement modifiedElement = yoctoUISetting.getCurrentInput();

		YoctoProfileElement savedProfileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		YoctoProfileElement profileElement = yoctoProfileSetting.getCurrentInput();

		if (savedElement.equals(modifiedElement) &&
				profileElement.getSelectedProfile().equals(savedProfileElement.getSelectedProfile())) {
			return true;
		}

		HashSet<IProject> yoctoProjects = getAffectedProjects(profileElement.getSelectedProfile());

		if (!yoctoProjects.isEmpty()) {
			boolean deleteConfirmed =
					MessageDialog.openConfirm(null, YoctoSDKMessages.getString(UPDATE_DIALOG_TITLE),
							YoctoSDKMessages.getFormattedString(UPDATE_DIALOG_MESSAGE, profileElement.getSelectedProfile()));

			if (!deleteConfirmed) {
				return false;
			}
		}

		saveElemToStore(modifiedElement, getPreferenceStore());
		YoctoSDKUtils.saveProfilesToDefaultStore(profileElement);

		updateProjects(yoctoProjects, modifiedElement);

		return super.performOk();
	}

	private void clearMessages() {
		setErrorMessage(null);
		setMessage(null);
		setTitle(getTitle());
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		YoctoUIElement defaultElement = YoctoSDKUtils.getDefaultElemFromDefaultStore();
		yoctoUISetting.setCurrentInput(defaultElement);
		super.performDefaults();
	}

	public void performSaveAs() {
		YoctoProfileElement profileElement = yoctoProfileSetting.getCurrentInput();
		YoctoUIElement uiElement = yoctoUISetting.getCurrentInput();

		SDKCheckResults result = yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, true);
		if (result != SDKCheckResults.SDK_PASS) {
			setErrorMessage(result.getMessage());
			return;
		}

		InputDialog profileNameDialog =
							new InputDialog(null,
											YoctoSDKMessages.getString(NEW_DIALOG_TITLE),
											YoctoSDKMessages.getString(NEW_DIALOG_MESSAGE),
											null,
											new ProfileNameInputValidator(profileElement));

		int returnCode = profileNameDialog.open();
		if (returnCode == IDialogConstants.CANCEL_ID) {
			return;
		}

		profileElement.addProfile(profileNameDialog.getValue());
		yoctoProfileSetting.addProfile(profileNameDialog.getValue());

		yoctoUISetting.setCurrentInput(uiElement);
		performOk();
	}

	public void switchProfile(String selectedProfile) {
		setPreferenceStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile));
		YoctoUIElement profileElement = YoctoSDKUtils.getElemFromStore(getPreferenceStore());
		yoctoUISetting.setCurrentInput(profileElement);
	}

	public void renameProfile(String oldProfileName, String newProfileName) {
		YoctoUIElement oldProfileElement = YoctoSDKUtils.getElemFromStore(YoctoSDKPlugin.getProfilePreferenceStore(oldProfileName));
		saveElemToStore(oldProfileElement, YoctoSDKPlugin.getProfilePreferenceStore(newProfileName));

		renameProfileInAffectedProjects(oldProfileName, newProfileName);
	}

	public void deleteProfile(String selectedProfile)
	{
		resetProfileInAffectedProjects(selectedProfile);
	}

	/* Save IDE wide Yocto Preference settings to a specific preference store */
	private void saveElemToStore(YoctoUIElement elem, IPreferenceStore store) {
		store.setValue(PreferenceConstants.TARGET_ARCH_INDEX, elem.getIntTargetIndex());

		if (elem.getEnumYoctoMode() == YoctoUIElement.YoctoMode.YOCTO_SDK_MODE) {
			store.setValue(PreferenceConstants.SDK_MODE, IPreferenceStore.TRUE);
		} else {
			store.setValue(PreferenceConstants.SDK_MODE, IPreferenceStore.FALSE);
		}
		store.setValue(PreferenceConstants.QEMU_KERNEL, elem.getStrQemuKernelLoc());
		store.setValue(PreferenceConstants.QEMU_OPTION, elem.getStrQemuOption());
		store.setValue(PreferenceConstants.SYSROOT, elem.getStrSysrootLoc());
		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
			store.setValue(PreferenceConstants.TARGET_MODE, IPreferenceStore.TRUE);
		} else {
			store.setValue(PreferenceConstants.TARGET_MODE, IPreferenceStore.FALSE);
		}
		store.setValue(PreferenceConstants.TOOLCHAIN_ROOT, elem.getStrToolChainRoot());
		store.setValue(PreferenceConstants.TOOLCHAIN_TRIPLET, elem.getStrTarget());
	}

	private void resetProfileInAffectedProjects(String usedProfile)
	{
		HashSet<IProject> yoctoProjects = getAffectedProjects(usedProfile);
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		profileElement.setSelectedProfile(PreferenceConstants.STANDARD_PROFILE_NAME);

		for (IProject project : yoctoProjects)
		{
			ProjectPreferenceUtils.saveProfiles(profileElement, project);
			YoctoUIElement elem = YoctoSDKUtils.getElemFromStore(
											YoctoSDKPlugin.getProfilePreferenceStore(PreferenceConstants.STANDARD_PROFILE_NAME));
			ProjectPreferenceUtils.saveElemToProjectEnv(elem, project);
		}
	}

	private void renameProfileInAffectedProjects(String oldProfileName, String newProfileName) {
		HashSet<IProject> yoctoProjects = getAffectedProjects(oldProfileName);
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		profileElement.setSelectedProfile(newProfileName);

		for (IProject project : yoctoProjects)
		{
			ProjectPreferenceUtils.saveProfiles(profileElement, project);
		}
	}

	private void updateProjects(HashSet<IProject> yoctoProjects, YoctoUIElement elem) {
		for (IProject project : yoctoProjects)
		{
			ProjectPreferenceUtils.saveElemToProjectEnv(elem, project);
		}
	}

	private HashSet<IProject> getAffectedProjects(String usedProfile)
	{
		HashSet<IProject> yoctoProjects = new HashSet<IProject>();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

		for (IProject project : projects)
		{
			try
			{
				if (!project.hasNature(YoctoSDKProjectNature.YoctoSDK_NATURE_ID)) {
					continue;
				}
			} catch (CoreException e)
			{
				// project is closed or does not exist so don't update
				continue;
			}

			if (!projectUsesProfile(project, usedProfile)) {
				continue;
			}

			yoctoProjects.add(project);
		}
		return yoctoProjects;
	}

	private boolean projectUsesProfile(IProject project, String profile)
	{
		YoctoProfileElement profileElement = ProjectPreferenceUtils.getProfiles(project);

		if (!profileElement.getSelectedProfile().equals(profile)) {
			return false;
		}

		return true;
	}
}

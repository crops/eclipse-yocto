/*******************************************************************************
 * Copyright (c) 2012 BMW Car IT GmbH.
 * Copyright (c) 2010 Intel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT GmbH - initial implementation
 * Intel - initial API implementation (copied from YoctoSDKPreferencePage)
 *******************************************************************************/
package org.yocto.sdk.ide.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoProfileSetting;
import org.yocto.sdk.ide.YoctoProjectSpecificSetting;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtilsConstants;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

public class YoctoSDKProjectPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private static final String REVALIDATION_MESSAGE = "Poky.SDK.Revalidation.Message";

	private YoctoProfileSetting yoctoProfileSetting;
	private YoctoProjectSpecificSetting yoctoProjectSpecificSetting;
	private YoctoUISetting yoctoUISetting;
	private IProject project = null;

	private Listener changeListener;

	public YoctoSDKProjectPropertyPage() {
		changeListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (getErrorMessage() != null) {
					setErrorMessage(null);
					setMessage(YoctoSDKMessages.getString(REVALIDATION_MESSAGE), INFORMATION);
				}
			}
		};
	}

	@Override
	protected Control createContents(Composite parent) {
		IProject project = getProject();

		YoctoProfileElement globalProfileElement= YoctoSDKUtils.getProfilesFromDefaultStore();
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromProjectPreferences(project);

		String selectedProfile = profileElement.getSelectedProfile();
		if (!globalProfileElement.contains(selectedProfile)) {
			selectedProfile = globalProfileElement.getSelectedProfile();
		}

		yoctoProfileSetting = new YoctoProfileSetting(
				new YoctoProfileElement(globalProfileElement.getProfilesAsString(), selectedProfile), this, false);
		boolean useProjectSpecificSetting = getUseProjectSpecificOptionFromProjectPreferences(project);

		if (useProjectSpecificSetting) {
			yoctoUISetting = new YoctoUISetting(getElemFromProjectPreferences(project));
		} else {
			yoctoUISetting = new YoctoUISetting(YoctoSDKUtils.getElemFromStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile)));
		}

		yoctoProjectSpecificSetting = new YoctoProjectSpecificSetting(yoctoProfileSetting, yoctoUISetting, this);

		initializeDialogUnits(parent);
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		yoctoProfileSetting.createComposite(composite);
		yoctoProjectSpecificSetting.createComposite(composite);
		yoctoUISetting.createComposite(composite);

		if (useProjectSpecificSetting) {
			yoctoProfileSetting.setUIFormEnabledState(false);
			yoctoProjectSpecificSetting.setUseProjectSpecificSettings(true);
			yoctoUISetting.setUIFormEnabledState(true);

			SDKCheckResults result = yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
			if (result != SDKCheckResults.SDK_PASS) {
				setErrorMessage(result.getMessage());
			}
		} else {
			yoctoProfileSetting.setUIFormEnabledState(true);
			yoctoProjectSpecificSetting.setUseProjectSpecificSettings(false);
			yoctoUISetting.setUIFormEnabledState(false);
		}

		composite.addListener(SWT.Modify, changeListener);
		composite.addListener(SWT.Selection, changeListener);

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private IProject getProject() {
		if (project != null) {
			return project;
		}

		IAdaptable adaptable = getElement();
		if (adaptable == null) {
			throw new IllegalStateException("Project can only be retrieved after properties page has been set up.");
		}

		project = (IProject) adaptable.getAdapter(IProject.class);
		return project;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		YoctoUIElement defaultElement = YoctoSDKUtils.getDefaultElemFromDefaultStore();
		yoctoUISetting.setCurrentInput(defaultElement);
		yoctoProjectSpecificSetting.setUseProjectSpecificSettings(true);
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		clearMessages();

		IProject project = getProject();

		if (yoctoProjectSpecificSetting.isUsingProjectSpecificSettings()) {
			SDKCheckResults result = yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
			if (result != SDKCheckResults.SDK_PASS) {
				setErrorMessage(result.getMessage());
				return false;
			}

			saveUseProjectSpecificOptionToProjectPreferences(project, true);
			YoctoSDKUtils.saveProfilesToProjectPreferences(yoctoProfileSetting.getCurrentInput(), project);
			saveElemToProjectPreferences(yoctoUISetting.getCurrentInput(), project);
		} else {
			saveUseProjectSpecificOptionToProjectPreferences(project, false);
			YoctoSDKUtils.saveProfilesToProjectPreferences(yoctoProfileSetting.getCurrentInput(), project);
		}

		YoctoSDKUtils.saveElemToProjectEnv(yoctoUISetting.getCurrentInput(), getProject());

		return super.performOk();
	}

	private void saveUseProjectSpecificOptionToProjectPreferences(IProject project, boolean useProjectSpecificSetting) {
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(YoctoSDKUtilsConstants.PROJECT_SCOPE);
		if (projectNode == null) {
			return;
		}

		if (useProjectSpecificSetting) {
			projectNode.put(PreferenceConstants.PROJECT_SPECIFIC_PROFILE, IPreferenceStore.TRUE);
		} else {
			projectNode.put(PreferenceConstants.PROJECT_SPECIFIC_PROFILE, IPreferenceStore.FALSE);
		}

		try {
			projectNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private boolean getUseProjectSpecificOptionFromProjectPreferences(IProject project) {
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(YoctoSDKUtilsConstants.PROJECT_SCOPE);
		if (projectNode == null) {
			return false;
		}

		String useProjectSpecificSettingString = projectNode.get(PreferenceConstants.PROJECT_SPECIFIC_PROFILE, IPreferenceStore.FALSE);

		if (useProjectSpecificSettingString.equals(IPreferenceStore.FALSE)) {
			return false;
		}
		return true;
	}

	/* Save POKY Preference settings to project's preference store */
	private void saveElemToProjectPreferences(YoctoUIElement elem, IProject project) {
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(YoctoSDKUtilsConstants.PROJECT_SCOPE);
		if (projectNode == null) {
			return;
		}

		projectNode.putInt(PreferenceConstants.TARGET_ARCH_INDEX, elem.getIntTargetIndex());
		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
			projectNode.put(PreferenceConstants.SDK_MODE, IPreferenceStore.TRUE);
		} else {
			projectNode.put(PreferenceConstants.SDK_MODE, IPreferenceStore.FALSE);
		}
		projectNode.put(PreferenceConstants.QEMU_KERNEL, elem.getStrQemuKernelLoc());
		projectNode.put(PreferenceConstants.QEMU_OPTION, elem.getStrQemuOption());
		projectNode.put(PreferenceConstants.SYSROOT, elem.getStrSysrootLoc());

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
			projectNode.put(PreferenceConstants.TARGET_MODE, IPreferenceStore.TRUE);
		} else {
			projectNode.put(PreferenceConstants.TARGET_MODE, IPreferenceStore.FALSE);
		}
		projectNode.put(PreferenceConstants.TOOLCHAIN_ROOT, elem.getStrToolChainRoot());
		projectNode.put(PreferenceConstants.TOOLCHAIN_TRIPLET, elem.getStrTarget());

		try {
			projectNode.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	/* Get POKY Preference settings from project's preference store */
	private YoctoUIElement getElemFromProjectPreferences(IProject project) {
		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectNode = projectScope.getNode(YoctoSDKUtilsConstants.PROJECT_SCOPE);
		if (projectNode == null) {
			return YoctoSDKUtils.getElemFromProjectEnv(project);
		}

		YoctoUIElement elem = new YoctoUIElement();
		elem.setStrToolChainRoot(projectNode.get(PreferenceConstants.TOOLCHAIN_ROOT,""));
		elem.setStrTarget(projectNode.get(PreferenceConstants.TOOLCHAIN_TRIPLET,""));
		elem.setStrQemuKernelLoc(projectNode.get(PreferenceConstants.QEMU_KERNEL,""));
		elem.setStrSysrootLoc(projectNode.get(PreferenceConstants.SYSROOT,""));
		elem.setStrQemuOption(projectNode.get(PreferenceConstants.QEMU_OPTION,""));
		String sTemp = projectNode.get(PreferenceConstants.TARGET_ARCH_INDEX,"");
		if (!sTemp.isEmpty()) {
			elem.setIntTargetIndex(Integer.valueOf(sTemp).intValue());
		}

		if (projectNode.get(PreferenceConstants.SDK_MODE,"").equalsIgnoreCase(IPreferenceStore.TRUE)) {
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_SDK_MODE);
		} else {
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_TREE_MODE);
		}

		if(projectNode.get(PreferenceConstants.TARGET_MODE,"").equalsIgnoreCase(IPreferenceStore.TRUE)) {
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		} else {
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);
		}
		return elem;
	}

	private void clearMessages() {
		setErrorMessage(null);
		setMessage(null);
		setTitle(getTitle());
	}

	public void switchProfile(String selectedProfile)
	{
		YoctoUIElement profileElement = YoctoSDKUtils.getElemFromStore(YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile));
		yoctoUISetting.setCurrentInput(profileElement);
	}

	public void switchToProjectSpecificProfile()
	{
		YoctoUIElement profileElement = getElemFromProjectPreferences(getProject());
		yoctoUISetting.setCurrentInput(profileElement);
	}

	public void switchToSelectedProfile()
	{
		switchProfile(yoctoProfileSetting.getCurrentInput().getSelectedProfile());
	}
}

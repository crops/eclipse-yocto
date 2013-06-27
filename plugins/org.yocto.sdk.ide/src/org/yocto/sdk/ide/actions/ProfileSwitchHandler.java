/*******************************************************************************
 * Copyright (c) 2013 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT - initial implementation
 *******************************************************************************/
package org.yocto.sdk.ide.actions;

import java.util.Map;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.menus.UIElement;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoSDKChecker;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.utils.ProjectPreferenceUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;

public class ProfileSwitchHandler extends AbstractHandler implements IElementUpdater {
	private static final String PROJECT_SPECIFIC_ERROR = "Preferences.Profile.ProjectSpecific.Error.Title";
	private static final String PROJECT_SPECIFIC_ERROR_MESSAGE = "Preferences.Profile.ProjectSpecific.Error.Message";

	public static final String PROFILE_SWITCH_COMMAND = "org.yocto.sdk.ide.targetProfile.switch"; //$NON-NLS-N$
	public static final String PROJECT_SPECIFIC_PARAMETER = "##PROJECT_SPECIFIC_PROFILE##"; //$NON-NLS-N$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(HandlerUtil.matchesRadioState(event)) {
			return null;
		}

		String currentState = event.getParameter(RadioState.PARAMETER_ID);
		HandlerUtil.updateRadioState(event.getCommand(), currentState);

		switchProfile(getSelectedProject(event), currentState);

		return null;
	}

	public IProject getSelectedProject(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (selection instanceof ITreeSelection) {
			Object selectedItem = ((ITreeSelection) selection).getFirstElement();
			if (selectedItem instanceof IResource) {
				return ((IResource) selectedItem).getProject();
			} else if (selectedItem instanceof ICElement) {
				ICProject cProject = ((ICElement) selectedItem).getCProject();
				if (cProject != null) {
					return cProject.getProject();
				}
			} else if (selectedItem instanceof IAdaptable) {
				Object projectObject = ((IAdaptable) selectedItem).getAdapter(IProject.class);
				if (projectObject != null && projectObject instanceof IProject) {
					return ((IProject) projectObject);
				}
			}
		}

		return null;
	}

	private void switchProfile(IProject project, String selectedProfile) {
		if (PROJECT_SPECIFIC_PARAMETER.equals(selectedProfile)) {
			YoctoUIElement yoctoUIElement = ProjectPreferenceUtils.getElem(project);
			SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(yoctoUIElement);

			if ((result != SDKCheckResults.SDK_PASS)) {
				Display display = Display.getCurrent();
				ErrorDialog.openError(display.getActiveShell(),
										YoctoSDKMessages.getString(PROJECT_SPECIFIC_ERROR),
										YoctoSDKMessages.getFormattedString(PROJECT_SPECIFIC_ERROR_MESSAGE,
															project.getName()),
										new Status(Status.ERROR, YoctoSDKPlugin.PLUGIN_ID, result.getMessage()));
				return;
			}

			ProjectPreferenceUtils.saveElemToProjectEnv(yoctoUIElement, project);
			ProjectPreferenceUtils.saveUseProjectSpecificOption(project, true);
		} else {
			IPreferenceStore store = YoctoSDKPlugin.getProfilePreferenceStore(selectedProfile);
			YoctoUIElement yoctoUIElement = YoctoSDKUtils.getElemFromStore(store);
			ProjectPreferenceUtils.saveElemToProjectEnv(yoctoUIElement, project);
			ProjectPreferenceUtils.saveUseProjectSpecificOption(project, false);

			YoctoProfileElement profileSettings = ProjectPreferenceUtils.getProfiles(project);
			profileSettings.setSelectedProfile(selectedProfile);
			ProjectPreferenceUtils.saveProfiles(profileSettings, project);
		}
	}

	/*
	 * Workaround for BUG 398647 to allow checking radio items
	 * in a dynamic contribution
	 *
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=398647
	 */
	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
			ICommandService service = (ICommandService) element.getServiceLocator().getService(ICommandService.class);
			String state = (String) parameters.get(RadioState.PARAMETER_ID);
			Command command = service.getCommand(PROFILE_SWITCH_COMMAND);
			State commandState = command.getState(RadioState.STATE_ID);
			if (commandState.getValue().equals(state)) {
				element.setChecked(true);
			}
	}
}

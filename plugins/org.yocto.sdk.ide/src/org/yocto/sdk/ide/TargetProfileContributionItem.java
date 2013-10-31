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
package org.yocto.sdk.ide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.yocto.sdk.ide.actions.ProfileSwitchHandler;
import org.yocto.sdk.ide.utils.ProjectPreferenceUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;

public class TargetProfileContributionItem extends CompoundContributionItem implements IWorkbenchContribution {
	private IServiceLocator serviceLocator;

	public TargetProfileContributionItem() {}

	public TargetProfileContributionItem(String id) {
		super(id);
	}

	protected CommandContributionItem createProfileItem(IServiceLocator serviceLocator,
														String parameter, String label) {
		CommandContributionItemParameter itemParameter;
		itemParameter = new CommandContributionItemParameter(serviceLocator,
														null,
														ProfileSwitchHandler.PROFILE_SWITCH_COMMAND,
														CommandContributionItem.STYLE_RADIO);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put(RadioState.PARAMETER_ID, parameter);

		itemParameter.label = label;
		itemParameter.parameters = params;

		return new CommandContributionItem(itemParameter);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		TreeSet<String> profiles = YoctoSDKUtils.getProfilesFromDefaultStore().getProfiles();
		ArrayList<IContributionItem> items = new ArrayList<IContributionItem>();

		for (String profile : profiles) {
			items.add(createProfileItem(serviceLocator, profile, profile));
		}

		updateSelection(serviceLocator);

		return items.toArray(new IContributionItem[profiles.size()]);
	}

	public IProject getSelectedProject(IServiceLocator serviceLocator) {
		ISelectionService selectionService = (ISelectionService) serviceLocator.getService(ISelectionService.class);
		ISelection selection = selectionService.getSelection();

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

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	protected void updateSelection(IServiceLocator serviceLocator) {
		ICommandService commandService = (ICommandService) serviceLocator.getService(ICommandService.class);
		Command command = commandService.getCommand(ProfileSwitchHandler.PROFILE_SWITCH_COMMAND);
		IProject project = getSelectedProject(serviceLocator);
		if (project == null) {
			return;
		}
		try {
			if (ProjectPreferenceUtils.getUseProjectSpecificOption(project)) {
				HandlerUtil.updateRadioState(command, ProfileSwitchHandler.PROJECT_SPECIFIC_PARAMETER);
				return;
			}

			String selectedProfile = ProjectPreferenceUtils.getProfiles(project).getSelectedProfile();
			HandlerUtil.updateRadioState(command, selectedProfile);
		} catch (ExecutionException e) {
			// ignore
		}
	}
}

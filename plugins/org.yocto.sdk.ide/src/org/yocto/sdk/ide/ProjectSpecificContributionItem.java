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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.actions.ProfileSwitchHandler;
import org.yocto.sdk.ide.utils.ProjectPreferenceUtils;

public class ProjectSpecificContributionItem extends TargetProfileContributionItem {
	private static final String PROJECT_SPECIFIC_PROFILE =
			"Preferences.Profile.ProjectSpecific.Profile.Label"; //$NON-NLS-N$
	private static final String DISABLED_COMMAND_ID = "org.yocto.sdk.ide.command.disabled"; //$NON-NLS-N$

	private IServiceLocator serviceLocator;

	public ProjectSpecificContributionItem() {}

	public ProjectSpecificContributionItem(String id) {
		super(id);
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		ArrayList<IContributionItem> items = new ArrayList<IContributionItem>();

		IProject project = getSelectedProject(serviceLocator);
		YoctoUIElement yoctoUIElement = ProjectPreferenceUtils.getElem(project);
		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(yoctoUIElement);

		if ((result != SDKCheckResults.SDK_PASS)) {
			CommandContributionItemParameter parameter = new CommandContributionItemParameter(serviceLocator,
															null,
															DISABLED_COMMAND_ID,
															CommandContributionItem.STYLE_PUSH);

			parameter.label = YoctoSDKMessages.getString(PROJECT_SPECIFIC_PROFILE);

			items.add(new CommandContributionItem(parameter));
		} else {
			items.add(super.createProfileItem(serviceLocator, ProfileSwitchHandler.PROJECT_SPECIFIC_PARAMETER,
												YoctoSDKMessages.getString(PROJECT_SPECIFIC_PROFILE)));
		}

		updateSelection(serviceLocator);

		return items.toArray(new IContributionItem[items.size()]);
	}

	@Override
	public void initialize(IServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}
}

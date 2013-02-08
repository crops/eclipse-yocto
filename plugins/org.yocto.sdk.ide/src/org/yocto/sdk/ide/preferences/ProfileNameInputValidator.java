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
package org.yocto.sdk.ide.preferences;

import org.eclipse.jface.dialogs.IInputValidator;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoSDKMessages;

public class ProfileNameInputValidator implements IInputValidator {
	private static final String WARNING_CONTAINS_COMMA = "Preferences.Profile.Validator.InvalidName.Comma";
	private static final String WARNING_CONTAINS_DOUBLEQUOTE = "Preferences.Profile.Validator.InvalidName.Quote";
	private static final String PROFILE_NAME_IS_EMPTY = "Preferences.Profile.Validator.InvalidName.Empty";
	private static final String WARNING_ALREADY_EXISTS = "Preferences.Profile.Validator.InvalidName.Exists";

	private final String selectedItem;
	private final YoctoProfileElement profileSetting;

	public ProfileNameInputValidator(YoctoProfileElement profileSetting) {
		this(profileSetting, "");
	}

	public ProfileNameInputValidator(YoctoProfileElement profileSetting, String selectedItem) {
		this.selectedItem = selectedItem;
		this.profileSetting = profileSetting;
	}

	@Override
	public String isValid(String newText) {
		if (newText.contains(",")) {
			return YoctoSDKMessages.getString(WARNING_CONTAINS_COMMA);
		}

		if (newText.contains("\"")) {
			return YoctoSDKMessages.getString(WARNING_CONTAINS_DOUBLEQUOTE);
		}

		if (newText.isEmpty()) {
			return YoctoSDKMessages.getString(PROFILE_NAME_IS_EMPTY);
		}

		if (selectedItemEquals(newText)) {
			return null;
		}

		if (profileSetting.contains(newText)) {
			return YoctoSDKMessages.getString(WARNING_ALREADY_EXISTS);
		}

		return null;
	}

	private boolean selectedItemEquals(String newText) {
		return !selectedItem.isEmpty() && newText.equals(selectedItem);
	}
}

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

import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.yocto.sdk.ide.preferences.PreferenceConstants;

public class YoctoProfileElement {
	private TreeSet<String> profiles = new TreeSet<String>(new Comparator<String>() {

		@Override
		public int compare(String o1, String o2) {
			int strcompare = o1.compareTo(o2);

			if (strcompare == 0) {
				return strcompare;
			}

			// Standard profile always less than anything else
			if (o1.equals(PreferenceConstants.STANDARD_PROFILE_NAME)) {
				return -1;
			}

			if (o2.equals(PreferenceConstants.STANDARD_PROFILE_NAME)) {
				return 1;
			}

			return strcompare;
		}
	});

	private String selectedProfile;

	public YoctoProfileElement(String profilesString, String selectedProfile) {
		setProfilesFromString(profilesString);
		this.selectedProfile = selectedProfile;
	}

	public void addProfile(String profile) {
		this.profiles.add(profile);
	}

	public boolean contains(String newText) {
		return profiles.contains(newText);
	}

	public TreeSet<String> getProfiles() {
		return profiles;
	}

	public String getProfilesAsString() {
		String profileString = "";

		for (String profile : profiles) {
			profileString += "\"" + profile + "\",";
		}
		return profileString.substring(0, profileString.length() - 1);
	}

	public String getSelectedProfile() {
		return selectedProfile;
	}

	public void remove(String profile) {
		this.profiles.remove(profile);
	}

	public void rename(String oldProfileName, String newProfileName) {
		this.remove(oldProfileName);
		this.addProfile(newProfileName);

		if (selectedProfile.equals(oldProfileName)) {
			selectedProfile = newProfileName;
		}
	}

	public void setProfiles(TreeSet<String> profiles) {
		this.profiles = profiles;
	}

	public void setProfilesFromString(String profilesString) {
		StringTokenizer tokenizer = new StringTokenizer(profilesString, ",");

		while (tokenizer.hasMoreElements()) {
			String config = (String) tokenizer.nextElement();
			profiles.add(config.replace("\"", ""));
		}
	}

	public void setSelectedProfile(String selectedProfile) {
		this.selectedProfile = selectedProfile;
	}
}

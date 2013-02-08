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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.yocto.sdk.ide.YoctoSDKPlugin;

import static org.yocto.sdk.ide.preferences.PreferenceConstants.*;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		store.setDefault(TOOLCHAIN_ROOT, "");
		store.setDefault(SDK_MODE, true);
		store.setDefault(TARGET_MODE, false);
		store.setDefault(TARGET_ARCH_INDEX, -1);
		store.setDefault(IP_ADDR, "");
		store.setDefault(QEMU_KERNEL, "");
		store.setDefault(QEMU_OPTION, "");
		store.setDefault(SYSROOT, "");
		store.setDefault(TOOLCHAIN_TRIPLET, "");
		store.setDefault(PROFILES, "\"" + STANDARD_PROFILE_NAME + "\"");
		store.setDefault(SELECTED_PROFILE, STANDARD_PROFILE_NAME);
	}

}

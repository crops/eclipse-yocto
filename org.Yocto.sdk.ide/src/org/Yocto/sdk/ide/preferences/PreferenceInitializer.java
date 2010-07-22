package org.Yocto.sdk.ide.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.Yocto.sdk.ide.YoctoSDKPlugin;

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
		store.setDefault(PreferenceConstants.TOOLCHAIN_ROOT, true);
		//store.setDefault(PreferenceConstants.ROOT_DIR_NAME, "rootdir"); //$NON-NLS-1$
		//store.setDefault(SRCBIN_BINNAME, "bin"); //$NON-NLS-1$
		store.setDefault(PreferenceConstants.TARGET_QEMU, true);

		store.setDefault(PreferenceConstants.TARGET_ARCH_LIST, PreferenceConstants.TARGET_ARCHITECTURE_LIST);
		store.setDefault(PreferenceConstants.TARGET_ARCH_INDEX, 0);
	}

}

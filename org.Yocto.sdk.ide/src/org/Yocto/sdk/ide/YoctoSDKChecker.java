package org.Yocto.sdk.ide;

import java.io.File;

public class YoctoSDKChecker {
	public static enum SDKCheckResults {
		SDK_PASS,
		TOOLCHAIN_LOCATION_EMPTY,
		TOOLCHAIN_LOCATION_NON_EXIST,
		TARGET_EMPTY,
		QEMU_KERNEL_EMPTY,
		QEMU_ROOTFS_EMPTY,
		SDK_BIN_NON_EXIST,
		SDK_SYSROOT_NON_EXIST,
		SDK_PKGCONFIG_NON_EXIST,
		SDK_QEMU_KERNEL_NON_EXIST,
		SDK_QEMU_ROOTFS_NON_EXIST,
		SDK_ENV_SETUP_SCRIPT_NON_EXIST
	};

	public static enum SDKCheckRequestFrom {
		Wizard,
		Menu,
		Preferences
	};

	private static final String WIZARD_SDK_LOCATION_EMPTY     = "Wizard.SDK.Location.Empty";
	private static final String WIZARD_SDK_TARGET_EMPTY      = "Wizard.SDK.Target.Empty";
	private static final String WIZARD_SDK_BIN_NONEXIST       = "Wizard.SDK.Bin.Nonexist";
	private static final String WIZARD_SDK_SYSROOT_NONEXIST   = "Wizard.SDK.Sysroot.Nonexist";
	private static final String WIZARD_SDK_PKGCONFIG_NONEXIST = "Wizard.SDK.Pkgconfig.Nonexist";

	private static final String MENU_SDK_LOCATION_EMPTY     = "Menu.SDK.Location.Empty";
	private static final String MENU_SDK_TARGET_EMPTY      = "Menu.SDK.Target.Empty";
	private static final String MENU_SDK_BIN_NONEXIST       = "Menu.SDK.Bin.Nonexist";
	private static final String MENU_SDK_SYSROOT_NONEXIST   = "Menu.SDK.Sysroot.Nonexist";
	private static final String MENU_SDK_PKGCONFIG_NONEXIST = "Menu.SDK.Pkgconfig.Nonexist";

	private static final String PREFERENCES_SDK_BIN_NONEXIST       = "Preferences.SDK.Bin.Nonexist";
	private static final String PREFERENCES_SDK_SYSROOT_NONEXIST   = "Preferences.SDK.Sysroot.Nonexist";
	private static final String PREFERENCES_SDK_PKGCONFIG_NONEXIST = "Preferences.SDK.Pkgconfig.Nonexist";
	private static final String PREFERENCES_SDK_KERNEL_NONEXIST   = "Preferences.SDK.Kernel.Nonexist";
	private static final String PREFERENCES_SDK_ROOTFS_NONEXIST    = "Preferences.SDK.Rootfs.Nonexist";
	private static final String PREFERENCES_SDK_ENVSCRIPT_NONEXIST = "Preferences.SDK.EnvScript.Nonexist";

	public static SDKCheckResults checkYoctoSDK(String sdkroot, String toolchain_location, String target, String target_qemu,
			String qemu_kernel, String qemu_rootfs, String env_script, String ip_addr) {
		if (toolchain_location.isEmpty()) {
			return SDKCheckResults.TOOLCHAIN_LOCATION_EMPTY;			
		} else {
			File toolchain = new File(toolchain_location);
			if (!toolchain.exists())
				return SDKCheckResults.TOOLCHAIN_LOCATION_NON_EXIST;
		}
		if (target.isEmpty() || target==null) {
			return SDKCheckResults.TARGET_EMPTY;
		}
		/*
		if (target_qemu.equals("true")) {
			if (qemu_kernel.isEmpty())
				return SDKCheckResults.QEMU_KERNEL_EMPTY;
			else {
				File kernel_file = new File(qemu_kernel);
				if (!kernel_file.exists())
					return SDKCheckResults.SDK_QEMU_KERNEL_NON_EXIST;
			}
			if (qemu_rootfs.isEmpty())
				return SDKCheckResults.QEMU_ROOTFS_EMPTY;
			else {
				File rootfs_dir = new File(qemu_rootfs);
				if (!rootfs_dir.exists())
					return SDKCheckResults.SDK_QEMU_ROOTFS_NON_EXIST;
			}
		}
		*/
		if (!qemu_kernel.isEmpty()) {
			File kernel_file = new File(qemu_kernel);
			if (!kernel_file.exists())
				return SDKCheckResults.SDK_QEMU_KERNEL_NON_EXIST;
		}
		if (!qemu_rootfs.isEmpty()) {
			File rootfs_dir = new File(qemu_rootfs);
			if (!rootfs_dir.exists())
				return SDKCheckResults.SDK_QEMU_ROOTFS_NON_EXIST;
		}
		if (!env_script.isEmpty()) {
			File script_file = new File(env_script);
			if (!script_file.exists())
				return SDKCheckResults.SDK_ENV_SETUP_SCRIPT_NON_EXIST;
		}
		/*
		else{
			String Yocto_sdk_path = toolchain_location + File.separator + "bin";
	        File sdk_bin_dir = new File(Yocto_sdk_path);
	        if (! sdk_bin_dir.exists())
	        	return SDKCheckResults.SDK_BIN_NON_EXIST;
		}
		*/
		return SDKCheckResults.SDK_PASS;
	}

	private static String getWizardErrorMessage(SDKCheckResults result) {
		switch (result) {
		case TOOLCHAIN_LOCATION_EMPTY:
			return  YoctoSDKMessages.getString(WIZARD_SDK_LOCATION_EMPTY);
		case TARGET_EMPTY:
			return  YoctoSDKMessages.getString(WIZARD_SDK_TARGET_EMPTY);
		case SDK_BIN_NON_EXIST:
			return  YoctoSDKMessages.getString(WIZARD_SDK_BIN_NONEXIST);
		case SDK_SYSROOT_NON_EXIST:
			return  YoctoSDKMessages.getString(WIZARD_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  YoctoSDKMessages.getString(WIZARD_SDK_PKGCONFIG_NONEXIST);
		default:
			return null;
		}
	}

	private static String getMenuErrorMessage(SDKCheckResults result) {
		switch (result) {
		case TOOLCHAIN_LOCATION_EMPTY:
			return  YoctoSDKMessages.getString(MENU_SDK_LOCATION_EMPTY);
		case TARGET_EMPTY:
			return  YoctoSDKMessages.getString(MENU_SDK_TARGET_EMPTY);
		case SDK_BIN_NON_EXIST:
			return  YoctoSDKMessages.getString(MENU_SDK_BIN_NONEXIST);
		case SDK_SYSROOT_NON_EXIST:
			return  YoctoSDKMessages.getString(MENU_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  YoctoSDKMessages.getString(MENU_SDK_PKGCONFIG_NONEXIST);
		default:
			return null;
		}
	}

	private static String getPreferencesErrorMessage(SDKCheckResults result) {
		switch (result) {
		case SDK_BIN_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_BIN_NONEXIST); 
		case SDK_SYSROOT_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_PKGCONFIG_NONEXIST);		
		case SDK_QEMU_KERNEL_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_SDK_KERNEL_NONEXIST);
		case SDK_QEMU_ROOTFS_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_SDK_ROOTFS_NONEXIST);
		case SDK_ENV_SETUP_SCRIPT_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_SDK_ENVSCRIPT_NONEXIST);
		default:
			return null;
		}
	}

	public static String getErrorMessage(SDKCheckResults result, SDKCheckRequestFrom from) {
		switch (from) {
			case Wizard:
				return getWizardErrorMessage(result);
			case Menu:
				return getMenuErrorMessage(result);
			case Preferences:
				return getPreferencesErrorMessage(result);
			default:
				return null;
		}
	}	
}

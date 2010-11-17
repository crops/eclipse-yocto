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
package org.yocto.sdk.ide;

import java.io.File;

public class YoctoSDKChecker {
	public static enum SDKCheckResults {
		SDK_PASS,
		TOOLCHAIN_LOCATION_EMPTY,
		TOOLCHAIN_LOCATION_NON_EXIST,
		TARGET_EMPTY,
		QEMU_KERNEL_EMPTY,
		//QEMU_ROOTFS_EMPTY,
		SDK_BIN_NON_EXIST,
		SDK_SYSROOT_NON_EXIST,
		SDK_PKGCONFIG_NON_EXIST,
		QEMU_KERNEL_NON_EXIST,
		SYSROOT_NON_EXIST,
		SYSROOT_EMPTY,
		ENV_SETUP_SCRIPT_NON_EXIST
	};

	public static enum SDKCheckRequestFrom {
		Wizard,
		Menu,
		Preferences
	};

	private static final String WIZARD_SDK_LOCATION_EMPTY     		= "Wizard.SDK.Location.Empty";
	private static final String WIZARD_TOOLCHAIN_LOCATION_NONEXIST	= "Wizard.Toolcahin.Location.Nonexist";
	private static final String WIZARD_SDK_TARGET_EMPTY      		= "Wizard.SDK.Target.Empty";
	private static final String WIZARD_SDK_BIN_NONEXIST				= "Wizard.SDK.Bin.Nonexist";
	private static final String WIZARD_SDK_SYSROOT_NONEXIST			= "Wizard.SDK.Sysroot.Nonexist";
	private static final String WIZARD_SDK_PKGCONFIG_NONEXIST		= "Wizard.SDK.Pkgconfig.Nonexist";
	private static final String WIZARD_QEMU_KERNEL_EMPTY			= "Wizard.Qemu.Kernel.Empty";
	private static final String WIZARD_SYSROOT_EMPTY				= "Wizard.Sysroot.Empty";
	private static final String WIZARD_QEMU_KERNEL_NONEXIST			= "Wizard.Qemu.Kernel.Nonexist";
	private static final String WIZARD_SYSROOT_NONEXIST				= "Wizard.Sysroot.Nonexist";

	private static final String MENU_SDK_LOCATION_EMPTY     		= "Menu.SDK.Location.Empty";
	private static final String MENU_TOOLCHAIN_LOCATION_NONEXIST	= "Menu.Toolchain.Location.Nonexist";
	private static final String MENU_SDK_TARGET_EMPTY      			= "Menu.SDK.Target.Empty";
	private static final String MENU_SDK_BIN_NONEXIST       		= "Menu.SDK.Bin.Nonexist";
	private static final String MENU_SDK_SYSROOT_NONEXIST   		= "Menu.SDK.Sysroot.Nonexist";
	private static final String MENU_SDK_PKGCONFIG_NONEXIST 		= "Menu.SDK.Pkgconfig.Nonexist";
	private static final String MENU_QEMU_KERNEL_EMPTY 				= "Menu.Qemu.Kernel.Empty";
	private static final String MENU_SYSROOT_EMPTY 					= "Menu.Sysroot.Empty";
	private static final String MENU_QEMU_KERNEL_NONEXIST 			= "Menu.Qemu.Kernel.Nonexist";
	private static final String MENU_SYSROOT_NONEXIST 				= "Menu.Sysroot.Nonexist";

	private static final String PREFERENCES_SDK_BIN_NONEXIST       = "Preferences.SDK.Bin.Nonexist";
	private static final String PREFERENCES_SDK_SYSROOT_NONEXIST   = "Preferences.SDK.Sysroot.Nonexist";
	private static final String PREFERENCES_SDK_PKGCONFIG_NONEXIST = "Preferences.SDK.Pkgconfig.Nonexist";
	
	private static final String ENV_SCRIPT_NONEXIST = "Env.Script.Nonexist";
	
	private static final String PREFERENCES_TOOLCHAIN_LOCATION_NONEXIST = "Preferences.Toolchain.Location.Nonexist";
	private static final String PREFERENCES_QEMU_KERNEL_EMPTY 			= "Preferences.Qemu.Kernel.Empty";
	private static final String PREFERENCES_SYSROOT_EMPTY 				= "Preferences.Sysroot.Empty";
	private static final String PREFERENCES_QEMU_KERNEL_NONEXIST   		= "Preferences.Qemu.Kernel.Nonexist";
	private static final String PREFERENCES_SYSROOT_NONEXIST   			= "Preferences.Sysroot.Nonexist";

	public static SDKCheckResults checkYoctoSDK(String sdkroot, String toolchain_location, String target, String target_qemu,
			String qemu_kernel, String sysroot, String ip_addr) {
	
		if (toolchain_location.isEmpty()) {
			return SDKCheckResults.TOOLCHAIN_LOCATION_EMPTY;			
		} else {
			File toolchain = new File(toolchain_location);
			if (!toolchain.exists())
				return SDKCheckResults.TOOLCHAIN_LOCATION_NON_EXIST;
		}
		
		if (sysroot.isEmpty()) 
			return SDKCheckResults.SYSROOT_EMPTY;
		else {
			File sysroot_dir = new File(sysroot);
			if (!sysroot_dir.exists())
				return SDKCheckResults.SYSROOT_NON_EXIST;
		}
	
		if (target.isEmpty() || target==null) {
			return SDKCheckResults.TARGET_EMPTY;
		}
		
		if (target_qemu.equals("true")) {
			if (qemu_kernel.isEmpty())
				return SDKCheckResults.QEMU_KERNEL_EMPTY;
			else {
				File kernel_file = new File(qemu_kernel);
				if (!kernel_file.exists())
					return SDKCheckResults.QEMU_KERNEL_NON_EXIST;
			}
		
		}
		
		return SDKCheckResults.SDK_PASS;
	}

	private static String getWizardErrorMessage(SDKCheckResults result) {
		switch (result) {
		case TOOLCHAIN_LOCATION_EMPTY:
			return  YoctoSDKMessages.getString(WIZARD_SDK_LOCATION_EMPTY);
		case TOOLCHAIN_LOCATION_NON_EXIST:
			return YoctoSDKMessages.getString(WIZARD_TOOLCHAIN_LOCATION_NONEXIST);
		case TARGET_EMPTY:
			return  YoctoSDKMessages.getString(WIZARD_SDK_TARGET_EMPTY);
		case QEMU_KERNEL_EMPTY:
			return YoctoSDKMessages.getString(WIZARD_QEMU_KERNEL_EMPTY);
		case SYSROOT_EMPTY:
			return YoctoSDKMessages.getString(WIZARD_SYSROOT_EMPTY);
		case QEMU_KERNEL_NON_EXIST:
			return YoctoSDKMessages.getString(WIZARD_QEMU_KERNEL_NONEXIST);
		case SYSROOT_NON_EXIST:
			return YoctoSDKMessages.getString(WIZARD_SYSROOT_NONEXIST);
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
		case TOOLCHAIN_LOCATION_NON_EXIST:
			return YoctoSDKMessages.getString(MENU_TOOLCHAIN_LOCATION_NONEXIST);
		case TARGET_EMPTY:
			return  YoctoSDKMessages.getString(MENU_SDK_TARGET_EMPTY);
		case QEMU_KERNEL_EMPTY:
			return YoctoSDKMessages.getString(MENU_QEMU_KERNEL_EMPTY);
		//case QEMU_ROOTFS_EMPTY:
		//	return YoctoSDKMessages.getString(MENU_QEMU_ROOTFS_EMPTY);
		case QEMU_KERNEL_NON_EXIST:
			return YoctoSDKMessages.getString(MENU_QEMU_KERNEL_NONEXIST);
		case SYSROOT_NON_EXIST:
			return YoctoSDKMessages.getString(MENU_SYSROOT_NONEXIST);
		case SYSROOT_EMPTY:
			return YoctoSDKMessages.getString(MENU_SYSROOT_EMPTY);
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
		case TOOLCHAIN_LOCATION_EMPTY:
			return  YoctoSDKMessages.getString(MENU_SDK_LOCATION_EMPTY);
		case TOOLCHAIN_LOCATION_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_TOOLCHAIN_LOCATION_NONEXIST);
		case TARGET_EMPTY:
			return  YoctoSDKMessages.getString(MENU_SDK_TARGET_EMPTY);
		case SDK_BIN_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_BIN_NONEXIST); 
		case SDK_SYSROOT_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  YoctoSDKMessages.getString(PREFERENCES_SDK_PKGCONFIG_NONEXIST);	
		case QEMU_KERNEL_EMPTY:
			return YoctoSDKMessages.getString(PREFERENCES_QEMU_KERNEL_EMPTY);
		case SYSROOT_EMPTY:
			return YoctoSDKMessages.getString(PREFERENCES_SYSROOT_EMPTY);
		case QEMU_KERNEL_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_QEMU_KERNEL_NONEXIST);
		case SYSROOT_NON_EXIST:
			return YoctoSDKMessages.getString(PREFERENCES_SYSROOT_NONEXIST);
		case ENV_SETUP_SCRIPT_NON_EXIST:
			return YoctoSDKMessages.getString(ENV_SCRIPT_NONEXIST);
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

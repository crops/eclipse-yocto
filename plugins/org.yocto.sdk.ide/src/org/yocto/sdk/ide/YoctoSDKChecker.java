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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

public class YoctoSDKChecker {

	public static enum SDKCheckResults {
		SDK_PASS,
		POKY_DEVICE_EMPTY,
		TOOLCHAIN_LOCATION_EMPTY,
		TOOLCHAIN_LOCATION_NONEXIST,
		SDK_TARGET_EMPTY,
		QEMU_KERNEL_EMPTY,
		SYSROOT_EMPTY,
		QEMU_KERNEL_NONEXIST,
		SYSROOT_NONEXIST,
		WRONG_ADT_VERSION,
		ENV_SETUP_SCRIPT_NONEXIST,
		TOOLCHAIN_NO_SYSROOT,
		TOOLCHAIN_HOST_MISMATCH
	};

	public static enum SDKCheckRequestFrom {
		Wizard,
		Menu,
		Preferences,
		Other
	};

	private static final String POKY_DEVICE_EMPTY = "Poky.SDK.Device.Empty";
	private static final String TOOLCHAIN_LOCATION_EMPTY     = "Poky.SDK.Location.Empty";
	private static final String SDK_TARGET_EMPTY      = "Poky.SDK.Target.Empty";
	private static final String TOOLCHAIN_LOCATION_NONEXIST = "Poky.SDK.Location.Nonexist";
	private static final String QEMU_KERNEL_EMPTY 	  = "Poky.Qemu.Kernel.Empty";
	private static final String SYSROOT_EMPTY = "Poky.Sysroot.Empty";
	private static final String QEMU_KERNEL_NONEXIST = "Poky.Qemu.Kernel.Nonexist";
	private static final String SYSROOT_NONEXIST = "Poky.Sysroot.Nonexist";
	private static final String WRONG_ADT_VERSION = "Poky.ADT.Sysroot.Wrongversion";
	private static final String ENV_SETUP_SCRIPT_NONEXIST = "Poky.Env.Script.Nonexist";
	private static final String TOOLCHAIN_NO_SYSROOT = "Poky.Toolchain.No.Sysroot";
	private static final String TOOLCHAIN_HOST_MISMATCH = "Poky.Toolchain.Host.Mismatch";
	private static final String[] saInvalidVer = {"1.0", "0.9", "0.9+"};
	
	private static final String SYSROOTS_DIR = "sysroots";

	public static SDKCheckResults checkYoctoSDK(YoctoUIElement elem) {
		if (elem.getStrToolChainRoot().isEmpty())
			return SDKCheckResults.TOOLCHAIN_LOCATION_EMPTY;
		else {
			File fToolChain = new File(elem.getStrToolChainRoot());
			if (!fToolChain.exists())
				return SDKCheckResults.TOOLCHAIN_LOCATION_NONEXIST;
		}

		if (elem.getStrSysrootLoc().isEmpty()) {
			return SDKCheckResults.SYSROOT_EMPTY;
		} else {
			File fSysroot = new File(elem.getStrSysrootLoc());
			if (!fSysroot.exists())
				return SDKCheckResults.SYSROOT_NONEXIST;
		}

		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
			//Check for SDK compatible with the host arch
			String platform = YoctoSDKUtils.getPlatformArch();
			String sysroot_dir_str = elem.getStrToolChainRoot() + "/" + SYSROOTS_DIR;
			File sysroot_dir = new File(sysroot_dir_str);
			if (!sysroot_dir.exists())
				return SDKCheckResults.TOOLCHAIN_NO_SYSROOT;

			String toolchain_host_arch = null;

			try {
				toolchain_host_arch = findHostArch(sysroot_dir);
			} catch(NullPointerException e) {
				return SDKCheckResults.TOOLCHAIN_NO_SYSROOT;
			}

			if (!toolchain_host_arch.equalsIgnoreCase(platform)) {
				if (!platform.matches("i\\d86") || !toolchain_host_arch.matches("i\\d86"))
					return SDKCheckResults.TOOLCHAIN_HOST_MISMATCH;
			}
		}

		if (elem.getIntTargetIndex() < 0 || elem.getStrTarget().isEmpty()) {
			//if this is poky tree mode, prompt user whether bitbake meta-ide-support is executed?
			if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_TREE_MODE)
				return SDKCheckResults.ENV_SETUP_SCRIPT_NONEXIST;
			else
				return SDKCheckResults.SDK_TARGET_EMPTY;
		} else {
			String sFileName;

			if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
				sFileName = elem.getStrToolChainRoot()+"/" + YoctoSDKProjectNature.DEFAULT_ENV_FILE_PREFIX+elem.getStrTarget();
			} else {
				//POKY TREE Mode
				sFileName = elem.getStrToolChainRoot() + YoctoSDKProjectNature.DEFAULT_TMP_PREFIX + YoctoSDKProjectNature.DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
			}

			try {
				File file = new File(sFileName);
				boolean bVersion = false;

				if (file.exists()) {
					BufferedReader input = new BufferedReader(new FileReader(file));

					try {
						String line = null;

						while ((line = input.readLine()) != null) {
							if (line.startsWith("export "+ YoctoSDKProjectNature.SDK_VERSION)) {
								int beginIndex = 2;
								String sVersion = "";
								for (;;) {
									char cValue = line.charAt(line.indexOf('=') + beginIndex++);

									if ((cValue != '.') && (!Character.isDigit(cValue)) && (cValue != '+'))
										break;
									else
										sVersion += String.valueOf(cValue);
								}

								for (int i = 0; i < saInvalidVer.length; i++) {
									if (!sVersion.equals(saInvalidVer[i])) {
										bVersion = true;
										break;
									}
								}

								break;
							}
						}
					} finally {
						input.close();
					}

					if (!bVersion)
						return SDKCheckResults.WRONG_ADT_VERSION;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
			if (elem.getStrQemuKernelLoc().isEmpty()) {
				return SDKCheckResults.QEMU_KERNEL_EMPTY;
			} else {
				File fQemuKernel = new File(elem.getStrQemuKernelLoc());
				if (!fQemuKernel.exists())
					return SDKCheckResults.QEMU_KERNEL_NONEXIST;
			}
		}

		return SDKCheckResults.SDK_PASS;
	}

	public static String getErrorMessage(SDKCheckResults result, SDKCheckRequestFrom from) {
		String strErrorMsg;

		switch (from) {
		case Wizard:
			strErrorMsg = "Yocto Wizard Configuration Error:";
			break;
		case Menu:
			strErrorMsg = "Yocto Menu Configuration Error!";
			break;
		case Preferences:
			strErrorMsg = "Yocto Preferences Configuration Error!";
			break;
		default:
			strErrorMsg = "Yocto Configuration Error!";
			break;
		}

		switch (result) {
		case POKY_DEVICE_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(POKY_DEVICE_EMPTY);
			break;
		case TOOLCHAIN_LOCATION_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(TOOLCHAIN_LOCATION_EMPTY);
			break;
		case SDK_TARGET_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(SDK_TARGET_EMPTY);
			break;
		case TOOLCHAIN_LOCATION_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(TOOLCHAIN_LOCATION_NONEXIST);
			break;
		case QEMU_KERNEL_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(QEMU_KERNEL_EMPTY);
			break;
		case SYSROOT_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(SYSROOT_EMPTY);
			break;
		case QEMU_KERNEL_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(QEMU_KERNEL_NONEXIST);
			break;
		case SYSROOT_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(SYSROOT_NONEXIST);
			break;
		case WRONG_ADT_VERSION:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(WRONG_ADT_VERSION);
			break;
		case ENV_SETUP_SCRIPT_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(ENV_SETUP_SCRIPT_NONEXIST);
			break;
		case TOOLCHAIN_NO_SYSROOT:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(TOOLCHAIN_NO_SYSROOT);
			break;
		case TOOLCHAIN_HOST_MISMATCH:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(TOOLCHAIN_HOST_MISMATCH);
			break;
		default:
			break;
		}

		return strErrorMsg;
	}

	private static String findHostArch(File sysroot_dir) {
		FilenameFilter nativeFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith("sdk-linux")) {
					return true;
				} else {
					return false;
				}
			}
		};

		File[] files = sysroot_dir.listFiles(nativeFilter);
		String arch = null;

		for (File file : files) {
			if (file.isDirectory()) {
				String path = file.getName();
				String[] subPath = path.split("-");
				arch = subPath[0];
			} else {
				continue;
			}
		}

		return arch;
	}
}

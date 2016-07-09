/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * Copyright (c) 2013 BMW Car IT GmbH.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 * BMW Car IT - include error and advice messages with check results
 *******************************************************************************/
package org.yocto.sdk.ide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jface.preference.IPreferenceStore;
import org.yocto.sdk.ide.natures.YoctoSDKProjectNature;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtilsConstants;

public class YoctoSDKChecker {
	private static final String[] saInvalidVer = {"1.0", "0.9", "0.9+"};
	private static final String SYSROOTS_DIR = "sysroots";
	private static final String SDK_VERSION = "OECORE_SDK_VERSION";

	public static enum SDKCheckResults {
		SDK_PASS("", false),
		TOOLCHAIN_LOCATION_EMPTY(
				"Poky.SDK.Location.Empty", true),
		TOOLCHAIN_LOCATION_NONEXIST(
				"Poky.SDK.Location.Nonexist", true),
		SDK_TARGET_EMPTY(
				"Poky.SDK.Target.Empty", true),
		SDK_NO_TARGET_SELECTED(
				"Poky.SDK.No.Target.Selected", false),
		SYSROOT_EMPTY(
				"Poky.Sysroot.Empty", true),
		SYSROOT_NONEXIST(
				"Poky.Sysroot.Nonexist", true),
		QEMU_KERNEL_EMPTY(
				"Poky.Qemu.Kernel.Empty", true),
		QEMU_KERNEL_NONEXIST(
				"Poky.Qemu.Kernel.Nonexist", true),
		WRONG_SDK_VERSION(
				"Poky.SDK.Sysroot.Wrongversion", false),
		ENV_SETUP_SCRIPT_NONEXIST(
				"Poky.Env.Script.Nonexist", false),
		TOOLCHAIN_NO_SYSROOT(
				"Poky.Toolchain.No.Sysroot", false),
		TOOLCHAIN_HOST_MISMATCH(
				"Poky.Toolchain.Host.Mismatch", false);

		private static final String DEFAULT_ADVICE = "Default.Advice";
		private static final String ADVICE_SUFFIX = ".Advice";

		private final String messageID;
		private final boolean addDefaultAdvice;

		private SDKCheckResults(final String messageID, final boolean addDefaultAdvice) {
			this.messageID = messageID;
			this.addDefaultAdvice = addDefaultAdvice;
		}

		public String getMessage() {
			return YoctoSDKMessages.getString(messageID);
		}

		public String getAdvice() {
			String advice = YoctoSDKMessages.getString(messageID + ADVICE_SUFFIX);

			if (addDefaultAdvice) {
				advice += YoctoSDKMessages.getString(DEFAULT_ADVICE);
			}

			return advice;
		}
	};

	public static enum SDKCheckRequestFrom {
		Wizard("Poky.SDK.Error.Origin.Wizard"),
		Menu("Poky.SDK.Error.Origin.Menu"),
		Preferences("Poky.SDK.Error.Origin.Preferences"),
		Other("Poky.SDK.Error.Origin.Other");

		private final String errorMessageID;

		private SDKCheckRequestFrom(final String errorMessageID) {
			this.errorMessageID = errorMessageID;
		}

		public String getErrorMessage() {
			return YoctoSDKMessages.getString(errorMessageID);
		}
	};

	public static void checkIfGloballySelectedYoctoProfileIsValid() throws YoctoGeneralException {
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		IPreferenceStore selectedProfileStore = YoctoSDKPlugin.getProfilePreferenceStore(profileElement.getSelectedProfile());
		YoctoUIElement elem = YoctoSDKUtils.getElemFromStore(selectedProfileStore);

		SDKCheckResults result = checkYoctoSDK(elem);
		if (result != SDKCheckResults.SDK_PASS){
			String strErrorMsg =  getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			throw new YoctoGeneralException(strErrorMsg);
		}
	}

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
			String platform = getPlatformArch();
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

		if (elem.getStrTarget().isEmpty() && elem.getStrTargetsArray().length > 0) {
			return SDKCheckResults.SDK_NO_TARGET_SELECTED;
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
				sFileName = elem.getStrToolChainRoot()+"/" + YoctoSDKUtilsConstants.DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
			} else {
				//POKY TREE Mode
				sFileName = elem.getStrToolChainRoot() + YoctoSDKUtilsConstants.DEFAULT_TMP_PREFIX +
						YoctoSDKUtilsConstants.DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
			}

			try {
				File file = new File(sFileName);
				boolean bVersion = false;

				if (file.exists()) {
					BufferedReader input = new BufferedReader(new FileReader(file));

					try {
						String line = null;

						while ((line = input.readLine()) != null) {
							if (line.startsWith("export "+ SDK_VERSION)) {
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
						return SDKCheckResults.WRONG_SDK_VERSION;
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
		strErrorMsg = from.getErrorMessage();
		strErrorMsg += "\n" + result.getMessage();
		strErrorMsg += "\n" + result.getAdvice();

		return strErrorMsg;
	}

	private static String getPlatformArch() {
		String value = null;
		try
		{
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec("uname -m");
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while ( (line = br.readLine()) != null) {
				value = line;
			}
			proc.waitFor();

		} catch (Throwable t) {
			t.printStackTrace();
		}
		return value;
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

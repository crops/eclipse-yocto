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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.yocto.sdk.ide.preferences.PreferenceConstants;

public class YoctoSDKUtils {
	public static enum SDKCheckResults {
		SDK_PASS,
		POKY_DEVICE_EMPTY,
		TOOLCHAIN_LOCATION_EMPTY,
		TOOLCHAIN_LOCATION_NONEXIST,
		SDK_TARGET_EMPTY,
		QEMU_KERNEL_EMPTY,
		QEMU_ROOTFS_EMPTY,
		QEMU_KERNEL_NONEXIST,
		QEMU_ROOTFS_NONEXIST,
		SDK_SYSROOT_NON_EXIST,
		ENV_SETUP_SCRIPT_NONEXIST
	};

	public static enum SDKCheckRequestFrom {
		Wizard,
		Menu,
		Preferences
	};

	private static final String POKY_DEVICE_EMPTY = "Poky.SDK.Device.Empty";
	private static final String TOOLCHAIN_LOCATION_EMPTY     = "Poky.SDK.Location.Empty";
	private static final String SDK_TARGET_EMPTY      = "Poky.SDK.Target.Empty";
	private static final String TOOLCHAIN_LOCATION_NONEXIST = "Poky.SDK.Location.Nonexist";
	private static final String QEMU_KERNEL_EMPTY 	  = "Poky.Qemu.Kernel.Empty";
	private static final String QEMU_ROOTFS_EMPTY = "Poky.Qemu.Rootfs.Empty";
	private static final String QEMU_KERNEL_NONEXIST = "Poky.Qemu.Kernel.Nonexist";
	private static final String QEMU_ROOTFS_NONEXIST = "Poky.Qemu.RootFS.Nonexist";
	private static final String SDK_SYSROOT_NON_EXIST = "Poky.SDK.Sysroot.Nonexist";
	private static final String ENV_SETUP_SCRIPT_NONEXIST = "Poky.Env.Script.Nonexist";


	public static SDKCheckResults checkYoctoSDK(YoctoUIElement elem) {

		if (elem.getStrToolChainRoot().isEmpty())
			return SDKCheckResults.TOOLCHAIN_LOCATION_EMPTY;
		else {
			File fToolChain = new File(elem.getStrToolChainRoot());
			if (!fToolChain.exists())
				return SDKCheckResults.TOOLCHAIN_LOCATION_NONEXIST;
		}

		if (elem.getIntTargetIndex() < 0 || elem.getStrTarget().isEmpty())
			return SDKCheckResults.SDK_TARGET_EMPTY;

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE)
		{
			if (elem.getStrQemuKernelLoc().isEmpty())
				return SDKCheckResults.QEMU_KERNEL_EMPTY;
			else {
				File fQemuKernel = new File(elem.getStrQemuKernelLoc());
				if (!fQemuKernel.exists())
					return SDKCheckResults.QEMU_KERNEL_NONEXIST;
			}
			if (elem.getStrQemuRootFSLoc().isEmpty())
				return SDKCheckResults.QEMU_ROOTFS_EMPTY;
			else {
				File fQemuRootFS = new File(elem.getStrQemuRootFSLoc());
				if (!fQemuRootFS.exists())
					return SDKCheckResults.QEMU_ROOTFS_NONEXIST;
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
		case QEMU_ROOTFS_EMPTY:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(QEMU_ROOTFS_EMPTY);
			break;
		case QEMU_KERNEL_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(QEMU_KERNEL_NONEXIST);
			break;
		case QEMU_ROOTFS_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(QEMU_ROOTFS_NONEXIST);
			break;
		case SDK_SYSROOT_NON_EXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(SDK_SYSROOT_NON_EXIST);
			break;
		case ENV_SETUP_SCRIPT_NONEXIST:
			strErrorMsg = strErrorMsg + "\n" + YoctoSDKMessages.getString(ENV_SETUP_SCRIPT_NONEXIST);
			break;
		default:
			break;
		}
		return strErrorMsg;
	}


	public static String getEnvValue(IProject project, String strKey)
	{
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		IEnvironmentVariable var = env.getVariable(strKey, ccdesc);

		if (var == null)
		{
			System.out.printf("ENV key %s is NULL\n", strKey);
			return "";			
		}

		else
			return var.getValue();
	}

	/* Save project wide settings into ENV VARs including POKY preference settings
	 * and Environment Script File export VARs
	 */
	public static void setEnvVars(ICProjectDescription cpdesc,
			YoctoUIElement elem, HashMap<String, String> envMap) {
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		String delimiter = manager.getDefaultDelimiter();

		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE)
			env.addVariable(PreferenceConstants.SDK_MODE, IPreferenceStore.TRUE,
					IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		else
			env.addVariable(PreferenceConstants.SDK_MODE, IPreferenceStore.FALSE,
					IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		env.addVariable(PreferenceConstants.TOOLCHAIN_ROOT, elem.getStrToolChainRoot(),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.TOOLCHAIN_TRIPLET, elem.getStrTarget(),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.TARGET_ARCH_INDEX, String.valueOf(elem.getIntTargetIndex()),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE)
			env.addVariable(PreferenceConstants.TARGET_MODE, IPreferenceStore.TRUE,
					IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		else
			env.addVariable(PreferenceConstants.TARGET_MODE, IPreferenceStore.FALSE,
					IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		env.addVariable(PreferenceConstants.QEMU_KERNEL, elem.getStrQemuKernelLoc(),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.QEMU_ROOTFS, elem.getStrQemuRootFSLoc(),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		if (envMap == null)
		{
			System.out.println("ENV var hasmap is NULL, Please check ENV script File!");
			return;
		}
		Iterator<String> iter = envMap.keySet().iterator();
		while (iter.hasNext())
		{					
			String sKey = (String)iter.next();
			String sValue = (String)envMap.get(sKey);
			env.addVariable(sKey, sValue, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		}
		return;

	}

	/* Load project wide POKY Preference settings into YoctoUIElement */
	public static YoctoUIElement getElemFromProjectEnv(IProject project)
	{
		YoctoUIElement elem = new YoctoUIElement();
		elem.setStrToolChainRoot(getEnvValue(project, PreferenceConstants.TOOLCHAIN_ROOT));
		elem.setStrTarget(getEnvValue(project, PreferenceConstants.TOOLCHAIN_TRIPLET));
		elem.setStrQemuKernelLoc(getEnvValue(project, PreferenceConstants.QEMU_KERNEL));
		elem.setStrQemuRootFSLoc(getEnvValue(project, PreferenceConstants.QEMU_ROOTFS));
		String sTemp = getEnvValue(project, PreferenceConstants.TARGET_ARCH_INDEX);
		if (!sTemp.isEmpty())
			elem.setIntTargetIndex(Integer.valueOf(sTemp).intValue());
		if (getEnvValue(project, PreferenceConstants.SDK_MODE).equalsIgnoreCase(IPreferenceStore.TRUE))
		{
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_SDK_MODE);
		}
		else
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_TREE_MODE);

		if(getEnvValue(project, PreferenceConstants.TARGET_MODE).equalsIgnoreCase(IPreferenceStore.TRUE))
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		else
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);
		return elem;
	}

	/* Load IDE wide POKY Preference settings into Preference Store */
	public static void saveElemToStore(YoctoUIElement elem)
	{
		IPreferenceStore store= YoctoSDKPlugin.getDefault().getPreferenceStore();

		store.setValue(PreferenceConstants.TARGET_ARCH_INDEX, elem.getIntTargetIndex());
		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE)
			store.setValue(PreferenceConstants.SDK_MODE, IPreferenceStore.TRUE);
		else
			store.setValue(PreferenceConstants.SDK_MODE, IPreferenceStore.FALSE);
		store.setValue(PreferenceConstants.QEMU_KERNEL, elem.getStrQemuKernelLoc());
		store.setValue(PreferenceConstants.QEMU_ROOTFS, elem.getStrQemuRootFSLoc());
		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE)
			store.setValue(PreferenceConstants.TARGET_MODE, IPreferenceStore.TRUE);
		else
			store.setValue(PreferenceConstants.TARGET_MODE, IPreferenceStore.FALSE);
		store.setValue(PreferenceConstants.TOOLCHAIN_ROOT, elem.getStrToolChainRoot());
		store.setValue(PreferenceConstants.TOOLCHAIN_TRIPLET, elem.getStrTarget());		
	}

	/* Load IDE wide POKY Preference settings into YoctoUIElement */
	public static YoctoUIElement getElemFromStore()
	{
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		YoctoUIElement elem = new YoctoUIElement();
		if (store.getString(PreferenceConstants.SDK_MODE).equals(IPreferenceStore.TRUE))
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_SDK_MODE);
		else
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_TREE_MODE);

		elem.setStrToolChainRoot(store.getString(PreferenceConstants.TOOLCHAIN_ROOT));
		elem.setStrTarget(store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET));
		elem.setIntTargetIndex(store.getInt(PreferenceConstants.TARGET_ARCH_INDEX));
		elem.setStrQemuKernelLoc(store.getString(PreferenceConstants.QEMU_KERNEL));
		elem.setStrQemuRootFSLoc(store.getString(PreferenceConstants.QEMU_ROOTFS));

		if (store.getString(PreferenceConstants.TARGET_MODE).equals(IPreferenceStore.TRUE))
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		else
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);

		return elem;
	}

	public static String splitString(String strValue, String strDelim)
	{
		int iBeginIndex = strValue.indexOf(strDelim);
		if (iBeginIndex < 0)
			return "";
		int iEndIndex = strValue.indexOf(' ', iBeginIndex + 1);

		if (iEndIndex < 0)
			return strValue.substring(iBeginIndex + strDelim.length()); 
		else 
			return strValue.substring(iBeginIndex + strDelim.length(), iEndIndex); 
	}	

	public static HashMap<String, String> parseEnvScript(String sFileName)
	{
		try
		{
			HashMap<String, String> envMap = new HashMap<String, String>();
			File file = new File(sFileName);

			if (file.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(file));

				try
				{
					String line = null;

					while ((line = input.readLine()) != null)
					{
						if (!line.startsWith("export"))
							continue;
						String sKey = line.substring("export".length() + 1, line.indexOf('='));
						String sValue = line.substring(line.indexOf('=') + 1);
						if (sValue.startsWith("\"") && sValue.endsWith("\""))
							sValue = sValue.substring(sValue.indexOf('"') + 1, sValue.lastIndexOf('"'));
						/* If PATH ending with $PATH, we need to join with current system path */
						if (sKey.equalsIgnoreCase("PATH")) {
							if (sValue.lastIndexOf("$PATH") >= 0)
								sValue = sValue.substring(0, sValue.lastIndexOf("$PATH")) + System.getenv("PATH");
						}
						envMap.put(sKey, sValue);
						System.out.printf("get env key %s value %s\n", sKey, sValue);
					}

				}
				finally {
					input.close();
				}
			}

			return envMap;

		} 
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}

	}

}

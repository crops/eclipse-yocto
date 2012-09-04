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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
	private static final String DEFAULT_SYSROOT_PREFIX = "--sysroot=";
	private static final String LIBTOOL_SYSROOT_PREFIX = "--with-libtool-sysroot=";
	private static final String SYSROOTS_DIR = "sysroots";

	public static SDKCheckResults checkYoctoSDK(YoctoUIElement elem) {		
		
		if (elem.getStrToolChainRoot().isEmpty())
			return SDKCheckResults.TOOLCHAIN_LOCATION_EMPTY;
		else {
			File fToolChain = new File(elem.getStrToolChainRoot());
			if (!fToolChain.exists())
				return SDKCheckResults.TOOLCHAIN_LOCATION_NONEXIST;
		}

		if (elem.getStrSysrootLoc().isEmpty())
			return SDKCheckResults.SYSROOT_EMPTY;
		else {
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
			
			try 
			{
				toolchain_host_arch = findHostArch(sysroot_dir);
			}
			catch(NullPointerException e) 
			{
				return SDKCheckResults.TOOLCHAIN_NO_SYSROOT;
			}
			
			if (!toolchain_host_arch.equalsIgnoreCase(platform)) {
				if (!platform.matches("i\\d86") || !toolchain_host_arch.matches("i\\d86"))
					return SDKCheckResults.TOOLCHAIN_HOST_MISMATCH;
			}
		}
		if (elem.getIntTargetIndex() < 0 || elem.getStrTarget().isEmpty())
		{
			//if this is poky tree mode, prompt user whether bitbake meta-ide-support is executed?
			if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_TREE_MODE)
				return SDKCheckResults.ENV_SETUP_SCRIPT_NONEXIST;
			else
				return SDKCheckResults.SDK_TARGET_EMPTY;
		}
		else
		{
			String sFileName;
			if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
				sFileName = elem.getStrToolChainRoot()+"/" + YoctoSDKProjectNature.DEFAULT_ENV_FILE_PREFIX+elem.getStrTarget();
			}
			else {
				//POKY TREE Mode
				sFileName = elem.getStrToolChainRoot() + YoctoSDKProjectNature.DEFAULT_TMP_PREFIX + YoctoSDKProjectNature.DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
			}
			try
			{

				File file = new File(sFileName);
				boolean bVersion = false;

				if (file.exists()) {
					BufferedReader input = new BufferedReader(new FileReader(file));

					try
					{
						String line = null;

						while ((line = input.readLine()) != null)
						{							
							if (line.startsWith("export "+ YoctoSDKProjectNature.SDK_VERSION))
							{
								int beginIndex = 2;
								String sVersion = "";
								for (;;) 
								{
									char cValue = line.charAt(line.indexOf('=') + beginIndex++);
									if ((cValue != '.') && (!Character.isDigit(cValue)) && (cValue != '+'))
										break;
									else
										sVersion += String.valueOf(cValue);
								}
								for (int i = 0; i < saInvalidVer.length; i++)
								{
									if (!sVersion.equals(saInvalidVer[i]))
									{
										bVersion = true;
										break;
									}
										
								}
								break;
							}
						}

					}
					finally {
						input.close();
					}
					if (!bVersion)
						return SDKCheckResults.WRONG_ADT_VERSION;

				}
			}
			catch (IOException e)
			{
				e.printStackTrace();

			}
		}

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE)
		{
			if (elem.getStrQemuKernelLoc().isEmpty())
				return SDKCheckResults.QEMU_KERNEL_EMPTY;
			else {
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
		env.addVariable(PreferenceConstants.QEMU_OPTION, elem.getStrQemuOption(),
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.SYSROOT, elem.getStrSysrootLoc(),
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
			String targetFilePath;
			File targetFile;
			//replace --sysroot
			if (sKey.matches("CFLAGS") || sKey.matches("CXXFLAGS") || sKey.matches("CXXFLAGS") || sKey.matches("LDFLAGS") || 
					sKey.matches("CPPFLAGS")) {
				
				int SYSROOT_idx = sValue.lastIndexOf(DEFAULT_SYSROOT_PREFIX);
				if (SYSROOT_idx >=0 )
					sValue = sValue.substring(0, SYSROOT_idx) + DEFAULT_SYSROOT_PREFIX + elem.getStrSysrootLoc();
				else
					sValue = " " + DEFAULT_SYSROOT_PREFIX + elem.getStrSysrootLoc();
				targetFilePath = elem.getStrSysrootLoc() + "/" + elem.getStrTarget();
				targetFile = new File(targetFilePath);
				if (targetFile.exists())
					sValue = sValue + "/" + elem.getStrTarget();
			} else if (sKey.matches("CONFIGURE_FLAGS")) {			
				int LIBTOOL_idx = sValue.lastIndexOf(LIBTOOL_SYSROOT_PREFIX);
				if (LIBTOOL_idx >= 0)
					sValue = sValue.substring(0, LIBTOOL_idx) + LIBTOOL_SYSROOT_PREFIX + elem.getStrSysrootLoc();
				else
					sValue = " " + LIBTOOL_SYSROOT_PREFIX + elem.getStrSysrootLoc();
				targetFilePath = elem.getStrSysrootLoc() + "/" + elem.getStrTarget();
				targetFile = new File(targetFilePath);
				if (targetFile.exists())
					sValue = sValue + "/" + elem.getStrTarget();		
			} else if(sKey.matches("PKG_CONFIG_SYSROOT_DIR") || sKey.matches("OECORE_TARGET_SYSROOT")) {
				sValue = elem.getStrSysrootLoc();
				targetFilePath = elem.getStrSysrootLoc() + "/" + elem.getStrTarget();
				targetFile = new File(targetFilePath);
				if (targetFile.exists())
					sValue = sValue + "/" + elem.getStrTarget();
			} else if (sKey.matches("PKG_CONFIG_PATH")) {
				sValue = elem.getStrSysrootLoc();
				targetFilePath = elem.getStrSysrootLoc() + "/" + elem.getStrTarget();
				targetFile = new File(targetFilePath);
				if (targetFile.exists())
					sValue = sValue + "/" + elem.getStrTarget();
				sValue = sValue + "/usr/lib/pkgconfig";
			} 
			//	env.addVariable(sKey, elem.getStrSysrootLoc(), IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			/*
			else if (sKey.matches("PKG_CONFIG_PATH"))
				env.addVariable(sKey, elem.getStrSysrootLoc()+"/"+elem.getStrTarget()+"/usr/lib/pkgconfig", IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
				//env.addVariable(sKey, sValue, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			else if (sKey.matches("PKG_CONFIG_SYSROOT_DIR"))
				env.addVariable(sKey, elem.getStrSysrootLoc()+"/"+elem.getStrTarget(), IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			*/
			env.addVariable(sKey, sValue, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		}
		//add ACLOCAL OPTS for libtool 2.4 support
		env.addVariable("OECORE_ACLOCAL_OPTS",
				"-I " + env.getVariable(YoctoSDKProjectNature.NATIVE_SYSROOT, ccdesc).getValue() + "/usr/share/aclocal", 
				IEnvironmentVariable.ENVVAR_REPLACE,
				delimiter,
				ccdesc);
		return;

	}

	/* Load project wide POKY Preference settings into YoctoUIElement */
	public static YoctoUIElement getElemFromProjectEnv(IProject project)
	{
		YoctoUIElement elem = new YoctoUIElement();
		elem.setStrToolChainRoot(getEnvValue(project, PreferenceConstants.TOOLCHAIN_ROOT));
		elem.setStrTarget(getEnvValue(project, PreferenceConstants.TOOLCHAIN_TRIPLET));
		elem.setStrQemuKernelLoc(getEnvValue(project, PreferenceConstants.QEMU_KERNEL));
		elem.setStrSysrootLoc(getEnvValue(project, PreferenceConstants.SYSROOT));
		elem.setStrQemuOption(getEnvValue(project, PreferenceConstants.QEMU_OPTION));
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
		store.setValue(PreferenceConstants.QEMU_OPTION, elem.getStrQemuOption());
		store.setValue(PreferenceConstants.SYSROOT, elem.getStrSysrootLoc());
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
		elem.setStrQemuOption(store.getString(PreferenceConstants.QEMU_OPTION));
		elem.setStrSysrootLoc(store.getString(PreferenceConstants.SYSROOT));

		if (store.getString(PreferenceConstants.TARGET_MODE).equals(IPreferenceStore.TRUE))
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		else
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);

		return elem;
	}

	public static String qemuTargetTranslate(String strTargetArch) 
	{
		String qemu_target = "";
		if (strTargetArch.indexOf("i586") != -1)
			qemu_target = "qemux86";
		else if (strTargetArch.indexOf("x86_64") != -1)
			qemu_target = "qemux86-64";
		else if (strTargetArch.indexOf("arm") != -1)
			qemu_target = "qemuarm";
		else if (strTargetArch.indexOf("mips") != -1)
			qemu_target = "qemumips";
		else if (strTargetArch.indexOf("ppc") != -1)
			qemu_target = "qemuppc";
		return qemu_target;
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

	public static void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException
	{
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (natureId.equals(natures[i]))
				return;
		}

		String[] newNatures = new String[natures.length + 1];
	    System.arraycopy(natures, 0, newNatures, 0, natures.length);
	    newNatures[natures.length] = natureId;
	    description.setNatureIds(newNatures);
	    project.setDescription(description, monitor);

	}

	public static String getPlatformArch() 
	{
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
            int exitVal = proc.waitFor();
            
        } catch (Throwable t)
          {
            t.printStackTrace();
          }
		return value;
	}
	
	static private String findHostArch(File sysroot_dir) {
		FilenameFilter nativeFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith("sdk-linux")) {
					return true;
				} else {
					return false;
				}
			}
		};

		File copyFile = null;
		File[] files = sysroot_dir.listFiles(nativeFilter);
		String arch = null;
		for (File file : files) {
			if (file.isDirectory()) {
				String path = file.getName();
				String[] subPath = path.split("-");
				arch = subPath[0];
			} else 
				continue;
        }
		return arch;
	}
}
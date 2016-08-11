/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 * BMW Car IT - add methods to use different preference stores
 * Atanas Gegov (BMW Car IT) - add method to get the project environment
 *******************************************************************************/
package org.yocto.sdk.ide.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.natures.YoctoSDKCMakeProjectNature;
import org.yocto.sdk.ide.natures.YoctoSDKEmptyProjectNature;
import org.yocto.sdk.ide.preferences.LoggerConstants;
import org.yocto.sdk.ide.preferences.PreferenceConstants;

public class YoctoSDKUtils {

	private static final String DEFAULT_SYSROOT_PREFIX = "--sysroot=";
	private static final String LIBTOOL_SYSROOT_PREFIX = "--with-libtool-sysroot=";

	private static final String DEFAULT_USR_BIN = "/usr/bin/";
	private static final String NATIVE_SYSROOT = "OECORE_NATIVE_SYSROOT";

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
	private static void setEnvVars(ICProjectDescription cpdesc,
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
				"-I " + env.getVariable(NATIVE_SYSROOT, ccdesc).getValue() + "/usr/share/aclocal",
				IEnvironmentVariable.ENVVAR_REPLACE,
				delimiter,
				ccdesc);
		return;

	}

	private static String getEnvironmentSetupFileFullPath(YoctoUIElement elem) {
		String envSetupFile = "";

		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
			envSetupFile = elem.getStrToolChainRoot() + "/";
		} else {
			//POKY TREE Mode
			envSetupFile = elem.getStrToolChainRoot() + YoctoSDKUtilsConstants.DEFAULT_TMP_PREFIX;
		}
		envSetupFile += YoctoSDKUtilsConstants.DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
		return envSetupFile;
	}

	public static HashMap<String, String> parseEnvScript(String sFileName) {
		try {
			HashMap<String, String> envMap = new HashMap<String, String>();
			File file = new File(sFileName);

			if (file.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(file));

				try {
					String line = null;

					while ((line = input.readLine()) != null) {
						if (!line.startsWith("export")) {
							continue;
						}
						String sKey = line.substring("export".length() + 1, line.indexOf('='));
						String sValue = line.substring(line.indexOf('=') + 1);
						if (sValue.startsWith("\"") && sValue.endsWith("\""))
							sValue = sValue.substring(sValue.indexOf('"') + 1, sValue.lastIndexOf('"'));
						/* If PATH ending with $PATH, we need to join with current system path */
						if (sKey.equalsIgnoreCase("PATH") && (sValue.lastIndexOf("$PATH") >= 0)) {
							if (envMap.containsKey(sKey)) {
								sValue = sValue.substring(0, sValue.lastIndexOf("$PATH")) + envMap.get(sKey);
							} else {
								sValue = sValue.substring(0, sValue.lastIndexOf("$PATH")) + System.getenv("PATH");
							}
						}

						if(sValue.toUpperCase().contains("$SDKTARGETSYSROOT")) {
							String rValue = sValue.replaceAll(Matcher.quoteReplacement("$SDKTARGETSYSROOT"), envMap.get("SDKTARGETSYSROOT"));
							envMap.put(sKey, rValue) ;
						} else {
							envMap.put(sKey, sValue);
						}

						System.out.printf("get env key %s value %s\n", sKey, sValue);
					}
				} finally {
					input.close();
				}
			}

			return envMap;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void setEnvironmentVariables(IProject project, YoctoUIElement elem) {
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);

		String sFileName = getEnvironmentSetupFileFullPath(elem);
		HashMap<String, String> envMap = parseEnvScript(sFileName);

		setEnvVars(cpdesc, elem, envMap);
		try {
			CoreModel.getDefault().setProjectDescription(project,cpdesc);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static void createRemoteDebugAndQemuLaunchers(IProject project, YoctoUIElement elem) throws YoctoGeneralException {
		ILaunchManager lManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType =
				lManager.getLaunchConfigurationType("org.eclipse.ui.externaltools.ProgramLaunchConfigurationType");
		ILaunchConfigurationType debug_configType =
				lManager.getLaunchConfigurationType("org.eclipse.cdt.launch.remoteApplicationLaunchType");

		String sPath = getEnvValue(project, "PATH");
		String sDebugName = getEnvValue(project, "GDB");
		String sysroot_str = elem.getStrSysrootLoc();
		String target_prefix_str = getEnvValue(project,"TARGET_PREFIX");
		String target_str = target_prefix_str.substring(0, target_prefix_str.length() - 1);

		if (configType == null || debug_configType == null) {
			throw new YoctoGeneralException("Failed to get program or remote debug launcher!");
		}
		
		createRemoteDebugLauncher(project, lManager, debug_configType, target_str, sPath, sDebugName, sysroot_str);
		ArrayList<String> listValue = new ArrayList<String>();
		listValue.add(new String("org.eclipse.ui.externaltools.launchGroup"));

		if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
			String sFileName = getEnvironmentSetupFileFullPath(elem);
			createQemuLauncher(project, configType, listValue, sFileName, elem);
		}
	}

	protected static void createRemoteDebugLauncher(IProject project,
			ILaunchManager lManager, ILaunchConfigurationType configType,
			String sTargetTriplet,	String strPath, String sDebugName, String sSysroot) {
		try {

			String sDebugSubDir = DEFAULT_USR_BIN + sTargetTriplet;
			StringTokenizer token = new StringTokenizer(strPath, ":");
			String strDebugger = "";
			while (token.hasMoreTokens())
			{
				String sTemp = token.nextToken();
				if (sTemp.endsWith(sDebugSubDir)) {
					strDebugger = sTemp + "/" + sDebugName;
					break;
				}
			}
			if (strDebugger.isEmpty())
				return;
			//If get default Debugger successfully, go ahead!

			//create the gdbinit file
			String sDebugInitFile = project.getLocation().toString() + "/.gdbinit";
			FileWriter out = new FileWriter(new File(sDebugInitFile));
			out.write("set sysroot " + sSysroot);
			out.flush();
			out.close();

			//set the launch configuration
			String projectName = project.getName();
			String configName = projectName+"_gdb_"+sTargetTriplet;
			int i;
			ILaunchConfiguration[] configs=lManager.getLaunchConfigurations(configType);
			for(i=0; i<configs.length; i++)
			{	//delete the old configuration
				ILaunchConfiguration config=configs[i];
				if(config.getName().equals(configName)) {
					config.delete();
					break;
				}
			}
			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(project, configName);
			Set<String> modes=new HashSet<String>();
			modes.add("debug");
			w_copy.setAttribute(IGDBLaunchConfigurationConstants.ATTR_GDB_INIT, sDebugInitFile); // DSF
			w_copy.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_AUTO_SOLIB, false); // DSF
			w_copy.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, strDebugger); // DSF


			w_copy.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			if (!project.hasNature(YoctoSDKEmptyProjectNature.YoctoSDK_EMPTY_NATURE_ID)) {
				String pathToCompiledBinary = "";
				if (project.hasNature(YoctoSDKCMakeProjectNature.YoctoSDK_CMAKE_NATURE_ID)) {
					pathToCompiledBinary = "Debug/";
				} else {
					pathToCompiledBinary = "src/";
				}
				pathToCompiledBinary += projectName;
				w_copy.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, pathToCompiledBinary);
			}

			w_copy.doSave();
		}
		catch (CoreException e)
		{
			System.out.println(e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println("Failed to generate debug init file!");
			System.out.println(e.getMessage());
		}
	}

	protected static void createQemuLauncher(IProject project,
			ILaunchConfigurationType configType,
			ArrayList<String> listValue, String sScriptFile,
			YoctoUIElement elem) {
		try {

			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(null, "qemu_"+elem.getStrTarget());

			w_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);
			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", "/usr/bin/xterm");

			String argument = "-e \"source " + sScriptFile + ";runqemu " + qemuTargetTranslate(elem.getStrTarget()) + " " +
			elem.getStrQemuKernelLoc() + " " + elem.getStrSysrootLoc() + " " +  elem.getStrQemuOption() + ";bash\"";

			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS", argument);
			w_copy.doSave();
		} catch (CoreException e) {
		}

	}

	private static String qemuTargetTranslate(String strTargetArch) {
		String qemu_target = "";
		if (strTargetArch.indexOf("i586") != -1) {
			qemu_target = "qemux86";
		} else if (strTargetArch.indexOf("x86_64") != -1) {
			qemu_target = "qemux86-64";
		} else if (strTargetArch.indexOf("arm") != -1) {
			qemu_target = "qemuarm";
		} else if (strTargetArch.indexOf("mips") != -1) {
			qemu_target = "qemumips";
		} else if (strTargetArch.indexOf("ppc") != -1) {
			qemu_target = "qemuppc";
		}
		return qemu_target;
	}

	/* Get IDE wide POKY Preference settings from a specific preference store */
	public static YoctoUIElement getElemFromStore(IPreferenceStore store) {
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

	/* Get default POKY Preference settings from the default preference store */
	public static YoctoUIElement getDefaultElemFromDefaultStore()
	{
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		YoctoUIElement elem = new YoctoUIElement();
		if (store.getDefaultString(PreferenceConstants.SDK_MODE).equals(IPreferenceStore.TRUE))
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_SDK_MODE);
		else
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_TREE_MODE);

		elem.setStrToolChainRoot(store.getDefaultString(PreferenceConstants.TOOLCHAIN_ROOT));
		elem.setStrTarget(store.getDefaultString(PreferenceConstants.TOOLCHAIN_TRIPLET));
		elem.setIntTargetIndex(store.getDefaultInt(PreferenceConstants.TARGET_ARCH_INDEX));
		elem.setStrQemuKernelLoc(store.getDefaultString(PreferenceConstants.QEMU_KERNEL));
		elem.setStrQemuOption(store.getDefaultString(PreferenceConstants.QEMU_OPTION));
		elem.setStrSysrootLoc(store.getDefaultString(PreferenceConstants.SYSROOT));

		if (store.getDefaultString(PreferenceConstants.TARGET_MODE).equals(IPreferenceStore.TRUE))
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		else
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);

		return elem;
	}

	/* Save profiles and selected profile to the default preference store */
	public static void saveProfilesToDefaultStore(YoctoProfileElement profileElement) {
		saveProfilesToStore(profileElement, YoctoSDKPlugin.getDefault().getPreferenceStore());
	}

	/* Save profiles and selected profile to a specific preference store */
	private static void saveProfilesToStore(YoctoProfileElement profileElement, IPreferenceStore store) {
		store.setValue(PreferenceConstants.PROFILES, profileElement.getProfilesAsString());
		store.setValue(PreferenceConstants.SELECTED_PROFILE, profileElement.getSelectedProfile());
	}

	/* Get profiles and selected profile from the default preference store */
	public static YoctoProfileElement getProfilesFromDefaultStore()
	{
		return getProfilesFromStore(YoctoSDKPlugin.getDefault().getPreferenceStore());
	}

	/* Get profiles and selected profile from a specific preference store */
	private static YoctoProfileElement getProfilesFromStore(IPreferenceStore store) {
		String profiles = store.getString(PreferenceConstants.PROFILES);
		String selectedProfile = store.getString(PreferenceConstants.SELECTED_PROFILE);

		return new YoctoProfileElement(profiles, selectedProfile);
	}

	public static Logger registerLogger(String logger_name, String log_file) {
		Logger logger = null;
		if (System.getenv(LoggerConstants.ENV_LOG_FILE) != null) {
			try {
				Handler handler = new FileHandler(log_file, LoggerConstants.LOG_SIZE, LoggerConstants.LOG_COUNT, true);
				logger = Logger.getLogger(logger_name) ;
				handler.setFormatter(new SimpleFormatter());
				logger.addHandler(handler);
				logger.log(Level.INFO, "Registered logger : " + logger_name + " Log File: [" + log_file + "]");
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Logging is not enabled. Set ENABLE_SDK_LOGS=1 environment variable to enable the logger") ;
		}
		return logger ;
	}

	public static void unRegisterLogger(Logger logger, String log_file) {
		if (System.getenv(LoggerConstants.ENV_LOG_FILE) != null) {
			logger.log(Level.INFO, "Unregistered logger : " + logger.getName() + " Log file: [" + log_file + "]");
			Handler[] handlers = logger.getHandlers() ;
			if(handlers != null) {
				for (Handler handler : handlers) {
					handler.close();
					logger.removeHandler(handler);
				}
			}
		}
	}
}

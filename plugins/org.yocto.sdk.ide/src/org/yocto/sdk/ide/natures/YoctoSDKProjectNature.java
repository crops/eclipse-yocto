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
package org.yocto.sdk.ide.natures;

import org.eclipse.cdt.internal.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.cdt.internal.autotools.core.configure.IAConfiguration;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoSDKChecker;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;


@SuppressWarnings("restriction")
public class YoctoSDKProjectNature implements IProjectNature {
	public static final  String YoctoSDK_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKNature";

	private static final String DEFAULT_HOST_STR = "host";
	private static final String DEFAULT_TARGET_STR = "target";
	private static final String DEFAULT_BUILD_STR = "build";
	private static final String DEFAULT_AUTOGEN_OPT_STR = "autogenOpts";

	private static final String DEFAULT_CONFIGURE_STR = "configure";
	private static final String DEFAULT_AUTOGEN_STR = "autogen";
	private static final String DEFAULT_LIBTOOL_SYSROOT_PREFIX = " --with-libtool-sysroot=";

	private IProject proj;

	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return proj;
	}

	public void setProject(IProject project) {
		this.proj = project;
	}

	public static void addYoctoSDKNature(IProject project, IProgressMonitor monitor) throws CoreException {
		YoctoSDKUtils.addNature(project, YoctoSDK_NATURE_ID, monitor);
	}

	public static void configureAutotoolsOptions(IProject project) {
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		YoctoUIElement elem = YoctoSDKUtils.getElemFromProjectEnv(project);
		String sysroot_str = elem.getStrSysrootLoc();
		String id = icfg.getId();
		String CFLAGS_str = YoctoSDKUtils.getEnvValue(project, "CFLAGS");
		String CXXFLAGS_str = YoctoSDKUtils.getEnvValue(project, "CXXFLAGS");
		String CPPFLAGS_str = YoctoSDKUtils.getEnvValue(project, "CPPFLAGS");
		String LDFLAGS_str = YoctoSDKUtils.getEnvValue(project, "LDFLAGS");
		
		String command_prefix = "CFLAGS=\" -g -O0 " + CFLAGS_str + "\" CXXFLAGS=\" -g -O0 "
		+ CXXFLAGS_str + "\" LDFLAGS=\"" + LDFLAGS_str + "\" CPPFLAGS=\"" + CPPFLAGS_str + "\"";
		String autogen_setting = command_prefix+" autogen.sh" + DEFAULT_LIBTOOL_SYSROOT_PREFIX + sysroot_str;
		String configure_setting = command_prefix + " configure" + DEFAULT_LIBTOOL_SYSROOT_PREFIX + sysroot_str;
		IAConfiguration cfg = AutotoolsConfigurationManager.getInstance().getConfiguration(project, id);
		String strConfigure = YoctoSDKUtils.getEnvValue(project, "CONFIGURE_FLAGS");

		cfg.setOption(DEFAULT_CONFIGURE_STR, configure_setting);
		cfg.setOption(DEFAULT_BUILD_STR, YoctoSDKUtils.splitString(strConfigure, "--build="));
		cfg.setOption(DEFAULT_HOST_STR, YoctoSDKUtils.splitString(strConfigure, "--host="));
		cfg.setOption(DEFAULT_TARGET_STR, YoctoSDKUtils.splitString(strConfigure, "--target="));
		cfg.setOption(DEFAULT_AUTOGEN_STR, autogen_setting);
		cfg.setOption(DEFAULT_AUTOGEN_OPT_STR, strConfigure);

		AutotoolsConfigurationManager.getInstance().addConfiguration(project, cfg);
		AutotoolsConfigurationManager.getInstance().saveConfigs(project);
	}

	public static void configureAutotools(IProject project) throws YoctoGeneralException {
		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		YoctoSDKUtils.saveProfilesToProjectPreferences(profileElement, project);
		IPreferenceStore selecteProfileStore = YoctoSDKPlugin.getProfilePreferenceStore(profileElement.getSelectedProfile());
		YoctoUIElement elem = YoctoSDKUtils.getElemFromStore(selecteProfileStore);
		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(elem);
		if (result != SDKCheckResults.SDK_PASS){
			String strErrorMsg =  YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			throw new YoctoGeneralException(strErrorMsg);
		}
		else
		{
			YoctoSDKUtils.setEnvironmentVariables(project, elem);
			configureAutotoolsOptions(project);
		}
	}
}

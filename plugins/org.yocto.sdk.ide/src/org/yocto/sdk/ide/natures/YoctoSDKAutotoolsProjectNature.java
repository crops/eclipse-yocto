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
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.utils.ProjectPreferenceUtils;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;

@SuppressWarnings("restriction")
public class YoctoSDKAutotoolsProjectNature extends YoctoSDKProjectNature {
	public static final  String YoctoSDK_AUTOTOOLS_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKAutotoolsNature";

	private static final String DEFAULT_HOST_STR = "host";
	private static final String DEFAULT_TARGET_STR = "target";
	private static final String DEFAULT_BUILD_STR = "build";
	private static final String DEFAULT_AUTOGEN_OPT_STR = "autogenOpts";

	private static final String DEFAULT_CONFIGURE_STR = "configure";
	private static final String DEFAULT_AUTOGEN_STR = "autogen";
	private static final String DEFAULT_LIBTOOL_SYSROOT_PREFIX = " --with-libtool-sysroot=";

	public static void configureAutotoolsOptions(IProject project) {
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		YoctoUIElement elem = ProjectPreferenceUtils.getElemFromProjectEnv(project);
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
		cfg.setOption(DEFAULT_BUILD_STR, splitString(strConfigure, "--build="));
		cfg.setOption(DEFAULT_HOST_STR, splitString(strConfigure, "--host="));
		cfg.setOption(DEFAULT_TARGET_STR, splitString(strConfigure, "--target="));
		cfg.setOption(DEFAULT_AUTOGEN_STR, autogen_setting);
		cfg.setOption(DEFAULT_AUTOGEN_OPT_STR, strConfigure);

		AutotoolsConfigurationManager.getInstance().addConfiguration(project, cfg);
		AutotoolsConfigurationManager.getInstance().saveConfigs(project);
	}

	private static String splitString(String strValue, String strDelim) {
		int iBeginIndex = strValue.indexOf(strDelim);

		if (iBeginIndex < 0) {
			return "";
		}
		int iEndIndex = strValue.indexOf(' ', iBeginIndex + 1);

		if (iEndIndex < 0) {
			return strValue.substring(iBeginIndex + strDelim.length());
		} else {
			return strValue.substring(iBeginIndex + strDelim.length(), iEndIndex);
		}
	}
}

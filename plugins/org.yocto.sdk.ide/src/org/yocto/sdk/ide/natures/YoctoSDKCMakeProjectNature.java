/*******************************************************************************
 * Copyright (c) 2013 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide.natures;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;

public class YoctoSDKCMakeProjectNature extends YoctoSDKProjectNature {
	public static final  String YoctoSDK_CMAKE_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKCMakeNature";

	// Considered poky's cmake.bbclass for this method
	public static void extendProjectEnvironmentForCMake(IProject project) {
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		String delimiter = manager.getDefaultDelimiter();

		env.addVariable("CCACHE", "", IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		env.addVariable("OECMAKE_SOURCEPATH", "..",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		String oecmakeBuildPathString = "";
		env.addVariable("OECMAKE_BUILDPATH", oecmakeBuildPathString,
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable("EXTRA_OEMAKE", "-C " + oecmakeBuildPathString,
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		String ccString = YoctoSDKUtils.getEnvValue(project, "CC");
		String ccFlagsString = "";

		if (!ccString.equals("") && !ccString.equals(" ")) {
			ccString.trim();
			String[] ccSplitString = ccString.split(" ");
			ccString = ccSplitString[0];

			for(int i=1; i<ccSplitString.length; i++) {
				if(ccSplitString[i].indexOf("sysroot")<0)
					ccFlagsString+=ccSplitString[i] + " ";
			}
		}

		env.addVariable("OECMAKE_C_COMPILER", ccString,
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		String cxxString = YoctoSDKUtils.getEnvValue(project, "CXX");
		String cxxFlagsString = "";

		if (!cxxString.equals("") && !cxxString.equals(" ")) {
			cxxString.trim();
			String[] cxxSplitString = cxxString.split(" ");
			cxxString = cxxSplitString[0];

			for(int i=1; i<cxxSplitString.length; i++) {
				if(cxxSplitString[i].indexOf("sysroot")<0)
					cxxFlagsString+=cxxSplitString[i] +  " ";
			}
		}

		env.addVariable("OECMAKE_CXX_COMPILER", cxxString,
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		String hostCCArchString = YoctoSDKUtils.getEnvValue(project, "HOST_CC_ARCH");
		String toolchainOptionsString = YoctoSDKUtils.getEnvValue(project, "TOOLCHAIN_OPTIONS");
		String cppFlagsString = YoctoSDKUtils.getEnvValue(project, "CPPFLAGS") + " " + ccFlagsString;
		cxxFlagsString = YoctoSDKUtils.getEnvValue(project, "CXXFLAGS") + " " + cxxFlagsString;
		String selectedOptimizationString = YoctoSDKUtils.getEnvValue(project, "SELECTED_OPTIMIZATION");
		env.addVariable("OECMAKE_C_FLAGS", hostCCArchString + " " + toolchainOptionsString + " " + cppFlagsString,
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable("OECMAKE_CXX_FLAGS", hostCCArchString + " " + toolchainOptionsString + " " + cxxFlagsString
				+ " -fpermissive",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable("OECMAKE_C_FLAGS_RELEASE", selectedOptimizationString + " " + cppFlagsString + " -DNDEBUG",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable("OECMAKE_CXX_FLAGS_RELEASE", selectedOptimizationString + " " + cxxFlagsString + " -DNDEBUG",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		env.addVariable("OECMAKE_RPATH", "",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable("OECMAKE_PERLNATIVE_DIR", "",
				IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		try {
			CoreModel.getDefault().setProjectDescription(project, cpdesc);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}

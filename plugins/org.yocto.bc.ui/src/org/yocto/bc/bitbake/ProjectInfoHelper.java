/*****************************************************************************
 * Copyright (c) 2013 Ken Gilmer, Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * A helper class for ProjectInfo related tasks.
 * 
 * @author kgilmer
 * 
 */
public class ProjectInfoHelper {

	protected static final String DEFAULT_INIT_SCRIPT = "oe-init-build-env";
	/**
	 * @param path
	 * @return The path to bitbake init script
	 * @throws IOException
	 */
	public static String getInitScriptPath(String path) throws IOException {
		String val = path + File.separator + DEFAULT_INIT_SCRIPT;

		File inFile = new File(path, ".eclipse-data");
		if(inFile.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(inFile));
			val = br.readLine();
			br.close();
		}

		return val;
	}

	public static String getProjectName(String projectRoot) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; ++i) {
			try {
				if (projects[i].getLocationURI().getPath().equals(projectRoot)) {
					return projects[i].getName();
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}
}

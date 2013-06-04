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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.yocto.bc.ui.model.ProjectInfo;

/**
 * A helper class for ProjectInfo related tasks.
 * 
 * @author kgilmer
 * 
 */
public class ProjectInfoHelper {
	public static final String OEFS_SCHEME = "OEFS://";
	public static final String FILE_SCHEME = "file";
	public static final String RSE_SCHEME = "rse";

	protected static final String DEFAULT_INIT_SCRIPT = "oe-init-build-env";
	/**
	 * @param path
	 * @return The path to bitbake init script
	 * @throws IOException
	 */
	public static String getInitScriptPath(URI uri) throws IOException {
		String val = uri.getPath() + "/" + DEFAULT_INIT_SCRIPT;
		return val;
	}

	public static String getProjectName(URI projectRoot) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; ++i) {
			try {
				if (projects[i].getLocationURI().equals(projectRoot)) {
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

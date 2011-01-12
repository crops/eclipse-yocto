/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

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

	/**
	 * @param path
	 * @return The path to bitbake init script
	 * @throws IOException
	 */
	public static String getInitScriptPath(String path) throws IOException {
		File inFile = new File(path, ".eclipse-data");
		BufferedReader br = new BufferedReader(new FileReader(inFile));

		String val = br.readLine();

		br.close();

		return val;
	}
	
	public static String getInitScript(String path) throws IOException {
		File inFile = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		StringBuffer sb = new StringBuffer();
		String line = null;
		
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		
		br.close();

		return sb.toString();
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

	/**
	 * This method will store the path to the bitbake init script for future
	 * reference.
	 * 
	 * @param path
	 * @param projInfo
	 * @throws IOException
	 */
	public static void store(String path, ProjectInfo projInfo) throws IOException {
		writeToFile(path, projInfo.getInitScriptPath());
	}

	private static void writeToFile(String path, String init) throws IOException {
		File outFile = new File(path, ".eclipse-data");
		FileOutputStream fos = new FileOutputStream(outFile);

		fos.write(init.getBytes());

		fos.flush();
		fos.close();
	}

}

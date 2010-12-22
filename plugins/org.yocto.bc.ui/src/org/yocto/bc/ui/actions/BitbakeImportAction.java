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
package org.yocto.bc.ui.actions;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.yocto.bc.bitbake.BBCommonVars;
import org.yocto.bc.bitbake.BBRecipe;
import org.yocto.bc.ui.Activator;

public  class BitbakeImportAction extends AbstractBitbakeCommandAction {

	private class ImportJob extends Job {

		public ImportJob() {
			super(getJobTitle());
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			try {
				BBRecipe br = new BBRecipe(bbs, recipe.getLocationURI().getPath());
				br.initialize();
				String filePath = (String) br.get(BBCommonVars.S);
				
				//"${WORKDIR}/${PN}-${PV}"
				if (filePath == null) {
					filePath = ((String) br.get(BBCommonVars.WORKDIR)) + File.separator + ((String) br.get(BBCommonVars.PN)) + "-" + ((String) br.get(BBCommonVars.PV));
				}
				
				String projectName = (String) br.get(BBCommonVars.PN);
				
				if (filePath == null || projectName == null) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to parse recipe file.");
				}
				
				File workdir = new File(filePath);
				
				if (workdir.exists() && workdir.isFile()) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, workdir.getPath() + " is an invalid workdir.");
				}
			
				if (!workdir.exists()) {
					execCommands(new String[] {"bitbake -c patch -b " + recipe.getLocationURI().getPath()}, monitor);
				}
				
				if (!workdir.exists()) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to retrieve sources from BitBake.  Consult console.");
				}
				
				IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
				IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
				IProject proj = wsroot.getProject(projectName);
				proj.create(desc, monitor);		
				proj.open(monitor);
				
				String copyCmd = "cp -r " + workdir.getAbsolutePath() + File.separator + "* \"" + proj.getLocationURI().getPath() + "\"";
				execCommands(new String[] {copyCmd} , monitor);
				
				proj.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				
			} catch (Exception e) {
				e.printStackTrace();
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to create project.", e);
			}
			
			return Status.OK_STATUS;
		}
		
	}

	@Override
	public String [] getCommands() {
		return null;
	}
	

	@Override
	public Job getJob() {
		return new ImportJob();
	}
	
	@Override
	public String getJobTitle() {
		return "Importing " + recipe.getName();
	}
}
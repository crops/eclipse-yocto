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
package org.yocto.bc.ui.wizards.newproject;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;
import org.yocto.bc.ui.model.ProjectInfo;


/**
 * Creates a bbc project
 * @author kgilmer
 *
 */
public class CreateBBCProjectOperation extends WorkspaceModifyOperation {

	public static final String OEFS_SCHEME = "OEFS://";
	public static final QualifiedName BBC_PROJECT_INIT = new QualifiedName(null, "BBC_PROJECT_INIT");
	public static void addNatureToProject(IProject proj, String nature_id, IProgressMonitor monitor) throws CoreException {
		IProjectDescription desc = proj.getDescription();
		Vector<String> natureIds = new Vector<String>();
		
		natureIds.add(nature_id);
		natureIds.addAll(Arrays.asList(desc.getNatureIds()));
		desc.setNatureIds((String[]) natureIds.toArray(new String[natureIds.size()]));
		
		proj.setDescription(desc, monitor);
	}
	
	private ProjectInfo projInfo;

	public CreateBBCProjectOperation(ProjectInfo projInfo) {
		this.projInfo = projInfo;
	}
	
	protected void addNatures(IProject proj, IProgressMonitor monitor) throws CoreException {
		addNatureToProject(proj, BitbakeCommanderNature.NATURE_ID, monitor);
	}

	private IProjectDescription createProjectDescription(IWorkspace workspace, ProjectInfo projInfo2) throws CoreException {
		IProjectDescription desc = workspace.newProjectDescription(projInfo2.getProjectName());

		try {
			desc.setLocationURI(new URI(OEFS_SCHEME + projInfo2.getRootPath()));
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to load filesystem.", e));
		}

		return desc;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		IProjectDescription desc = createProjectDescription(ResourcesPlugin.getWorkspace(), projInfo);
		
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();

		IProject proj = wsroot.getProject(projInfo.getProjectName());
		proj.create(desc, monitor);
		proj.open(monitor);

		addNatures(proj, monitor);
	}
	
	public ProjectInfo getProjectInfo() {
		return projInfo;
	}
}

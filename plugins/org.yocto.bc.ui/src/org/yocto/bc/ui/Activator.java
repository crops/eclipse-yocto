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
package org.yocto.bc.ui;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import org.yocto.bc.bitbake.BBSession;
import org.yocto.bc.bitbake.ProjectInfoHelper;
import org.yocto.bc.bitbake.ShellSession;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;

public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.yocto.bc.ui";
	public static final String IMAGE_VARIABLE = "IMAGE_VARIABLE";
	public static final String IMAGE_FUNCTION = "IMAGE_FUNCTION";

	// The shared instance
	private static Activator plugin;
	private static Map shellMap;
	private static Map projInfoMap;
	private static Hashtable bbSessionMap;
	
	/**
	 * Get or create a BitBake session passing in ProjectInfo
	 * @param pinfo
	 * @return
	 * @throws IOException
	 */
	public static BBSession getBBSession(String projectRoot, Writer out) throws IOException {
		if (bbSessionMap == null) {
			bbSessionMap = new Hashtable();
		}
		
		BBSession bbs = (BBSession) bbSessionMap.get(projectRoot);
		
		if (bbs == null) {
			bbs = new BBSession(getShellSession(projectRoot, out), projectRoot);
			bbSessionMap.put(projectRoot, bbs);
		}
		
		return bbs;
	}
	
	/**
	 * Get or create a BitBake session passing in ProjectInfo
	 * @param pinfo
	 * @return
	 * @throws IOException
	 */
	public static BBSession getBBSession(String projectRoot) throws IOException {
		if (bbSessionMap == null) {
			bbSessionMap = new Hashtable();
		}
		
		BBSession bbs = (BBSession) bbSessionMap.get(projectRoot);
		
		if (bbs == null) {
			bbs = new BBSession(getShellSession(projectRoot, null), projectRoot);
			bbSessionMap.put(projectRoot, bbs);
		}
		
		return bbs;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static ProjectInfo getProjInfo(String location) throws CoreException, InvocationTargetException, InterruptedException {
		if (projInfoMap == null) {
			projInfoMap = new Hashtable();
		}
		
		ProjectInfo pi = (ProjectInfo) projInfoMap.get(location);
		
		if (pi == null) {
			pi = new ProjectInfo();
			pi.setLocation(location);
			try {
				pi.setInitScriptPath(ProjectInfoHelper.getInitScriptPath(location));
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		}
		
		return pi;
	}

	/**
	 * @param absolutePath
	 * @return a cached shell session for a given project root.
	 * @throws IOException 
	 */
	private static ShellSession getShellSession(String absolutePath, Writer out) throws IOException {
		if (shellMap == null) {
			shellMap = new Hashtable();
		}
		
		ShellSession ss = (ShellSession) shellMap.get(absolutePath);
		
		if (ss == null) {
			ss = new ShellSession(ShellSession.SHELL_TYPE_BASH, new File(absolutePath), ProjectInfoHelper.getInitScriptPath(absolutePath), out);
		}
		
		return ss;
	}

	private static String loadInit(String absolutePath) throws CoreException {
		IProject [] prjs = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		IProject foundPrj = null;
		
		for (int i = 0; i < prjs.length; ++i) {
			IProject p = prjs[i];
			
			System.out
					.println(p.getDescription().getLocationURI().getPath());
			
			if (p.getDescription().getLocationURI().getPath().equals(absolutePath)) {
				foundPrj = p;
				break;
			}
		}
		
		if (foundPrj == null) {
			throw new RuntimeException("Unable to find project associated with path! " + absolutePath);
		}
	
		return foundPrj.getPersistentProperty(CreateBBCProjectOperation.BBC_PROJECT_INIT);
	}
	
	public static void putProjInfo(String location, ProjectInfo pinfo) {
		if (projInfoMap == null) {
			projInfoMap = new Hashtable();
		}
		
		
		
		projInfoMap.put(location, pinfo);
	}
	
	/**
	 * The constructor
	 */
	public Activator() {
		this.getImageRegistry().put(IMAGE_VARIABLE, Activator.getImageDescriptor("icons/variable.gif"));
		this.getImageRegistry().put(IMAGE_FUNCTION, Activator.getImageDescriptor("icons/function.gif"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Reset a configuration
	 * @param path
	 */
	public static void resetBBSession(String path) {
		shellMap.remove(path);
		bbSessionMap.remove(path);
	}
}

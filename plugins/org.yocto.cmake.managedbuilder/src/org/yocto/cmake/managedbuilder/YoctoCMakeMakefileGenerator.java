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
package org.yocto.cmake.managedbuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.yocto.cmake.managedbuilder.job.ExecuteConfigureJob;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;

public class YoctoCMakeMakefileGenerator implements IManagedBuilderMakefileGenerator2 {

	private static final String TOOLCHAINCMAKE_FILE_NAME = "toolchain.cmake"; //$NON-NLS-1$
	private static final String MAKEFILE_NAME = "Makefile"; //$NON-NLS-1$
	private static final String CMAKE_FILE_NAME = "CMakeLists.txt"; //$NON-NLS-1$
	private static final String CMAKECACHE_FILE_NAME = "CMakeCache.txt"; //$NON-NLS-1$

	private IProject project;
	private int lastBuildInfoChecksum = 0;
	private IConfiguration configuration;
	private IProgressMonitor monitor;
	private IPath buildDir = null;

	@Override
	public String getMakefileName() {
		return MAKEFILE_NAME;
	}

	@Override
	public IPath getBuildWorkingDir() {
		IPath buildWorkingDir = null;

		if (buildDir != null) {
			buildWorkingDir = buildDir.removeFirstSegments(1);
		}

		return buildWorkingDir;
	}

	@Override
	public boolean isGeneratedResource(IResource resource) {
		return false;
	}

	@Override
	public void initialize(IProject project, IManagedBuildInfo info,
			IProgressMonitor monitor) {
		this.project = project;
		this.configuration = info.getDefaultConfiguration();
		this.monitor = monitor;

		if (info.getDefaultConfiguration() != null) {
			buildDir = project.getFolder(info.getConfigurationName()).getFullPath();
		}
	}

	@Override
	public void initialize(int buildKind, IConfiguration configuration, IBuilder builder,
			IProgressMonitor monitor) {
		this.project = configuration.getOwner().getProject();
		this.configuration = configuration;
		this.monitor = monitor;
		this.buildDir = project.getFolder(configuration.getName()).getFullPath();
	}

	@Override
	public void generateDependencies() throws CoreException {
		// nothing to do here
	}

	@Override
	public void regenerateDependencies(boolean force) throws CoreException {
		generateDependencies();
	}

	@Override
	public MultiStatus generateMakefiles(IResourceDelta delta)
			throws CoreException {
		int currentBuildInfoChecksum = ManagedBuildManager.getBuildInfo(project).hashCode();

		IFile cmakeFile = project.getFile(CMAKE_FILE_NAME);
		IResourceDelta cmakeDelta = delta.findMember(cmakeFile.getProjectRelativePath());
		IResourceDelta[] deltas = delta
				.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.REMOVED);

		if (deltas.length > 0 || cmakeDelta != null || currentBuildInfoChecksum != lastBuildInfoChecksum) {
			lastBuildInfoChecksum = currentBuildInfoChecksum;
			return regenerateMakefiles();
		} else {
			// CMake is not needed to run prior to building
			// just return that makefile generation is completed
			return new MultiStatus(
					ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK,
					new String(YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.ok.makefilesStillValid")), null); //$NON-NLS-1$
		}
	}

	@Override
	public MultiStatus regenerateMakefiles() throws CoreException {
		String taskName =
				YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.configure.creatingMakefiles"); //$NON-NLS-1$
		monitor.beginTask(taskName, 20);

		IFile cmakeFile = project.getFile(CMAKE_FILE_NAME);
		if (!cmakeFile.exists()) {
			return new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.CANCEL,
					new String(YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.cancel.missingCMakeList")), null); //$NON-NLS-1$
		}

		// Retrieve Build directory
		IPath workingDir = getBuildWorkingDir();
		IPath location = project.getLocation().append(workingDir);
		monitor.worked(1);

		// Create build directory if it doesn't exist
		if (!location.toFile().exists()) {
			monitor.subTask(
					YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.creatingBuildDirectory")); //$NON-NLS-1$
			location.toFile().mkdirs();
		} else {
			monitor.subTask(
					YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.removingCacheFiles")); //$NON-NLS-1$
			IFile cmakeCache = project.getFile(workingDir.append(CMAKECACHE_FILE_NAME));
			cmakeCache.delete(true, monitor);
		}
		monitor.setTaskName(taskName);

		// Create the Makefiles by executing cmake
		ExecuteConfigureJob job =
				new ExecuteConfigureJob(
						YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.configureJob.name"), //$NON-NLS-1$
						project, configuration, location);
		job.setPriority(Job.BUILD);
		job.setUser(false);

		job.schedule();
		try {
			job.join();
			monitor.done();
			return new MultiStatus(
					Activator.PLUGIN_ID, job.getResult().getSeverity(),
					job.getResult().getMessage(), null);
		} catch (InterruptedException e) {
			return new MultiStatus(
					Activator.PLUGIN_ID,
					IStatus.ERROR, new String(
							YoctoCMakeMessages.getString("YoctoCMakeMakefileGenerator.error.makeFileGenerationFailed")), null); //$NON-NLS-1$
		}
	}
}

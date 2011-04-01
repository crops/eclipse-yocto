package org.yocto.sdk.ide;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class YoctoSDKEmptyProjectNature implements IProjectNature {

	public static final  String YoctoSDK_EMPTY_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKEmptyNature";

	private IProject proj;

	public void configure() throws CoreException {
		// TODO Auto-generated method stub

	}

	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	public IProject getProject() {
		// TODO Auto-generated method stub
		return proj;
	}

	public void setProject(IProject project) {
		// TODO Auto-generated method stub
		this.proj = project;
	}

	public static void addYoctoSDKEmptyNature(IProject project, IProgressMonitor monitor) throws CoreException {
		YoctoSDKUtils.addNature(project, YoctoSDK_EMPTY_NATURE_ID, monitor);
	}

}

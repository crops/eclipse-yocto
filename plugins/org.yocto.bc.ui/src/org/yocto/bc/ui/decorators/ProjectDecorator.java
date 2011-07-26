package org.yocto.bc.ui.decorators;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;

public class ProjectDecorator implements ILightweightLabelDecorator {

	private ImageDescriptor image;

	public ProjectDecorator() {
		image = Activator.getImageDescriptor("icons/oe_decorator.gif");
	}
	

	public void decorate(Object element, IDecoration decoration) {
		IProject p = (IProject) element;
		
		try {
			if (p.isOpen() && p.hasNature(BitbakeCommanderNature.NATURE_ID)) {
				decoration.addOverlay(image, IDecoration.TOP_RIGHT);
			}
		} catch (CoreException e) {			
			e.printStackTrace();
		}
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}


	public void removeListener(ILabelProviderListener arg0) {
	}

}

/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 * BMW Car IT - extracted in an own class
 *******************************************************************************/
package org.yocto.sdk.ide.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class YoctoSDKNatureUtils {

	public static void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException
	{
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (natureId.equals(natures[i]))
				return;
		}

		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = natureId;
		description.setNatureIds(newNatures);
		project.setDescription(description, monitor);

	}

}

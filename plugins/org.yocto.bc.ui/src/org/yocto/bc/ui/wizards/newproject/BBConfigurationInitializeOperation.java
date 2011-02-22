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
package org.yocto.bc.ui.wizards.newproject;

import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.yocto.bc.bitbake.BBSession;
import org.yocto.bc.bitbake.ProjectInfoHelper;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;

public class BBConfigurationInitializeOperation implements IRunnableWithProgress {

	private final ProjectInfo pinfo;
	private final Writer writer;

	public BBConfigurationInitializeOperation(ProjectInfo pinfo) {
		this.pinfo = pinfo;
		writer = null;
	}

	public BBConfigurationInitializeOperation(ProjectInfo pinfo, Writer writer) {
		this.pinfo = pinfo;
		this.writer = writer;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		BBSession session;
		try {
			ProjectInfoHelper.store(pinfo.getRootPath(), pinfo);
			/* llu detach BBSession
			session = Activator.getBBSession(pinfo.getRootPath(), writer);

			if (!session.isInitialized()) {
				session.initialize();
			}
			*/

		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}
}

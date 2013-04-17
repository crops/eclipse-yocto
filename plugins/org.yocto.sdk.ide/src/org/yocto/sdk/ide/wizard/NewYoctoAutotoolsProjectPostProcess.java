/*******************************************************************************
 * Copyright (c) 2012 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.cdt.core.templateengine.process.processes.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.natures.YoctoSDKAutotoolsProjectNature;

public class NewYoctoAutotoolsProjectPostProcess extends ProcessRunner {

	public static final String CHMOD_COMMAND = "chmod +x "; //$NON-NLS-1$
	public static final String AUTOGEN_SCRIPT_NAME = "autogen.sh"; //$NON-NLS-1$

	public NewYoctoAutotoolsProjectPostProcess() {}

	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor) throws ProcessFailureException {

		String projectName = args[0].getSimpleValue();

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			if (!project.exists()) {
				throw new ProcessFailureException(Messages.getString("NewManagedProject.4") + projectName); //$NON-NLS-1$
			} else if (!project.hasNature(YoctoSDKAutotoolsProjectNature.YoctoSDK_AUTOTOOLS_NATURE_ID)) {
				throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + //$NON-NLS-1$
						YoctoSDKMessages.getFormattedString("AutotoolsProjectPostProcess.WrongProjectNature", //$NON-NLS-1$
								projectName));
			} else {
				IPath path = project.getLocation();
				String path_str = path.toString();
				String autogen_cmd = CHMOD_COMMAND + path_str + File.separator + AUTOGEN_SCRIPT_NAME;
				try {
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(autogen_cmd);
					InputStream stdin = proc.getInputStream();
					InputStreamReader isr = new InputStreamReader(stdin);
					BufferedReader br = new BufferedReader(isr);
					String line = null;
					String error_message = ""; //$NON-NLS-1$

					while ( (line = br.readLine()) != null) {
						error_message = error_message + line;
					}

					int exitVal = proc.waitFor();
					if (exitVal != 0) {
						throw new ProcessFailureException(
								YoctoSDKMessages.getFormattedString("AutotoolsProjectPostProcess.ChmodFailure", //$NON-NLS-1$
										projectName));
					}
				} catch (Throwable t) {
					t.printStackTrace();

				}
			}
		} catch (Exception e) {
			throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + e.getMessage(), e); //$NON-NLS-1$
		}
	}
}

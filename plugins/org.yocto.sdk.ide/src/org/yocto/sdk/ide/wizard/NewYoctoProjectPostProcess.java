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


@SuppressWarnings("restriction")
public class NewYoctoProjectPostProcess extends ProcessRunner {
	
	public NewYoctoProjectPostProcess() {}
	
	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor) throws ProcessFailureException {

		String projectName = args[0].getSimpleValue();
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			if (!project.exists()) {
					throw new ProcessFailureException(Messages.getString("NewManagedProject.4") + projectName); //$NON-NLS-1$
				} else {
					IPath path = project.getLocation();
					String path_str = path.toString();
					String autogen_cmd = "chmod +x " + path_str + "/autogen.sh";
					try {
						Runtime rt = Runtime.getRuntime();
						Process proc = rt.exec(autogen_cmd);
						InputStream stdin = proc.getInputStream();
						InputStreamReader isr = new InputStreamReader(stdin);
						BufferedReader br = new BufferedReader(isr);
						String line = null;
						String error_message = "";
						
						while ( (line = br.readLine()) != null) {
							error_message = error_message + line;
						}
						
						int exitVal = proc.waitFor();
						if (exitVal != 0) {
							throw new ProcessFailureException("Failed to make autogen.sh executable for project: " + projectName);
						} 
					} catch (Throwable t) {
						t.printStackTrace();
						
					}
				}
		}
		catch (Exception e)
		{
			throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + e.getMessage(), e); //$NON-NLS-1$
		} 
	}
}

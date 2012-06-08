/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.cdt.internal.autotools.core.AutotoolsNewMakeGenerator;
import org.eclipse.cdt.internal.autotools.ui.actions.InvokeAction;
import org.eclipse.cdt.internal.autotools.ui.actions.InvokeMessages;

import org.yocto.sdk.ide.YoctoSDKPlugin;

@SuppressWarnings("restriction")
public class InvokeSyncAction extends InvokeAction {
	protected void executeLocalConsoleCommand(final IConsole console, final String actionName, final String command,
			final String[] argumentList, final IPath execDir, final String password) throws CoreException, IOException {
		
		String errMsg = null;
		IProject project = getSelectedContainer().getProject();
		// Get a build console for the project
		ConsoleOutputStream consoleOutStream = console.getOutputStream();
		// FIXME: we want to remove need for ManagedBuilderManager, but how do we
		// get environment variables.
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration cfg = info.getDefaultConfiguration();

		StringBuffer buf = new StringBuffer();
		String[] consoleHeader = new String[3];

		consoleHeader[0] = actionName;
		consoleHeader[1] = cfg.getName();
		consoleHeader[2] = project.getName();
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
		String invokeMsg = InvokeMessages.getFormattedString("InvokeAction.console.message", //$NON-NLS-1$
				new String[]{actionName, execDir.toString()}); //$NON-NLS-1$
		buf.append(invokeMsg);
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
		consoleOutStream.write(buf.toString().getBytes());
		consoleOutStream.flush();
		
		ArrayList<String> additionalEnvs = new ArrayList<String>();
		String strippedCommand = AutotoolsNewMakeGenerator.stripEnvVars(command, additionalEnvs);
		// Get a launcher for the config command
		CommandLauncher launcher = new CommandLauncher();
		// Set the environment
		IEnvironmentVariable variables[] = ManagedBuildManager
				.getEnvironmentVariableProvider().getVariables(cfg, true);
		String[] env = null;
		ArrayList<String> envList = new ArrayList<String>();
		if (variables != null) {
			for (int i = 0; i < variables.length; i++) {
				envList.add(variables[i].getName()
						+ "=" + variables[i].getValue()); //$NON-NLS-1$
			}
			// add any additional environment variables specified ahead of script
			if (additionalEnvs.size() > 0)
				envList.addAll(additionalEnvs); 
			env = (String[]) envList.toArray(new String[envList.size()]);
		}
		OutputStream stdout = consoleOutStream;
		OutputStream stderr = consoleOutStream;

		launcher.showCommand(true);
		// Run the shell script via shell command.
		Process proc = launcher.execute(new Path(strippedCommand), argumentList, env,
					execDir, new NullProgressMonitor());

		if (proc != null) {
			// Close the input of the process since we will never write to it
			OutputStream out = proc.getOutputStream();
			if (!password.isEmpty()) {
				out.write(password.getBytes());
				out.write("\n".getBytes());
			}
			out.close();
			
			if (launcher.waitAndRead(stdout, stderr) != CommandLauncher.OK) {
				errMsg = launcher.getErrorMessage();
			}
		} else {
			errMsg = launcher.getErrorMessage();
		}
		
		if (errMsg != null)
			YoctoSDKPlugin.logErrorMessage(errMsg);	
	}	
}

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

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.cdt.internal.autotools.ui.actions.InvokeAction;
import org.eclipse.swt.widgets.Shell;

import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoSDKUtils;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.YoctoSDKProjectNature;
import org.yocto.sdk.ide.YoctoUIElement;


@SuppressWarnings("restriction")
public class ReconfigYoctoAction extends InvokeAction {
    private static final String DIALOG_TITLE  = "Menu.SDK.Dialog.Title";
    private static final String CONSOLE_MESSAGE  = "Menu.SDK.Console.Configure.Message";

	
	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;

		IProject project = container.getProject();

		//Try to get the per project configuration first
		YoctoUIElement elem = YoctoSDKUtils.getElemFromProjectEnv(project);
		if (elem.getStrToolChainRoot().isEmpty()|| elem.getStrTarget().isEmpty()){
			// No project environment has been set yet, use the Preference values
			elem = YoctoSDKUtils.getElemFromStore();
		} 

		SDKLocationDialog optionDialog = new SDKLocationDialog(new Shell(), YoctoSDKMessages.getString(DIALOG_TITLE), elem);
		optionDialog.open();
		elem = optionDialog.getElem();
		if (elem.getStrToolChainRoot() != null) {			
			try {
				YoctoSDKProjectNature.setEnvironmentVariables(project, elem);
				YoctoSDKProjectNature.configureAutotoolsOptions(project);
				IConsole console = CCorePlugin.getDefault().getConsole("org.yocto.sdk.ide.YoctoConsole");
				console.start(project);
				ConsoleOutputStream consoleOutStream;
				consoleOutStream = console.getOutputStream();
				String messages = YoctoSDKMessages.getString(CONSOLE_MESSAGE);
				consoleOutStream.write(messages.getBytes());
				consoleOutStream.flush();
				consoleOutStream.close();
			}
			catch (CoreException e)
			{
				System.out.println(e.getMessage());
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage());
			}
			catch (YoctoGeneralException e)
			{
				System.out.println(e.getMessage());
			}
		}
	}

	public void dispose() {

	}
}

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
package org.yocto.bc.ui.actions;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;
import org.yocto.bc.ui.wizards.variable.VariableWizard;

/**
 * Action to launch the Variable Wizard.
 * @author kgilmer
 *
 */
public class LaunchVariableWizardAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private Map<String, Object> session;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		VariableWizard wizard = new VariableWizard(session);
		
		WizardDialog wd = new WizardDialog(window.getShell(), wizard);
		wd.create();
		wd.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		session = null;
		
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			
			if (element instanceof IResource) {
				IProject p = ((IResource)element).getProject();

				try {
					if (p.isOpen() && p.hasNature(BitbakeCommanderNature.NATURE_ID)) {
						session = Activator.getBBSession(((IResource)element).getProject().getLocationURI().getPath());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	
		action.setEnabled(session != null);
	}
}
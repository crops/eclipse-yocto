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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.yocto.bc.ui.wizards.NewBitBakeFileRecipeWizard;

public class LaunchNewRecipeWizardAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private IStructuredSelection selection;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		NewBitBakeFileRecipeWizard wizard = new NewBitBakeFileRecipeWizard();
		
		wizard.init(window.getWorkbench(), selection);
		WizardDialog wd = new WizardDialog(window.getShell(), wizard);
		wd.create();
		wd.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}
}
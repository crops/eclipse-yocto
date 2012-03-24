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
package org.yocto.sdk.remotetools.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import org.yocto.sdk.remotetools.wizards.bsp.YoctoBSPWizard;

public class YoctoBspHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		 // Instantiates and initializes the wizard
	    YoctoBSPWizard bspWizard = new YoctoBSPWizard();
	    //bspWizard.init(window.getWorkbench(), (IStructuredSelection)selection);
	    // Instantiates the wizard container with the wizard and opens it
	    WizardDialog dialog = new WizardDialog(window.getShell(), bspWizard);
	    //dialog.create();
	    dialog.open();
		//YoctoBspDialog setting=new YoctoBspDialog(window.getShell());
		//setting.open();
		return null;
	}
}

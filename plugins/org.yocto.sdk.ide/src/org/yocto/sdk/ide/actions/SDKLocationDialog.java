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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

public class SDKLocationDialog extends Dialog {
	private String title;
	private YoctoUISetting yoctoUISetting;
	private YoctoUIElement elem;

	public SDKLocationDialog(Shell parentShell, String dialogTitle, YoctoUIElement elem) {
        super(parentShell);
        this.setElem(elem);
        this.yoctoUISetting = new YoctoUISetting(elem);
        this.title = dialogTitle;
        setShellStyle(getShellStyle() | SWT.RESIZE);
        
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite result = (Composite) super.createDialogArea(parent);
				
		try {
			yoctoUISetting.createComposite(result);
			yoctoUISetting.validateInput(SDKCheckRequestFrom.Menu, false);
		} catch (YoctoGeneralException e) {
			// TODO Auto-generated catch block
			System.out.println("Have you ever set the project specific Yocto Settings?");
			System.out.println(e.getMessage());
		}

		return result;
	}


	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			//this should not be called in fact, because widget change should be called firstly!
			try {
				yoctoUISetting.validateInput(SDKCheckRequestFrom.Menu, true);
				this.setElem(yoctoUISetting.getCurrentInput());
				super.buttonPressed(buttonId);
			} catch (YoctoGeneralException e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
			}
		}
		else if (buttonId == IDialogConstants.CANCEL_ID)
		{
			super.buttonPressed(buttonId);
		}			
	}

	public void setElem(YoctoUIElement elem) {
		this.elem = elem;
	}

	public YoctoUIElement getElem() {
		return elem;
	}

}

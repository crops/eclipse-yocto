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
package org.yocto.sdk.ide.preferences;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoSDKUtils;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckRequestFrom;

import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;



public class YoctoSDKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    
	private YoctoUISetting yoctoUISetting;

	public YoctoSDKPreferencePage() {
		//super(GRID);
        setPreferenceStore(YoctoSDKPlugin.getDefault().getPreferenceStore());
        //setDescription(YoctoSDKMessages.getString(PREFERENCES_Yocto_CONFIG));
        YoctoUIElement elem = YoctoSDKUtils.getElemFromStore();
        this.yoctoUISetting = new YoctoUISetting(elem);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		final Composite result= new Composite(parent, SWT.NONE);
		
		try {
			yoctoUISetting.createComposite(result);
			yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
			Dialog.applyDialogFont(result);
			return result;
		} catch (YoctoGeneralException e) {
			System.out.println("Have you ever set Yocto Project Reference before?");
			System.out.println(e.getMessage());
			return result;
		}

	}
	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		
		
		try {
			yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, true);

			YoctoUIElement elem = yoctoUISetting.getCurrentInput();
			YoctoSDKUtils.saveElemToStore(elem);

			return super.performOk();
		} catch (YoctoGeneralException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			return false;
		}		
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		YoctoUIElement defaultElement = YoctoSDKUtils.getDefaultElemFromStore();
		yoctoUISetting.setCurrentInput(defaultElement);
		super.performDefaults();
	}


}

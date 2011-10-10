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
		IPreferenceStore store= getPreferenceStore();
		ArrayList<Control> arrControls =  this.yoctoUISetting.getfControls();
		
		for (int i = 0; i < arrControls.size(); i++)
		{
			Control control = arrControls.get(i);
			String[] controlData = (String[])control.getData();
			String sKey = controlData[0];
			if (control instanceof Button)
			{
				sKey = sKey.substring(0, sKey.lastIndexOf("_"));
			}
			String sValue = store.getDefaultString(sKey);
			if (control instanceof Button)
			{
				if (sValue.equalsIgnoreCase("true"))
				{
					if (controlData[0].endsWith("_1")) //the 1st radio button of the group
						((Button)control).setSelection(true);
					else
						((Button)control).setSelection(false);//the 2nd radio button of the group
				}
				else
				{
					if (controlData[0].endsWith("_1")) //the 1st radio button of the group
						((Button)control).setSelection(false);
					else
						((Button)control).setSelection(true);//the 2nd radio button of the group
				}
			}
			else if (control instanceof Text)
			{
				((Text)control).setText(sValue);
			}
			else if (control instanceof Combo)
			{
				if (!sValue.isEmpty())
					((Combo)control).select(Integer.valueOf(sValue).intValue());
			}
		}

		try {
			yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, false);
		} catch (YoctoGeneralException e) {
			System.out.println("Have you ever set Yocto Project Reference before?");
			System.out.println(e.getMessage());
		}
		super.performDefaults();
	}


}

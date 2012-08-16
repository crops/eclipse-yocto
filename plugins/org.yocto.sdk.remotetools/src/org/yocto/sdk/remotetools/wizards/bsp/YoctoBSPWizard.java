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
package org.yocto.sdk.remotetools.wizards.bsp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.yocto.sdk.remotetools.YoctoBspElement;
import org.yocto.sdk.remotetools.YoctoBspPropertyElement;
import org.yocto.sdk.remotetools.YoctoJSONHelper;

/**
 * A wizard for creating Yocto BSP.
 * 
 * @author jzhang
 * 
 */
public class YoctoBSPWizard extends Wizard {
	private static final String CREATE_CMD = "/scripts/yocto-bsp create ";
	private static final String PROPERTY_VALUE_FILE = "/tmp/propertyvalues.json";
	
	private MainPage mainPage;
	private PropertiesPage propertiesPage;
	private YoctoBspElement bspElem;

	public YoctoBSPWizard() {
		super();
		bspElem = new YoctoBspElement();
	}

	@Override 
	public IWizardPage getNextPage(IWizardPage page) {
		propertiesPage.onEnterPage(mainPage.getBSPElement());
		return propertiesPage;
	}
	 
	@Override
	public void addPages() {
		mainPage = new MainPage(bspElem);
		addPage(mainPage);
		propertiesPage = new PropertiesPage(bspElem);
		addPage(propertiesPage);
	}

	@Override
	public boolean performFinish() {
		if (propertiesPage.validatePage()) {
			HashSet<YoctoBspPropertyElement> properties = propertiesPage.getProperties();
			YoctoJSONHelper.createBspJSONFile(properties);
			YoctoBspElement element = mainPage.getBSPElement();
			
			String create_bsp_cmd = element.getMetadataLoc() + CREATE_CMD + 
									element.getBspName() + " " + element.getKarch();
			
			if (!element.getBspOutLoc().isEmpty())
				create_bsp_cmd = create_bsp_cmd + " -o " + element.getBspOutLoc();
			else
				create_bsp_cmd = create_bsp_cmd + " -o " + element.getMetadataLoc() + "/meta-" + element.getBspName();
			create_bsp_cmd = create_bsp_cmd + " -i " + PROPERTY_VALUE_FILE;
			
			try {
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(create_bsp_cmd);
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
					MessageDialog.openError(getShell(),"Yocto-BSP", error_message);
					return false;
				} else {
					MessageDialog.openInformation(getShell(), "Yocto-BSP", error_message);
					return true;
				}
			} catch (Throwable t) {
				t.printStackTrace();
				return false;
			}
		} else {
			MessageDialog.openError(getShell(), "Yocto-BSP", "Property settings contains error!");
			return false;
		}
		
	}
	
	public boolean canFinish() {
		return (mainPage.validatePage() && propertiesPage.validatePage());
	}
}

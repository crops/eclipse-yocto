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
	private final YoctoBspElement bspElem;

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

	private BSPAction createBSP(){
		YoctoBspElement element = mainPage.getBSPElement();
		String createBspCmd = element.getMetadataLoc() + CREATE_CMD +
								element.getBspName() + " " + element.getKarch();

		if (!element.getBspOutLoc().isEmpty())
			createBspCmd = createBspCmd + " -o " + element.getBspOutLoc();
		else
			createBspCmd = createBspCmd + " -o " + element.getMetadataLoc() + "/meta-" + element.getBspName();
		createBspCmd = createBspCmd + " -i " + PROPERTY_VALUE_FILE;

		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(),  new OutputCollectorThread(createBspCmd), "Creating BSP ");
		progressDialog.run(true);
		return progressDialog.getBspAction();
	}

	@Override
	public boolean performFinish() {
		if (propertiesPage.validatePage()) {
			HashSet<YoctoBspPropertyElement> properties = propertiesPage.getProperties();
			YoctoJSONHelper.createBspJSONFile(properties);

			BSPAction createBSPAction = createBSP();
			if (createBSPAction.getMessage() !=  null && !createBSPAction.getMessage().isEmpty()) {
				MessageDialog.openError(getShell(),"Yocto-BSP", createBSPAction.getMessage());
				return false;
			} else {
				String message = "";
				for (String item : createBSPAction.getItems())
					message += item + "\n";
				MessageDialog.openInformation(getShell(), "Yocto-BSP", message);
				return true;
			}
		} else {
			MessageDialog.openError(getShell(), "Yocto-BSP", "Property settings contains error!");
			return false;
		}

	}

	@Override
	public boolean canFinish() {
		return (mainPage.validatePage() && propertiesPage.validatePage());
	}
}

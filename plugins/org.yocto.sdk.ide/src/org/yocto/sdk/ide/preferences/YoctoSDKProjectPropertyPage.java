/*******************************************************************************
 * Copyright (c) 2012 BMW Car IT GmbH.
 * Copyright (c) 2010 Intel.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT GmbH - initial implementation
 * Intel - initial API implementation (copied from YoctoSDKPreferencePage)
 *******************************************************************************/
package org.yocto.sdk.ide.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoSDKUtils;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.YoctoUISetting;

public class YoctoSDKProjectPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private YoctoUISetting yoctoUISetting;
	private IProject project = null;

	@Override
	protected Control createContents(Composite parent) {
		YoctoUIElement uiElement = loadUIElement();
		this.yoctoUISetting = new YoctoUISetting(uiElement);

		initializeDialogUnits(parent);
		final Composite result = new Composite(parent, SWT.NONE);

		try {
			yoctoUISetting.createComposite(result);
			yoctoUISetting
					.validateInput(SDKCheckRequestFrom.Preferences, false);
			Dialog.applyDialogFont(result);
			return result;
		} catch (YoctoGeneralException e) {
			System.out.println("Have you ever set Yocto Project Reference before?");
			System.out.println(e.getMessage());
			return result;
		}
	}

	private IProject getProject() {
		if (project != null) {
			return project;
		}

		IAdaptable adaptable = getElement();
		if (adaptable == null) {
			throw new IllegalStateException("Project can only be retrieved after properties page has been set up.");
		}

		project = (IProject) adaptable.getAdapter(IProject.class);
		return project;
	}

	private YoctoUIElement loadUIElement() {
		YoctoUIElement uiElement = YoctoSDKUtils.getElemFromProjectEnv(getProject());

		if (uiElement.getStrToolChainRoot().isEmpty()
				|| uiElement.getStrTarget().isEmpty()) {
			// No project environment has been set yet, use the Preference
			// values
			uiElement = YoctoSDKUtils.getElemFromStore();
		}

		return uiElement;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		YoctoUIElement defaultElement = YoctoSDKUtils.getDefaultElemFromStore();
		yoctoUISetting.setCurrentInput(defaultElement);
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		try {
			yoctoUISetting.validateInput(SDKCheckRequestFrom.Preferences, true);

			YoctoUIElement elem = yoctoUISetting.getCurrentInput();
			YoctoSDKUtils.saveElemToProjectEnv(elem, getProject());

			return super.performOk();
		} catch (YoctoGeneralException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
			return false;
		}
	}
}

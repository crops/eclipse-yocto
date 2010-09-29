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

import org.yocto.sdk.remotetools.Activator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SimpleSettingDialog extends BaseSettingDialog {
	
	private final String connPropName;
	/*
	protected SimpleSettingDialog(Shell parentShell, String title, String conn) {
		super(parentShell,title,conn);
	}
	*/
	
	public SimpleSettingDialog(Shell parentShell, String title, String connPropertyName) {
		super(parentShell,
				title,
				Activator.getDefault().getDialogSettings().get(connPropertyName)
				);
		connPropName=connPropertyName;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		return super.createDialogArea(parent);
	}

	@Override
	protected void okPressed() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
	    // store the value of the generate sections checkbox
		if(getCurrentConnection()==null) {
			settings.put(connPropName,
					(String)null);
		}else {
			settings.put(connPropName, 
					getCurrentConnection().getAliasName());
		}
		super.okPressed();
	}	
}

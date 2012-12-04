/*******************************************************************************
 * Copyright (c) 2012 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT GmbH - initial implementation
 *******************************************************************************/
package org.yocto.sdk.ide.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class YoctoSDKProjectPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	public YoctoSDKProjectPropertyPage() {
	}

	@Override
	protected Control createContents(Composite parent) {
		return null;
	}

}

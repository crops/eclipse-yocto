/*******************************************************************************
 * Copyright (c) 2013 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.yocto.sdk.ide.YoctoSDKPlugin;

public class YoctoSDKCMakeProjectNature extends YoctoSDKProjectNature {
	public static final  String YoctoSDK_CMAKE_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKCMakeNature";

}

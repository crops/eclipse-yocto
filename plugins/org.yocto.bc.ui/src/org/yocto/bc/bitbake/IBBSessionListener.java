/*******************************************************************************
 * Copyright (c) 2012 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/

package org.yocto.bc.bitbake;

import org.eclipse.core.resources.IResource;

public interface IBBSessionListener {
	public void changeNotified(IResource[] added, IResource[] removed, IResource[] changed);
}

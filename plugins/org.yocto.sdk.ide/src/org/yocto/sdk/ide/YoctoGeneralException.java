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
package org.yocto.sdk.ide;

/* All specific exceptions raised from yocto project should use
 * this specific exception class.
 * Currently we only use it for message printing 
 */

public class YoctoGeneralException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6600798490815526253L;
	/**
	 * 
	 */

	public YoctoGeneralException(String message)
	{
		super(message);
	}
}

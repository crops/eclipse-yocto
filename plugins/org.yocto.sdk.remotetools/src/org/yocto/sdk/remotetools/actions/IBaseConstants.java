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

public interface IBaseConstants {
	public static final String TCF_TYPE_ID="org.eclipse.tcf.rse.systemType";//$NON-NLS-1$
	
	public static final String QUALIFIER="org.yocto.sdk.remotetools";//$NON-NLS-1$
	
	public static final String CONNECTION_NAME_OPROFILE = QUALIFIER + "connection.oprofile"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_UST = QUALIFIER + "connection.ust"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_POWERTOP = QUALIFIER + "connection.powertop"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_LATENCYTOP = QUALIFIER + "connection.latencytop"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_PERF = QUALIFIER + "connection.perf"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_SYSTEMTAP = QUALIFIER + "connection.systemtap"; //$NON-NLS-1$
}

package org.yocto.sdk.remotetools.actions;

public interface IBaseConstants {
	public static final String TCF_TYPE_ID="org.eclipse.tm.tcf.rse.systemType";//$NON-NLS-1$
	
	public static final String QUALIFIER="org.yocto.sdk.remotetools";//$NON-NLS-1$
	
	public static final String CONNECTION_NAME_OPROFILE = QUALIFIER + "connection.oprofile"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_UST = QUALIFIER + "connection.ust"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_POWERTOP = QUALIFIER + "connection.powertop"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_LATENCYTOP = QUALIFIER + "connection.latencytop"; //$NON-NLS-1$
	public static final String CONNECTION_NAME_PERF = QUALIFIER + "connection.perf"; //$NON-NLS-1$
}

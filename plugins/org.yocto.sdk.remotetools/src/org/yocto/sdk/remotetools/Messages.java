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
package org.yocto.sdk.remotetools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.yocto.sdk.remotetools.messages"; //$NON-NLS-1$

	public static String ErrorNoSubsystem;
	public static String ErrorConnectSubsystem;
	public static String ErrorNoHost;
	public static String ErrorNoRemoteService;
	
	public static String ErrorOprofileViewer;
	public static String ErrorOprofile;
	public static String ErrorLttvGui;
	public static String ErrorUstProject;
	
	public static String InfoDownload;
	public static String InfoUpload;
	
	public static String BaseSettingDialog_Connection;
	public static String BaseSettingDialog_New;
	public static String BaseSettingDialog_Properties;
	public static String Usttrace_Argument_Text;
	public static String Usttrace_Application_Text;
	public static String Usttrace_Trace_Loc_Text;
	public static String Powertop_Time_Text;
	public static String Powertop_ShowPid_Text;
	public static String TerminalViewer_text;
	public static String Systemtap_KO_Text;
	public static String Import_to_Project;
	
	public static String LocalJob_Title;
	public static String ErrorLocalJob;
	public static String RemoteShellExec_1;
	public static String RemoteShellExec_2;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

package org.yocto.sdk.remotetools;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.yocto.sdk.remotetools.messages"; //$NON-NLS-1$

	public static String ErrorNoSubsystem;
	public static String ErrorConnectSubsystem;
	public static String ErrorNoHost;
	public static String ErrorNoRemoteService;
	
	public static String ErrorOprofileViewer;
	public static String ErrorLttvGui;
	
	public static String InfoDownload;
	public static String InfoUpload;
	
	public static String BaseSettingDialog_Connection;
	public static String BaseSettingDialog_New;
	public static String BaseSettingDialog_Properties;
	public static String Argument_Text;
	public static String Application_Text;
	public static String Powertop_Time_Text;
	public static String Powertop_ShowPid_Text;
	public static String TerminalViewer_text;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

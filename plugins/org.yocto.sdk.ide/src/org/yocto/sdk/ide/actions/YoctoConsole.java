package org.yocto.sdk.ide.actions;

import org.eclipse.linuxtools.internal.cdt.autotools.ui.Console;
import org.yocto.sdk.ide.YoctoSDKMessages;

@SuppressWarnings("restriction")
public class YoctoConsole extends Console {
	private static final String CONTEXT_MENU_ID = "YoctoConsole"; 
	private static final String CONSOLE_NAME = YoctoSDKMessages.getString("Console.SDK.Name");
	
	public YoctoConsole() {
		super(CONSOLE_NAME, CONTEXT_MENU_ID);
	}
}

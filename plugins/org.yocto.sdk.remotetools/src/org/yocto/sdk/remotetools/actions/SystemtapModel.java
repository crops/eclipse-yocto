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

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.yocto.sdk.remotetools.ShellSession;

public class SystemtapModel extends BaseModel {
	protected static final String DEFAULT_INIT_SCRIPT = "oe-init-build-env";
	protected static final String SYSTEMTAP_CONSOLE = "Systemtap Console";
	protected MessageConsole sessionConsole;
	private String metadata_location;
	private String remote_host;
	private String user_id;
	private String systemtap_script;
	private String systemtap_args;
	
	Display display;
	
	public SystemtapModel(String metadata_location, String remote_host, String user_id, String systemtap_script, String systemtap_args, Display display) {
		super(null);
		this.metadata_location=metadata_location;
		this.remote_host=remote_host;
		this.user_id=user_id;
		this.systemtap_script=systemtap_script;
		this.systemtap_args = systemtap_args;
		this.display=display;
		if (sessionConsole == null) {
			IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = conMan.getConsoles();
			for (int i = 0; i < existing.length; i++)
				if (SYSTEMTAP_CONSOLE.equals(existing[i].getName())) {
					sessionConsole = (MessageConsole) existing[i];
					break;
				}
			if (sessionConsole == null) {
				sessionConsole = new MessageConsole(SYSTEMTAP_CONSOLE, null);
				conMan.addConsoles(new IConsole[] { sessionConsole });
			}
		}
		
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(sessionConsole);
	}
	
	@Override
	
	public void preProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
	}
    
	public void process(IProgressMonitor monitor)
	throws InvocationTargetException, InterruptedException {
		try {
			ShellSession shell = new ShellSession(ShellSession.SHELL_TYPE_BASH, 
												new File(this.metadata_location),
												DEFAULT_INIT_SCRIPT, sessionConsole.newOutputStream());
			boolean acceptedKey = shell.ensureKnownHostKey(user_id, remote_host);
			if (acceptedKey) {
				String crosstapCmd = "crosstap " + user_id + "@" + remote_host + " " + systemtap_script;
				if (systemtap_args != null)
					crosstapCmd = crosstapCmd + " " + systemtap_args;
				shell.execute(crosstapCmd);
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}
	}
	
	
	public void postProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
	}

}

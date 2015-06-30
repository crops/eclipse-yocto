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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.yocto.remote.utils.CommonHelper;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.ShellSession;

public class SystemtapModel extends BaseModel {
	protected static final String DEFAULT_INIT_SCRIPT = "oe-init-build-env";
	protected static final String SYSTEMTAP_CONSOLE = "Systemtap Console";

	private static final String TASK_NAME = "systemtap command";

	protected MessageConsole sessionConsole;
	private String metadata_location;
	private String remote_host;
	private String user_id;
	private String systemtap_script;
	private String systemtap_args;

	Display display;
	
	public SystemtapModel(String metadata_location, String remote_host, String user_id, String systemtap_script, String systemtap_args, Display display) {
		super(null, TASK_NAME, "", "");
		this.metadata_location = metadata_location;
		this.remote_host = remote_host;
		this.user_id = user_id;
		this.systemtap_script = systemtap_script;
		this.systemtap_args = systemtap_args;
		this.display = display;
	}
	
	@Override
	public void preProcess(IProgressMonitor monitor) 
			throws InvocationTargetException, InterruptedException {
		final ITerminalServiceSubSystem terminalSubSystem = RemoteHelper.getTerminalSubSystem(host);
		if (!terminalSubSystem.isConnected()) {
			try {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(null);
				dialog.run(true, true, new IRunnableWithProgress(){
					@Override
					public void run(IProgressMonitor monitor) {
						monitor.beginTask("Connecting to remote target ...", 100);
						try {
							terminalSubSystem.connect(new NullProgressMonitor(), false);
							monitor.done();
						} catch (Exception e) {
							CommonHelper.showErrorDialog("Connection failure", null, e.getMessage());
							monitor.done();
						}
					}
				});
			} catch (OperationCanceledException e) {
				// user canceled, return silently
			} catch (Exception e) {
				SystemBasePlugin.logError(e.getLocalizedMessage(), e);
			}
		}
		}
	
		protected String changeTerm = "export TERM=vt100;";
    
	@Override
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
}

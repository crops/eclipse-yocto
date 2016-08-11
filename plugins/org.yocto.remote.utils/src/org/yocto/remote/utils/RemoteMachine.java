/*******************************************************************************
 * Copyright (c) 2013 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ioana Grigoropol(Intel) - initial API and implementation
 *******************************************************************************/
package org.yocto.remote.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.services.local.shells.LocalShellService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.shells.IShellService;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.IFileServiceSubSystem;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.IShellServiceSubSystem;
import org.eclipse.ui.console.MessageConsole;

@SuppressWarnings("restriction")
public class RemoteMachine {
	public static final String PROXY = "proxy";

	private Map<String, String> environment;
	private MessageConsole console;
	private CommandResponseHandler cmdHandler;
	private IShellService shellService;
	private IHost connection;

	private ISubSystem fileSubSystem;
	private IFileService fileService;

	public RemoteMachine(IHost connection) {
		setConnection(connection);
	}

	public String[] prepareEnvString(IProgressMonitor monitor){
		String[] env = null;
		try {
			if (shellService instanceof LocalShellService) {
				env  = shellService.getHostEnvironment();
			} else {
				List<String> envList = new ArrayList<String>();
				getRemoteEnvProxyVars(monitor);
				String value = "";
				for (String varName : environment.keySet()){
					value = varName + "=" + environment.get(varName);
					envList.add(value);
				}
				env = envList.toArray(new String[envList.size()]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return env;
	}
	public void getRemoteEnvProxyVars(IProgressMonitor monitor){
		try {
			if (environment != null && !environment.isEmpty())
				return;

			environment = new HashMap<String, String>();

			IShellService shellService = getShellService(SubMonitor.convert(monitor, 7));

			ProcessStreamBuffer buffer = null;
			try {
				SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
				IHostShell hostShell = shellService.runCommand("", "env" + " ; echo " + RemoteHelper.TERMINATOR + "; exit;", new String[]{}, subMonitor);
				buffer = RemoteHelper.processOutput(subMonitor, hostShell, cmdHandler);
				for(int i = 0; i < buffer.getOutputLines().size(); i++) {
					String out = buffer.getOutputLines().get(i);
					String[] tokens = out.split("=");
					if (tokens.length != 2)
						continue;
					String varName = tokens[0];
					String varValue = tokens[1];
					if (varName.contains(PROXY))
						environment.put(varName, varValue);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}
	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}
	public MessageConsole getConsole() {
		if (console == null)
			console = ConsoleHelper.findConsole(ConsoleHelper.YOCTO_CONSOLE);

		ConsoleHelper.showConsole(console);
		return console;
	}
	public CommandResponseHandler getCmdHandler() {
		if (cmdHandler == null)
			cmdHandler = new CommandResponseHandler(getConsole());
		return cmdHandler;
	}

	public IShellService getShellService(IProgressMonitor monitor) throws Exception {
		if (shellService != null)
			return shellService;

		final ISubSystem subsystem = getShellSubsystem();

		if (subsystem == null)
			throw new Exception(Messages.ErrorNoSubsystem);

		try {
			subsystem.connect(monitor, false);
		} catch (CoreException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		if (!subsystem.isConnected())
			throw new Exception(Messages.ErrorConnectSubsystem);

		shellService = ((IShellServiceSubSystem) subsystem).getShellService();
		return shellService;
	}

	private ISubSystem getShellSubsystem() {
		if (connection == null)
			return null;
		ISubSystem[] subSystems = connection.getSubSystems();
		for (int i = 0; i < subSystems.length; i++) {
			if (subSystems[i] instanceof IShellServiceSubSystem)
				return subSystems[i];
		}
		return null;
	}

	public IHost getConnection() {
		return connection;
	}
	public void setConnection(IHost connection) {
		this.connection = connection;
	}

	public IFileService getRemoteFileService(IProgressMonitor monitor) throws Exception {
		if (fileService == null) {

			while(getFileSubsystem() == null)
				Thread.sleep(2);
			try {
				getFileSubsystem().connect(monitor, false);
			} catch (CoreException e) {
				throw e;
			} catch (OperationCanceledException e) {
				throw new CoreException(Status.CANCEL_STATUS);
			}

			if (!getFileSubsystem().isConnected())
				throw new Exception(Messages.ErrorConnectSubsystem);

			fileService = ((IFileServiceSubSystem) getFileSubsystem()).getFileService();
		}
		return fileService;
	}

	public ISubSystem getFileSubsystem() {
		if (fileSubSystem == null) {
			if (connection == null)
				return null;
			ISubSystem[] subSystems = connection.getSubSystems();
			for (int i = 0; i < subSystems.length; i++) {
				if (subSystems[i] instanceof IFileServiceSubSystem) {
					fileSubSystem = subSystems[i];
					break;
				}
			}
		}
		return fileSubSystem;
	}

}

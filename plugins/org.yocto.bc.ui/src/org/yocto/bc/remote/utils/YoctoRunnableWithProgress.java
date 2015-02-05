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
package org.yocto.bc.remote.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteServices;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.swt.widgets.Display;
import org.yocto.remote.utils.CommandResponseHandler;
import org.yocto.remote.utils.OutputProcessor;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.YoctoCommand;

public class YoctoRunnableWithProgress implements IRunnableWithProgress {

	private String taskName;
	private IRemoteConnection remoteConnection;
	private IRemoteServices remoteServices;
	private IProgressMonitor monitor;
	private final ICalculatePercentage calculator;
	private int reportedWorkload;

	private final YoctoCommand command;

	public YoctoRunnableWithProgress(YoctoCommand command) throws IOException {
		this.command = command;
		this.calculator = new GitCalculatePercentage();
	}

	private interface ICalculatePercentage {
		public float calWorkloadDone(String info) throws IllegalArgumentException;
	}

	private class GitCalculatePercentage implements ICalculatePercentage {
		final Pattern pattern = Pattern.compile("^Receiving objects:\\s*(\\d+)%.*");
		@Override
		public float calWorkloadDone(String info) throws IllegalArgumentException {
			Matcher m = pattern.matcher(info.trim());
			if(m.matches()) {
				return new Float(m.group(1)) / 100;
			}else {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			this.monitor = monitor;
			this.monitor.beginTask(taskName, RemoteHelper.TOTALWORKLOAD);

			if (!remoteConnection.isOpen()) {
				try {
					remoteConnection.open(monitor);
				} catch (RemoteConnectionException e1) {
					e1.printStackTrace();
				}
			}

			remoteServices.initialize(new NullProgressMonitor());

			try {
				IHost connection = RemoteHelper.getRemoteConnectionByName(remoteConnection.getName());
				YoctoThread th = new YoctoThread(connection, command);
				th.run();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				monitor.done();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	class YoctoRunnableOutputProcessor extends OutputProcessor{

		public YoctoRunnableOutputProcessor(IProgressMonitor monitor,
				IHostShell hostShell, CommandResponseHandler cmdHandler,
				String task) {
			super(monitor, hostShell, cmdHandler, task);
		}
		@Override
		protected boolean isErrChStop(char ch) {
			return (ch == '\n' || ch == '\r');
		}

		@Override
		protected boolean isOutChStop(char ch) {
			return (ch == '\n');
		}

		@Override
		protected void processOutputBufferLine(char ch, String str) {
			processBuffer.addOutputLine(str);
		}

		@Override
		protected void processErrorBufferLine(char ch, String str) {
			processBuffer.addOutputLine(str);
			if (ch == '\r')
				reportProgress(str);
		}

	}

	class YoctoThread implements Runnable{
		private final IHost connection;
		private final YoctoCommand command;
		private final CommandResponseHandler cmdHandler;
		private IHostShell hostShell;

		YoctoThread(IHost connection, YoctoCommand command){
			this.connection = connection;
			this.cmdHandler = RemoteHelper.getCommandHandler(connection);
			this.command = command;
		}

		@Override
		public void run() {
			try {
				hostShell = RemoteHelper.runCommandRemote(this.connection, command, monitor);
				command.setProcessBuffer(new YoctoRunnableOutputProcessor(monitor, hostShell, cmdHandler, taskName).processOutput());
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private void updateMonitor(final int work){

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (monitor != null) {
					monitor.worked(work);
				}
			}

		});
	}

	private void doneMonitor(){
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				monitor.done();
			}
		});
	}

	public void reportProgress(String info) {
		if(calculator == null) {
			updateMonitor(1);
		} else {
			float percentage;
			try {
				percentage = calculator.calWorkloadDone(info);
			} catch (IllegalArgumentException e) {
				System.out.println(info);
				//can't get percentage
				return;
			}
			int delta = (int) (RemoteHelper.TOTALWORKLOAD * percentage - reportedWorkload);
			if( delta > 0 ) {
				updateMonitor(delta);
				reportedWorkload += delta;
			}

			if (reportedWorkload == RemoteHelper.TOTALWORKLOAD)
				doneMonitor();
		}
	}

	public IRemoteConnection getRemoteConnection() {
		return remoteConnection;
	}

	public void setRemoteConnection(IRemoteConnection remoteConnection) {
		this.remoteConnection = remoteConnection;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public IRemoteServices getRemoteServices() {
		return remoteServices;
	}

	public void setRemoteServices(IRemoteServices remoteServices) {
		this.remoteServices = remoteServices;
	}
}

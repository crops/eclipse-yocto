/*******************************************************************************
 * Copyright (c) 2013 BMW Car IT GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BMW Car IT - initial API and implementation
 *******************************************************************************/
package org.yocto.cmake.managedbuilder.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Map;

public class SystemProcess {

	private LinkedList<String> command = new LinkedList<String>();
	private File workingDirectory;
	private ProcessBuilder builder;
	private Process process;
	private StreamPipe outputRedirector;

	public SystemProcess(LinkedList<String> command) {
		this(command, null);
	}

	public SystemProcess(LinkedList<String> command, File directory) {
		this(command, directory, null);
	}

	public SystemProcess(LinkedList<String> command, File directory, Map<String, String> additionalEnvironmentVariables) {
		super();
		this.command = command;
		if (directory != null) {
			this.workingDirectory = directory;
		}
		setUpProcessBuilder(additionalEnvironmentVariables);
	}

	private void setUpProcessBuilder(Map<String, String> additionalEnvironmentVariables) {
		builder = new ProcessBuilder(command);
		if (workingDirectory != null) {
			builder.directory(workingDirectory);
		}
		builder.redirectErrorStream(true);

		if(additionalEnvironmentVariables != null && !additionalEnvironmentVariables.isEmpty()) {
			builder.environment().putAll(additionalEnvironmentVariables);
		}
	}

	public void start(OutputStream out) throws IOException {
		if (builder != null) {
			process = builder.start();
			outputRedirector = redirectOutput(process, out);
		}
	}

	public int waitForResultAndStop() throws InterruptedException {
		if (process == null) {
			return -1;
		}

		process.waitFor();
		outputRedirector.interrupt();

		return process.exitValue();
	}

	public void interrupt() {
		process.destroy();
	}

	private StreamPipe redirectOutput(Process process, OutputStream out) {
		InputStream in = process.getInputStream();
		StreamPipe stdoutPipe = new StreamPipe(in, out);
		stdoutPipe.start();

		return stdoutPipe;
	}


	private class StreamPipe extends Thread {
		private InputStream in;
		private OutputStream[] outputs;
		boolean shutdown = false;

		public StreamPipe(InputStream in, OutputStream... outputs) {
			this.in = in;
			this.outputs = outputs;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[1024];
			int length = 0;

			try {
				while(!shutdown && ((length = in.read(buffer)) > -1)) {
					for (OutputStream out : outputs) {
						out.write(buffer, 0, length);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void interrupt() {
			shutdown = true;
			super.interrupt();
		}
	}
}

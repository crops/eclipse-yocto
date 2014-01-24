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


public class YoctoCommand {
	private String command;
	private String initialDirectory;
	private String arguments;
	private ProcessStreamBuffer processBuffer;

	public YoctoCommand(String command, String initialDirectory, String arguments){
		this.setCommand(command);
		this.setInitialDirectory(initialDirectory);
		this.setArguments(arguments);
		this.setProcessBuffer(new ProcessStreamBuffer(false));
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = "bash -l -c \"" + command + "\"";
	}

	public String getInitialDirectory() {
		return initialDirectory;
	}

	public void setInitialDirectory(String initialDirectory) {
		this.initialDirectory = initialDirectory;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	public ProcessStreamBuffer getProcessBuffer() {
		return processBuffer;
	}

	public void setProcessBuffer(ProcessStreamBuffer processBuffer) {
		this.processBuffer = processBuffer;
	}

	@Override
	public String toString() {
		return command + " " + arguments;
	}
}

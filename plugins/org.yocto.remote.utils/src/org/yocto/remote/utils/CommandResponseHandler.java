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

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class CommandResponseHandler implements ICommandResponseHandler {
	private MessageConsoleStream consoleStream;
	private Boolean errorOccured = false;

	public CommandResponseHandler(MessageConsole console) {
		try {
			this.consoleStream = console.newMessageStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Boolean hasError() {
		return errorOccured;
	}

	@Override
	public void response(String line, boolean isError) {
		try {
			if (isError) {
				errorOccured = true;
			}
			consoleStream.println(line);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
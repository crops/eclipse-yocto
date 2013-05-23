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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;


public class ConsoleUtility {

	public static IOConsoleOutputStream getConsoleOutput(String consoleName) {
		return getConsole(consoleName).newOutputStream();
	}

	public static MessageConsole getConsole(String consoleName) {
		MessageConsole foundConsole = findConsole(consoleName);
		if (foundConsole != null) {
			foundConsole.clearConsole();
		} else {
			foundConsole = new MessageConsole(consoleName, null);
			ConsolePlugin.getDefault().
			getConsoleManager().addConsoles(new IConsole[] { foundConsole });
		}

		return foundConsole;
	}

	public static MessageConsole findConsole(String consoleName) {
		IConsole[] consoles =
				ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		for (IConsole console : consoles) {
			if (console.getName().equals(consoleName)) {
				return (MessageConsole) console;
			}
		}

		return null;
	}
}

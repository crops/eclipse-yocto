/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.io.IOException;

/**
 * Represents the bitbake environment of a recipe package.
 * @author kgilmer
 *
 */
public class BBRecipe extends BBSession {
	private final BBSession session;
	private final String filePath;

	public BBRecipe(BBSession session, String filePath) throws IOException {
		super(session.shell, session.pinfo.getRootPath());
		this.session = session;
		this.filePath = filePath;
	}
	
	@Override
	public void initialize() throws Exception {
		if (initialized) {
			return;
		}

		String ret = shell.execute("bitbake -e -b " + filePath);
		properties = parseBBEnvironment(ret);
		
		if (ret == null || properties.size() == 0) {
			throw new IOException("Failed to parse " + filePath);
		}
		
		initialized = true;
	}
}

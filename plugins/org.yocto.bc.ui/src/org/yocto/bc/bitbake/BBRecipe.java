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
		this.parsingCmd = "DISABLE_SANITY_CHECKS=1 bitbake -e -b " + filePath;
	}
	
	@Override
	public void initialize() throws Exception {
		if (this.size() == 0) {
			//System.out.println("Failed to parse " + filePath);
			//throw new IOException("Failed to parse " + filePath);
		}
	}

	protected String getDefaultDepends() {
		return this.filePath;
	}

	public BBSession getSession() {
		return session;
	}
}

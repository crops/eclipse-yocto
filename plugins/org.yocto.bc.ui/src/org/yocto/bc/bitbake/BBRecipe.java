/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer, 2013 Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.io.IOException;
import java.net.URI;

/**
 * Represents the bitbake environment of a recipe package.
 * @author kgilmer
 *
 */
public class BBRecipe extends BBSession {
	private final BBSession session;
	private final URI fileURI;

	public BBRecipe(BBSession session, URI filePath) throws IOException {
		super(session.shell, session.pinfo.getOriginalURI());
		this.session = session;
		this.fileURI = filePath;
		this.parsingCmd = "DISABLE_SANITY_CHECKS=1 bitbake -e -b " + filePath;
	}
	
	@Override
	public void initialize() throws Exception {
		if (this.size() == 0) {
			//System.out.println("Failed to parse " + fileURI);
			//throw new IOException("Failed to parse " + filePath);
		}
	}

	@Override
	protected URI getDefaultDepends() {
		return this.fileURI;
	}
}

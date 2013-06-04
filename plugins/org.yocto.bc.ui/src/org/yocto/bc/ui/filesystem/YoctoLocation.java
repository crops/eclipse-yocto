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
package org.yocto.bc.ui.filesystem;

import java.net.URI;
import java.net.URISyntaxException;

public class YoctoLocation{
	URI oefsURI;
	URI originalURI;

	public YoctoLocation(){
		try {
			oefsURI = new URI("");
			originalURI = new URI("");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public URI getOEFSURI() {
		return oefsURI;
	}

	public URI getOriginalURI() {
		return originalURI;
	}

	public void setOriginalURI(URI originalURI) {
		this.originalURI = originalURI;
	}

	public void setOEFSURI(URI uri) {
		this.oefsURI = uri;
	}
}

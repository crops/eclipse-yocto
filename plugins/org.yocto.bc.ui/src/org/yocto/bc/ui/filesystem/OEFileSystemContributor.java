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
package org.yocto.bc.ui.filesystem;

import java.net.URI;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ide.fileSystem.FileSystemContributor;

public class OEFileSystemContributor extends FileSystemContributor  {

	@Override
	public URI browseFileSystem(String initialPath, Shell shell) {
		return null;
	}
	
	@Override
	public URI getURI(String string) {
		return super.getURI(string);
	}
	
}

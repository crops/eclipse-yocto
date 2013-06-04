/*****************************************************************************
 * Copyright (c) 2013 Ken Gilmer, Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.ui.model;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ptp.remote.core.IRemoteServices;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.files.IFileService;
import org.yocto.bc.bitbake.ProjectInfoHelper;
import org.yocto.bc.ui.filesystem.YoctoLocation;
import org.yocto.remote.utils.RemoteHelper;

public class ProjectInfo implements IModelElement {
	private String name;
	private YoctoLocation location;
	private String init;
	private IHost connection;
	private IRemoteServices remoteServices;

	public ProjectInfo() {
	}
	
	public String getInitScriptPath() {
		return init;
	}
	public String getProjectName() {
		return name;
	}
	public URI getOriginalURI() {
		return location.getOriginalURI();
	}

	public URI getOEFSURI() {
		return location.getOEFSURI();
	}

	@Override
	public void initialize() throws Exception {
		name = new String();
		location = new YoctoLocation();
		init = new String();
	}

	public void setInitScriptPath(String init) {
		this.init = init;
	}

	public void setLocationURI(URI location) {
		if (this.location == null)
			this.location = new YoctoLocation();
		this.location.setOriginalURI(location);
		try {
			this.location.setOEFSURI(new URI(ProjectInfoHelper.OEFS_SCHEME + location.getPath() ));
		} catch (URISyntaxException e) {
			try {
				this.location.setOEFSURI(new URI(""));
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public IHost getConnection() {
		if (connection == null) {
			connection = RemoteHelper.getRemoteConnectionForURI(location.getOriginalURI(), new NullProgressMonitor());
		}
		return connection;
	}

	public void setConnection(IHost connection) {
		this.connection = connection;
	}

	public IRemoteServices getRemoteServices() {
		return remoteServices;
	}

	public void setRemoteServices(IRemoteServices remoteServices) {
		this.remoteServices = remoteServices;
	}

	public IFileService getFileService(IProgressMonitor monitor){
		try {
			return RemoteHelper.getConnectedRemoteFileService(connection, monitor);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

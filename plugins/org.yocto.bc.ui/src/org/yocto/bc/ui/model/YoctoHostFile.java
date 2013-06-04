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
package org.yocto.bc.ui.model;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.yocto.bc.ui.filesystem.Messages;
import org.yocto.bc.ui.filesystem.OEFile;
import org.yocto.bc.ui.filesystem.Policy;
import org.yocto.remote.utils.RemoteHelper;

public class YoctoHostFile implements IHostFile{
	private IHostFile file;
	private final URI fileURI;
	private ProjectInfo projectInfo;
	private IFileService fileService;

	public YoctoHostFile(ProjectInfo pInfo, URI fileURI, IProgressMonitor monitor) throws SystemMessageException {
		this.projectInfo = pInfo;
		this.fileURI = fileURI;
		String path = fileURI.getPath();
		fileService = projectInfo.getFileService(monitor);
		file = RemoteHelper.getRemoteHostFile(projectInfo.getConnection(), path, monitor);
	}

	public YoctoHostFile(ProjectInfo projectInfo, URI uri) {
		this.fileURI = uri;
		this.projectInfo = projectInfo;
	}

	public IHostFile getFile() {
		return file;
	}
	public void setFile(IHostFile file) {
		this.file = file;
	}
	public ProjectInfo getProjectInfo() {
		return projectInfo;
	}
	public void setProjectInfo(ProjectInfo projectInfo) {
		this.projectInfo = projectInfo;
	}
	@Override
	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}
	@Override
	public String getName() {
		return file.getName();
	}
	public URI getProjectLocationURI() {
		return projectInfo.getOriginalURI();
	}
	public URI getLocationURI() {
		projectInfo.getOriginalURI().getPath().indexOf(file.getAbsolutePath());
		return projectInfo.getOriginalURI();
	}
	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}
	@Override
	public String getParentPath() {
		return file.getParentPath();
	}
	public boolean copy(IFileStore destFileStore, IProgressMonitor monitor) {
		IHostFile destFile;
		try {
			OEFile oeFile = (OEFile)destFileStore;
			String parentPath = oeFile.getParentPath();
			destFile = fileService.createFile(parentPath, destFileStore.getName(), monitor);
			fileService.copy(file.getParentPath(), file.getName(), destFile.getParentPath(), destFile.getName(), monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	@Override
	public boolean exists() {
		return file.exists();
	}
	@Override
	public boolean canRead() {
		return file.canRead();
	}
	@Override
	public boolean canWrite() {
		return file.canWrite();
	}
	@Override
	public long getModifiedDate() {
		return file.getModifiedDate();
	}
	@Override
	public long getSize() {
		return file.getSize();
	}
	@Override
	public boolean isArchive() {
		return file.isArchive();
	}
	@Override
	public boolean isFile() {
		return file.isFile();
	}
	@Override
	public boolean isHidden() {
		return file.isHidden();
	}
	@Override
	public boolean isRoot() {
		return file.isRoot();
	}
	@Override
	public void renameTo(String newName) {
		file.renameTo(newName);
	}
	public URI getParentFile() {
		if (file.getParentPath().isEmpty())
			return null;
		try {
			return new URI(fileURI.getScheme(), fileURI.getHost(), file.getParentPath(), fileURI.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	public boolean delete(IProgressMonitor monitor) {
		try {
			fileService.delete(file.getParentPath(), file.getName(), monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * This method is called after a failure to modify a file or directory.
	 * Check to see if the parent is read-only and if so then
	 * throw an exception with a more specific message and error code.
	 *
	 * @param target The file that we failed to modify
	 * @param exception The low level exception that occurred, or <code>null</code>
	 * @throws CoreException A more specific exception if the parent is read-only
	 */
	private void checkReadOnlyParent() throws CoreException {
		String parent = file.getParentPath();
		String parentOfParent = parent.substring(0, parent.lastIndexOf("/"));
		IHostFile parentFile;
		try {
			parentFile = fileService.getFile(parentOfParent, parent, new NullProgressMonitor());
			if (parentFile == null || !parentFile.canRead() || !parentFile.canWrite()) {
				String message = NLS.bind(Messages.readOnlyParent, parent);
				Policy.error(EFS.ERROR_PARENT_READ_ONLY, message, null);
			}
		} catch (SystemMessageException e) {
			e.printStackTrace();
		}

	}

	public void mkdir(int options) {
		try {

			if (!file.isDirectory()) {
				file = fileService.createFolder(file.getParentPath(), file.getName(), new NullProgressMonitor());
				if (!file.isDirectory()) {
					checkReadOnlyParent();
					String message = NLS.bind(Messages.failedCreateWrongType, file.getAbsolutePath());
					Policy.error(EFS.ERROR_WRONG_TYPE, message);
				}
			}
		} catch (SystemMessageException e1) {
			e1.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

	}

	public String[] getChildNames(IProgressMonitor monitor) {
		if (file.isDirectory()) {
			IHostFile[] files;
			try {
				files = fileService.list(file.getAbsolutePath(), "*", IFileService.FILE_TYPE_FILES_AND_FOLDERS, monitor);
				ArrayList<String> names = new ArrayList<String>();

				for (IHostFile f : files) {
					names.add(f.getName());
				}

				String[] arrNames = new String[names.size()];
				names.toArray(arrNames);
				return arrNames;
			} catch (SystemMessageException e) {
				e.printStackTrace();
			}
		}
		return  new String[]{};
	}
	public IHost getConnection() {
		return projectInfo.getConnection();
	}

	public URI getChildURI(String name) {
		try {
			return new URI(fileURI.getScheme(), fileURI.getHost(), fileService.getFile(file.getAbsolutePath(), name, null).getAbsolutePath(), fileURI.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (SystemMessageException e) {
			e.printStackTrace();
		}
		return null;
	}
	public File toLocalFile() {
		//TODO
		//fileService.getFile(file.getParentPath(), file.getName(), null);
		return null;
	}
	public URI toURI() {
		return fileURI;
	}
	public YoctoHostFile getChildHostFile(String name) {
		try {
			return new YoctoHostFile(projectInfo, getChildURI(name), new NullProgressMonitor());
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	public URI getChildURIformPath(IPath path) {
		try {
			String fileName =  path.lastSegment();
			path = path.removeLastSegments(1);
			String newPath = fileService.getFile(file.getAbsolutePath() + "/" + path.toPortableString(), fileName, null).getAbsolutePath();
			return new URI(fileURI.getScheme(), fileURI.getHost(), newPath, fileURI.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void move(IFileStore destFile, IProgressMonitor monitor) {
		try {
			fileService.move(file.getParentPath(), file.getName(), destFile.getParent().toURI().getPath(), destFile.getName(), monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
		}
	}

	public OutputStream getOutputStream(int options, IProgressMonitor monitor) {
		try {
			return fileService.getOutputStream(file.getParentPath(), file.getName(), options, monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	public InputStream getInputStream(int options, IProgressMonitor monitor) {
		try {
			return fileService.getInputStream(file.getParentPath(), file.getName(), false, monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) {
		try {
			if ((options & EFS.SET_LAST_MODIFIED) != 0)
				fileService.setLastModified(file.getParentPath(), file.getName(), info.getLastModified(), monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
		}
	}

	public IFileService getFileService() {
		return fileService;
	}

	public void setFileService(IFileService fileService) {
		this.fileService = fileService;
	}
}

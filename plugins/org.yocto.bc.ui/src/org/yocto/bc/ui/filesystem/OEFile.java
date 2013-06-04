/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation, 2013 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ken Gilmer - adaptation from internal class.
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.ui.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.yocto.bc.bitbake.BBSession;
import org.yocto.bc.bitbake.ProjectInfoHelper;
import org.yocto.bc.bitbake.ShellSession;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.model.YoctoHostFile;

/**
 * File system implementation based on storage of files in the local
 * operating system's file system.
 */
public class OEFile extends FileStore {
	private static int attributes(File aFile) {
		if (!aFile.exists() || aFile.canWrite())
			return EFS.NONE;
		return EFS.ATTRIBUTE_READ_ONLY;
	}
	

	protected final YoctoHostFile file;

	private List<Object> ignoredPaths;

	/**
	 * The absolute file system path of the file represented by this store.
	 */
	protected final String filePath;

	private final URI root;

	/**
	 * Creates a new local file.
	 * 
	 * @param file The file this local file represents
	 * @param root 
	 */
	public OEFile(URI fileURI, List<Object> ignoredPaths, URI root, ProjectInfo projInfo, IProgressMonitor monitor) throws SystemMessageException {
		this.ignoredPaths = ignoredPaths;
		this.root = root;
		this.file = new YoctoHostFile(projInfo, fileURI, monitor);
		this.filePath = file.getAbsolutePath();
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
	private void checkReadOnlyParent(File target, Throwable exception) throws CoreException {
		File parent = target.getParentFile();
		if (parent != null && (attributes(parent) & EFS.ATTRIBUTE_READ_ONLY) != 0) {
			String message = NLS.bind(Messages.readOnlyParent, target.getAbsolutePath());
			Policy.error(EFS.ERROR_PARENT_READ_ONLY, message, exception);
		}
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) {
		return file.getChildNames(monitor);
	}

	/*
	 * detect if the path is potential builddir
	 */
	private boolean isPotentialBuildDir(String path) {
		String parentPath = path.substring(0, path.lastIndexOf("/"));
		String name = path.substring(path.lastIndexOf("/") + 1);
		boolean ret = true;
		try {
			IFileService fs = file.getFileService();
			IHostFile hostFile = fs.getFile(parentPath, name, new NullProgressMonitor());
			if (!hostFile.isDirectory())
				return false;
			IHostFile confDir = fs.getFile(path, BBSession.CONF_DIR, new NullProgressMonitor());
			if (!confDir.exists() || !confDir.isDirectory())
				return false;
			for (int i = 0; i < BBSession.BUILDDIR_INDICATORS.length && ret == true; i++) {
				IHostFile child = fs.getFile(path + "/" + BBSession.CONF_DIR, BBSession.BUILDDIR_INDICATORS[i], new NullProgressMonitor());
				if(!child.exists() || !child.isFile()) {
					ret = false;
					break;
				}
			}

		} catch (SystemMessageException e) {
			e.printStackTrace();
		}
		return ret;
	}

	/*
	 * try to find items for ignoreList
	 */
	private void updateIgnorePaths(String path, List<Object> list, IProgressMonitor monitor) {
		if(isPotentialBuildDir(path)) {
			BBSession config = null;
			try {
				config = Activator.getBBSession(Activator.getProjInfo(root), monitor);
				config.initialize();
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
			if (config.get("TMPDIR") == null || config.get("DL_DIR") == null || config.get("SSTATE_DIR") == null) {
				//wrong guess about the buildDir
				return;
			}else {
				if(!list.contains(config.get("TMPDIR"))) {
					list.add(config.get("TMPDIR"));
				}
				if(!list.contains(config.get("DL_DIR"))) {
					list.add(config.get("DL_DIR"));
				}
				if(!list.contains(config.get("SSTATE_DIR"))) {
					list.add(config.get("SSTATE_DIR"));
				}
			}
		}
	}

	@Override
	public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
		String[] children = childNames(options, monitor);
		IFileStore[] wrapped = new IFileStore[children.length];
		
		for (int i = 0; i < wrapped.length; i++) {
			String fullPath = file.toString() +File.separatorChar + children[i];
			
			updateIgnorePaths(fullPath, ignoredPaths, monitor);
			if (ignoredPaths.contains(fullPath)) {
				wrapped[i] = getDeadChild(children[i]);
			} else {
				wrapped[i] = getChild(children[i]);
			}			
		}
		
		return wrapped;
	}

	@Override
	public void copy(IFileStore destFileStore, int options, IProgressMonitor monitor) throws CoreException {
		if (destFileStore instanceof OEFile) {
			file.copy(destFileStore, monitor);
		}
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor = new NullProgressMonitor();
		try {
			monitor.beginTask(NLS.bind(Messages.deleting, this), 200);
			String message = Messages.deleteProblem;
			MultiStatus result = new MultiStatus(Policy.PI_FILE_SYSTEM, EFS.ERROR_DELETE, message, null);
			
			//don't allow Eclipse to delete entire OE directory
			
			if (!isProject()) {
				internalDelete(file, filePath, result, monitor);
			}
			
			if (!result.isOK())
				throw new CoreException(result);
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OEFile))
			return false;

		OEFile otherFile = (OEFile) obj;

		return file.equals(otherFile.file);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
		//in-lined non-native implementation
		FileInfo info = new FileInfo(file.getName());
		final long lastModified = file.getModifiedDate();
		if (lastModified <= 0) {
			//if the file doesn't exist, all other attributes should be default values
			info.setExists(false);
			return info;
		}
		info.setLastModified(lastModified);
		info.setExists(true);
		info.setLength(file.getSize());
		info.setDirectory(file.isDirectory());
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, file.exists() && !file.canWrite());
		info.setAttribute(EFS.ATTRIBUTE_HIDDEN, file.isHidden());
		return info;
	}
	
	@Override
	public IFileStore getChild(IPath path) {
		try {
			return new OEFile(file.getChildURIformPath(path), ignoredPaths, root, file.getProjectInfo(), new NullProgressMonitor());
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileStore getChild(String name) {

		try {
			return new OEFile(file.getChildURI(name), ignoredPaths, root, file.getProjectInfo(), new NullProgressMonitor());
		} catch (SystemMessageException e) {
			e.printStackTrace();
		}
		return null;

	}

	private IFileStore getDeadChild(String name) {
		return new OEIgnoreFile(file.getChildHostFile(name));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#getFileSystem()
	 */
	@Override
	public IFileSystem getFileSystem() {
		return OEFileSystem.getInstance();
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public IFileStore getParent() {
		URI parentURI = file.getParentFile();
		try {
			return parentURI == null ? null : new OEFile(parentURI, ignoredPaths, root, file.getProjectInfo(), new NullProgressMonitor());
		} catch (SystemMessageException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	/**
	 * Deletes the given file recursively, adding failure info to
	 * the provided status object.  The filePath is passed as a parameter
	 * to optimize java.io.File object creation.
	 */
	private boolean internalDelete(YoctoHostFile target, String pathToDelete, MultiStatus status, IProgressMonitor monitor) {
		target.delete(monitor);
		return false;
	}

	@Override
	public boolean isParentOf(IFileStore other) {
		if (!(other instanceof OEFile))
			return false;
		String thisPath = filePath;
		String thatPath = ((OEFile) other).filePath;
		int thisLength = thisPath.length();
		int thatLength = thatPath.length();
		//if equal then not a parent
		if (thisLength >= thatLength)
			return false;
		if (getFileSystem().isCaseSensitive()) {
			if (thatPath.indexOf(thisPath) != 0)
				return false;
		} else {
			if (thatPath.toLowerCase().indexOf(thisPath.toLowerCase()) != 0)
				return false;
		}
		//The common portion must end with a separator character for this to be a parent of that
		return thisPath.charAt(thisLength - 1) == File.separatorChar || thatPath.charAt(thisLength) == File.separatorChar;
	}

	/**
	 * @return
	 */
	private boolean isProject() {
		return this.file.toString().equals(root);
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		file.mkdir(options);
		return this;
	}

	@Override
	public void move(IFileStore destFile, int options, IProgressMonitor monitor) throws CoreException {
		file.move(destFile, monitor);
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		return file.getInputStream(options, monitor);
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		return file.getOutputStream(options, monitor);
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		file.putInfo(info, options, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.provider.FileStore#toLocalFile(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public File toLocalFile(int options, IProgressMonitor monitor) throws CoreException {
		return file.toLocalFile();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#toString()
	 */
	@Override
	public String toString() {
		return file.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.filesystem.IFileStore#toURI()
	 */
	@Override
	public URI toURI() {
		return URIUtil.toURI(filePath);
	}
	public String getParentPath() {
		return filePath.substring(0, filePath.lastIndexOf("/"));
	}
}

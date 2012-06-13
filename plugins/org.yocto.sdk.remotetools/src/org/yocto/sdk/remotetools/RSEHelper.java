/********************************************************************************
 * Copyright (c) 2009, 2010 MontaVista Software, Inc and Others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Anna Dushistova (MontaVista) - initial API and implementation
 * Lianhao Lu (Intel)			- Modified to add other file operations.
 ********************************************************************************/
package org.yocto.sdk.remotetools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.IRSECoreStatusCodes;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISubSystemConfigurationCategories;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.IShellServiceSubSystem;
import org.eclipse.rse.services.IService;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.shells.HostShellProcessAdapter;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.shells.IShellService;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.IFileServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;

public class RSEHelper {
	
	public static IHost getRemoteConnectionByName(String remoteConnection) {
		if (remoteConnection == null)
			return null;
		IHost[] connections = RSECorePlugin.getTheSystemRegistry().getHosts();
		for (int i = 0; i < connections.length; i++)
			if (connections[i].getAliasName().equals(remoteConnection))
				return connections[i];
		return null; // TODO Connection is not found in the list--need to react
		// somehow, throw the exception?

	}
	
	public static String getRemoteHostName(String remoteConnection)
	{
		final IHost host=getRemoteConnectionByName(remoteConnection);
		if(host == null)
			return null;
		else
			return host.getHostName();
	}

	public static IService getConnectedRemoteFileService(
			IHost currentConnection, IProgressMonitor monitor) throws Exception {
		final ISubSystem subsystem = getFileSubsystem(currentConnection);

		if (subsystem == null)
			throw new Exception(Messages.ErrorNoSubsystem);

		try {
			subsystem.connect(monitor, false);
		} catch (CoreException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		if (!subsystem.isConnected())
			throw new Exception(Messages.ErrorConnectSubsystem);

		return ((IFileServiceSubSystem) subsystem).getFileService();
	}

	public static ISubSystem getFileSubsystem(IHost host) {
		if (host == null)
			return null;
		ISubSystem[] subSystems = host.getSubSystems();
		for (int i = 0; i < subSystems.length; i++) {
			if (subSystems[i] instanceof IFileServiceSubSystem)
				return subSystems[i];
		}
		return null;
	}
	
	public static IService getConnectedShellService(
			IHost currentConnection, IProgressMonitor monitor) throws Exception {
		final ISubSystem subsystem = getShellSubsystem(currentConnection);

		if (subsystem == null)
			throw new Exception(Messages.ErrorNoSubsystem);

		try {
			subsystem.connect(monitor, false);
		} catch (CoreException e) {
			throw e;
		} catch (OperationCanceledException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}

		if (!subsystem.isConnected())
			throw new Exception(Messages.ErrorConnectSubsystem);

		return ((IShellServiceSubSystem) subsystem).getShellService();
	}
	
	public static ISubSystem getShellSubsystem(IHost host) {
		if (host == null)
			return null;
		ISubSystem[] subSystems = host.getSubSystems();
		for (int i = 0; i < subSystems.length; i++) {
			if (subSystems[i] instanceof IShellServiceSubSystem)
				return subSystems[i];
		}
		return null;
	}

	public static IHost[] getSuitableConnections() {
		
		//we only get RSE connections with files&cmds subsystem
		ArrayList <IHost> filConnections = new ArrayList <IHost>(Arrays.asList(RSECorePlugin.getTheSystemRegistry()
				.getHostsBySubSystemConfigurationCategory(ISubSystemConfigurationCategories.SUBSYSTEM_CATEGORY_FILES))); //$NON-NLS-1$
		
		ArrayList <IHost> terminalConnections = new ArrayList <IHost>(Arrays.asList(RSECorePlugin.getTheSystemRegistry()
				.getHostsBySubSystemConfigurationCategory("terminal")));//$NON-NLS-1$
		
		ArrayList shellConnections = new ArrayList(Arrays.asList(RSECorePlugin.getTheSystemRegistry()
				.getHostsBySubSystemConfigurationCategory("shells"))); //$NON-NLS-1$

		Iterator <IHost>iter = filConnections.iterator();
		while(iter.hasNext()){
			IHost fileConnection = iter.next();
			if(!terminalConnections.contains(fileConnection) && !shellConnections.contains(fileConnection)){
				iter.remove();
			}
		}
		
		return (IHost[]) filConnections.toArray(new IHost[filConnections.size()]);
	}
	
	public static void deleteRemoteFile(IHost connection, String remoteExePath,
			IProgressMonitor monitor) throws Exception {
		
		assert(connection!=null);
		monitor.beginTask(Messages.InfoUpload, 100);
		
		IFileService fileService;
		try {
			fileService = (IFileService) getConnectedRemoteFileService(
							connection,
							new SubProgressMonitor(monitor, 5));
	
			Path remotePath = new Path(remoteExePath);
			if(fileService.getFile(remotePath.removeLastSegments(1).toString(), 
					remotePath.lastSegment(), 
					new SubProgressMonitor(monitor, 5)).exists()) {
				fileService.delete(remotePath.removeLastSegments(1).toString(), 
						remotePath.lastSegment(), 
						new SubProgressMonitor(monitor, 10));
			}
		} finally {
			monitor.done();
		}
		return;
	}
	
	public static void putRemoteFile(IHost connection, String localExePath, String remoteExePath,
			IProgressMonitor monitor) throws Exception {
		
		assert(connection!=null);
		monitor.beginTask(Messages.InfoUpload, 100);
		
		IFileService fileService;
		try {
			fileService = (IFileService) getConnectedRemoteFileService(
							connection,
							new SubProgressMonitor(monitor, 5));
			File file = new File(localExePath);
			Path remotePath = new Path(remoteExePath);
			if(fileService.getFile(remotePath.removeLastSegments(1).toString(), 
					remotePath.lastSegment(), 
					new SubProgressMonitor(monitor, 5)).exists()) {
				fileService.delete(remotePath.removeLastSegments(1).toString(), 
						remotePath.lastSegment(), 
						new SubProgressMonitor(monitor, 10));
			}
			fileService.upload(file, remotePath.removeLastSegments(1)
					.toString(), remotePath.lastSegment(), true, null, null,
					new SubProgressMonitor(monitor, 80));
			// Need to change the permissions to match the original file
			// permissions because of a bug in upload
			//RemoteApplication p = remoteShellExec(
			//		config,
			//		"", "chmod", "+x " + spaceEscapify(remotePath.toString()), new SubProgressMonitor(monitor, 5)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//Thread.sleep(500);
			//p.destroy();
			
		} finally {
			monitor.done();
		}
		return;
	}
	
	public static void putRemoteFileInPlugin(IHost connection, String locaPathInPlugin, String remoteExePath,
			IProgressMonitor monitor) throws Exception {
		
		assert(connection!=null);
		monitor.beginTask(Messages.InfoUpload, 100);
		
		IFileService fileService;
		try {
			fileService = (IFileService) getConnectedRemoteFileService(
							connection,
							new SubProgressMonitor(monitor, 5));
			InputStream  inputStream = FileLocator.openStream(
				    Activator.getDefault().getBundle(), new Path(locaPathInPlugin), false);
			Path remotePath = new Path(remoteExePath);
			/*
			if(!fileService.getFile(remotePath.removeLastSegments(1).toString(), 
					remotePath.lastSegment(), 
					new SubProgressMonitor(monitor, 5)).exists()) {
			}
			*/
			/*
			fileService.upload(inputStream, remotePath.removeLastSegments(1)
					.toString(), remotePath.lastSegment(), true, null,
					new SubProgressMonitor(monitor, 80));
					*/
			//TODO workaround for now
			//in case the underlying scp file service doesn't support inputStream upload
			BufferedInputStream bis = new BufferedInputStream(inputStream);
			File tempFile = File.createTempFile("scp", "temp"); //$NON-NLS-1$ //$NON-NLS-2$
			FileOutputStream os = new FileOutputStream(tempFile);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			byte[] buffer = new byte[1024];
			int readCount;
			while( (readCount = bis.read(buffer)) > 0)
			{
				bos.write(buffer, 0, readCount);
			}
			bos.close();
			fileService.upload(tempFile, remotePath.removeLastSegments(1)
					.toString(), remotePath.lastSegment(), true, null, null,
					new SubProgressMonitor(monitor, 80));
			// Need to change the permissions to match the original file
			// permissions because of a bug in upload
			remoteShellExec(
					connection,
					"", "chmod", "+x " + spaceEscapify(remotePath.toString()), new SubProgressMonitor(monitor, 5)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
		} finally {
			monitor.done();
		}
		return;
	}
	
	public static void getRemoteFile(IHost connection, String localExePath, String remoteExePath,
			IProgressMonitor monitor) throws Exception {
		
		assert(connection!=null);
		monitor.beginTask(Messages.InfoDownload, 100);
		
		IFileService fileService;
		try {
			fileService = (IFileService) getConnectedRemoteFileService(
							connection,
							new SubProgressMonitor(monitor, 10));
			File file = new File(localExePath);
			file.deleteOnExit();
			monitor.worked(5);
			Path remotePath = new Path(remoteExePath);
			fileService.download(remotePath.removeLastSegments(1).toString(), 
					remotePath.lastSegment(),file,true, null,
					new SubProgressMonitor(monitor, 85));
			// Need to change the permissions to match the original file
			// permissions because of a bug in upload
			//RemoteApplication p = remoteShellExec(
			//		config,
			//		"", "chmod", "+x " + spaceEscapify(remotePath.toString()), new SubProgressMonitor(monitor, 5)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			//Thread.sleep(500);
			//p.destroy();
			
		} finally {
			monitor.done();
		}
		return;
	}
	
	public static ITerminalServiceSubSystem getTerminalSubSystem(
            IHost connection) {
        ISystemRegistry systemRegistry = RSECorePlugin.getTheSystemRegistry();
        ISubSystem[] subsystems = systemRegistry.getSubSystems(connection);
        for (int i = 0; i < subsystems.length; i++) {
        	if (subsystems[i] instanceof ITerminalServiceSubSystem) {
                ITerminalServiceSubSystem subSystem = (ITerminalServiceSubSystem) subsystems[i];
                return subSystem;
            }
        }
        return null;
    }
	
	public static String spaceEscapify(String inputString) {
		if (inputString == null)
			return null;

		return inputString.replaceAll(" ", "\\\\ "); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private final static String EXIT_CMD = "exit"; //$NON-NLS-1$
	private final static String CMD_DELIMITER = ";"; //$NON-NLS-1$
	
	public static Process remoteShellExec(IHost connection,
			String prelaunchCmd, String remoteCommandPath, String arguments,
			IProgressMonitor monitor) throws CoreException {
		
		monitor.beginTask(NLS.bind(Messages.RemoteShellExec_1,
				remoteCommandPath, arguments), 10);
		String realRemoteCommand = arguments == null ? spaceEscapify(remoteCommandPath)
				: spaceEscapify(remoteCommandPath) + " " + arguments; //$NON-NLS-1$

		String remoteCommand = realRemoteCommand + CMD_DELIMITER + EXIT_CMD;

		if(prelaunchCmd != null) {
			if (!prelaunchCmd.trim().equals("")) //$NON-NLS-1$
				remoteCommand = prelaunchCmd + CMD_DELIMITER + remoteCommand;
		}

		IShellService shellService;
		Process p = null;
		try {
			shellService = (IShellService) getConnectedShellService(
							connection,
							new SubProgressMonitor(monitor, 7));

			// This is necessary because runCommand does not actually run the
			// command right now.
			String env[] = new String[0];
			try {
				IHostShell hostShell = shellService.launchShell(
						"", env, new SubProgressMonitor(monitor, 3)); //$NON-NLS-1$
				hostShell.writeToShell(remoteCommand);
				p = new HostShellProcessAdapter(hostShell);
			} catch (Exception e) {
				if (p != null) {
					p.destroy();
				}
				abort(Messages.RemoteShellExec_2, e,
						IRSECoreStatusCodes.EXCEPTION_OCCURRED);
			}
		} catch (Exception e1) {
			abort(e1.getMessage(), e1,
					IRSECoreStatusCodes.EXCEPTION_OCCURRED);
		}

		monitor.done();
		return p;
	}
	
	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 */
	public static void abort(String message, Throwable exception, int code) throws CoreException {
		IStatus status;
		if (exception != null) {
			MultiStatus multiStatus = new MultiStatus(Activator.PLUGIN_ID, code, message, exception);
			multiStatus.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID, code, exception.getLocalizedMessage(), exception));
			status= multiStatus;
		} else {
			status= new Status(IStatus.ERROR, Activator.PLUGIN_ID, code, message, null);
		}
		throw new CoreException(status);
	}
}

/********************************************************************************
 * Copyright (c) 2013 MontaVista Software, Inc and Others.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Anna Dushistova (MontaVista) - initial API and implementation
 * Lianhao Lu (Intel)			- Modified to add other file operations.
 * Ioana Grigoropol (Intel)     - Separated remote functionality
 ********************************************************************************/
package org.yocto.remote.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.filesystem.InternalFileSystemCore;
import org.eclipse.core.internal.resources.LocalMetaArea;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.IRSECoreRegistry;
import org.eclipse.rse.core.IRSECoreStatusCodes;
import org.eclipse.rse.core.IRSEInitListener;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.IRSEUserIdConstants;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISubSystemConfigurationCategories;
import org.eclipse.rse.core.model.ISubSystemConfigurator;
import org.eclipse.rse.core.model.ISystemHostPool;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.internal.core.RSEInitJob;
import org.eclipse.rse.internal.ui.view.SystemPerspectiveHelpers;
import org.eclipse.rse.services.IService;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.IFileService;
import org.eclipse.rse.services.files.IHostFile;
import org.eclipse.rse.services.shells.HostShellProcessAdapter;
import org.eclipse.rse.services.shells.IHostShell;
import org.eclipse.rse.services.shells.IShellService;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.FileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.IFileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.subsystems.shells.core.subsystems.servicesubsystem.IShellServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.ui.console.MessageConsole;

@SuppressWarnings("restriction")
public class RemoteHelper {
	private final static String EXIT_CMD = "exit"; //$NON-NLS-1$
	private final static String CMD_DELIMITER = ";"; //$NON-NLS-1$
	private final static String LOCAL_CONN_NAME = "Local";
	private final static String CONNECTION_NAME = "127.0.0.1";
	private final static String HOST_NAME = "LOCALHOST";
	public static final String TERMINATOR = "234o987dsfkcqiuwey18837032843259d";//$NON-NLS-1$
	public static final int TOTALWORKLOAD = 100;
	private static Map<IHost, RemoteMachine> machines;

	public static IPath getWorkspaceMetaArea(){
		Workspace workspace = (Workspace)ResourcesPlugin.getWorkspace();
		LocalMetaArea metaDataArea = workspace.getMetaArea();
		return metaDataArea.getLocation();
	}
	public static void storeProjDescrInMetaArea(IHost conn, String projName, String remoteSrc){
		IPath path = getWorkspaceMetaArea();
		String sep = File.separator;
		String localDest = path.toString() + sep + ".projects" + sep + projName + sep + ".project";
		try {
			getRemoteFile(conn, localDest, remoteSrc + sep + ".project", new NullProgressMonitor());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String retrieveProjRootFromMetaArea(String projName){
		IPath path = getWorkspaceMetaArea();
		String sep = File.separator;
		return path.toString() + sep + ".projects" + sep + projName + sep;
	}
	public static void storeURIInMetaArea(String projName, URI uri){
		IPath path = getWorkspaceMetaArea();
		String sep = File.separator;
		File f = new File(path.toString() + sep + ".projects" + sep + projName + sep + ".originalURI");
		PrintWriter writer;
		try {
			writer = new PrintWriter(f);
			writer.println(uri.getScheme());
			writer.println(uri.getHost());
			writer.println(uri.getPath());
			writer.println(uri.getFragment());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static URI retrieveURIFromMetaArea(String projName){
		IPath path = getWorkspaceMetaArea();
		String sep = File.separator;
		File f = new File(path.toString() + sep + ".projects" + sep + projName + sep + ".originalURI");
		try {
			BufferedReader buf = new BufferedReader(new FileReader(f));
			String line = null;
			List<String> elems = new ArrayList<String>();
			while((line = buf.readLine()) != null){
				if (line.equals("null"))
					line = null;
				elems.add(line);
			}
			buf.close();
			if (elems.size() == 4){
				URI uri = new URI(elems.get(0), elems.get(1), elems.get(2), elems.get(3));
				return uri;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isInitialized(final URI uri){
		boolean init = RSECorePlugin.isInitComplete(RSECorePlugin.INIT_MODEL);
		if (!init) {
			RSEInitJob.getInstance().addInitListener(new IRSEInitListener() {
				@Override
				public void phaseComplete(int arg0) {
					try {
						InternalFileSystemCore.getInstance().getStore(uri);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			});
		}
		return init;
	}

	public static IHost getRemoteConnectionByName(String remoteConnection) {
		if (remoteConnection == null)
			return null;
		IHost[] connections = RSECorePlugin.getTheSystemRegistry().getHosts();
		for (int i = 0; i < connections.length; i++)
			if (connections[i].getAliasName().equals(remoteConnection))
				return connections[i];
		return null;
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

		ArrayList <IHost> shellConnections = new ArrayList <IHost>(Arrays.asList(RSECorePlugin.getTheSystemRegistry()
				.getHostsBySubSystemConfigurationCategory("shells"))); //$NON-NLS-1$

		Iterator <IHost>iter = filConnections.iterator();
		while(iter.hasNext()){
			IHost fileConnection = iter.next();
			if(!terminalConnections.contains(fileConnection) && !shellConnections.contains(fileConnection)){
				iter.remove();
			}
			IRSESystemType sysType = fileConnection.getSystemType();
			if (sysType == null || !sysType.isEnabled()) {
				iter.remove();
			}
		}

		return filConnections.toArray(new IHost[filConnections.size()]);
	}

	public static void putRemoteFileInPlugin(IHost connection, String locaPathInPlugin, String remoteExePath,
			IProgressMonitor monitor) throws Exception {

		assert(connection != null);
		monitor.beginTask(Messages.InfoUpload, 100);

		IFileService fileService;
		try {
			fileService = getConnectedRemoteFileService(
							connection,
							SubMonitor.convert(monitor, 5));
			InputStream  inputStream = FileLocator.openStream(
				    Activator.getDefault().getBundle(), new Path(locaPathInPlugin), false);
			Path remotePath = new Path(remoteExePath);

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
					SubMonitor.convert(monitor, 80));
			// Need to change the permissions to match the original file
			// permissions because of a bug in upload
			remoteShellExec(
					connection,
					"", "chmod", "+x " + spaceEscapify(remotePath.toString()), SubMonitor.convert(monitor, 5)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
			fileService = getConnectedRemoteFileService(
							connection,
							SubMonitor.convert(monitor, 10));
			File file = new File(localExePath);
			monitor.worked(5);
			Path remotePath = new Path(remoteExePath);
			fileService.download(remotePath.removeLastSegments(1).toString(),
					remotePath.lastSegment(),file,true, null,
					SubMonitor.convert(monitor, 85));
			// Need to change the permissions to match the original file
			// permissions because of a bug in upload
			//RemoteApplication p = remoteShellExec(
			//		config,
			//		"", "chmod", "+x " + spaceEscapify(remotePath.toString()), SubMonitor.convert(monitor, 5)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
							SubMonitor.convert(monitor, 7));

			// This is necessary because runCommand does not actually run the
			// command right now.
			String env[] = new String[0];
			try {
				IHostShell hostShell = shellService.launchShell(
						"", env, SubMonitor.convert(monitor, 3)); //$NON-NLS-1$
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
			status = multiStatus;
		} else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, code, message, null);
		}
		throw new CoreException(status);
	}
	/**
	 * Checks whether a IHost associated system's is enabled and not a local one
	 * @param host
	 * @return
	 */
	public static boolean isHostViable(IHost host) {
		IRSESystemType sysType = host.getSystemType();
		if (sysType != null && sysType.isEnabled() && !sysType.isLocal())
			return true;
		return false;
	}

	/**
	 * Ensures that RSECorePlugin is initialized before performing any actions
	 */
	public static void waitForRSEInitCompletition() {
		if (!RSECorePlugin.isInitComplete(RSECorePlugin.INIT_MODEL))
			try {
				RSECorePlugin.waitForInitCompletion(RSECorePlugin.INIT_MODEL);
			} catch (InterruptedException e) {
				return;
			}
	}
	public static RemoteMachine getRemoteMachine(IHost connection){
		if (!getMachines().containsKey(connection))
			getMachines().put(connection, new RemoteMachine(connection));
		return getMachines().get(connection);
	}

	private static Map<IHost, RemoteMachine> getMachines() {
		if (machines == null)
			machines = new HashMap<IHost, RemoteMachine>();
		return machines;
	}

	public static MessageConsole getConsole(IHost connection) {
		return getRemoteMachine(connection).getConsole();
	}

	public static CommandResponseHandler getCommandHandler(IHost connection) {
		return getRemoteMachine(connection).getCmdHandler();
	}

	public static ProcessStreamBuffer processOutput(IProgressMonitor monitor, IHostShell hostShell, CommandResponseHandler cmdHandler) throws Exception {
		return new CommandOutputProcessor(monitor, hostShell, cmdHandler, "").processOutput();
	}

	public static IHost getRemoteConnectionForURI(URI uri, IProgressMonitor monitor) {
		if (uri == null)
			return null;

		String host = uri.getHost();
		if (host == null) {
			// this is a local connection
			ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
			IHost local = null;
			while (local == null) {
				local = sr.getLocalHost();
			}
			return local;
		}
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		IHost[] connections = sr.getHosts();

		IHost unconnected = null;
		for (IHost conn : connections) {
			if (host.equalsIgnoreCase(conn.getHostName())) {
				IRemoteFileSubSystem fss = getRemoteFileSubSystem(conn);
				if (fss != null && fss.isConnected())
					return conn;
				unconnected = conn;
			}
		}

		return unconnected;
	}

	public static IRemoteFileSubSystem getRemoteFileSubSystem(IHost host) {
		IRemoteFileSubSystem candidate = null;
		IRemoteFileSubSystem otherServiceCandidate = null;
		IRemoteFileSubSystem[] subSystems = RemoteFileUtility.getFileSubSystems(host);

		for (IRemoteFileSubSystem subSystem : subSystems) {
			if (subSystem instanceof FileServiceSubSystem) {
				if (subSystem.isConnected())
					return subSystem;

				if (otherServiceCandidate == null)
					otherServiceCandidate = subSystem;

			} else if (candidate == null || (subSystem.isConnected() && !candidate.isConnected()))
				candidate = subSystem;

		}
		if (candidate != null && candidate.isConnected())
			return candidate;
		if (otherServiceCandidate != null)
			return otherServiceCandidate;
		return null;
	}

	public static IFileService getConnectedRemoteFileService(IHost connection, IProgressMonitor monitor) throws Exception {
		return getRemoteMachine(connection).getRemoteFileService(monitor);
	}

	public static IHostFile[] getRemoteDirContent(IHost connection, String remoteParent, String fileFilter, int fileType, IProgressMonitor monitor){

		try {
			IFileService fileServ = getConnectedRemoteFileService(connection, monitor);
			return fileServ.list(remoteParent, fileFilter, fileType, monitor);
		} catch (SystemMessageException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static IService getConnectedShellService(IHost connection, IProgressMonitor monitor) throws Exception {
		return getRemoteMachine(connection).getShellService(monitor);
	}

	public static void handleRunCommandRemote(IHost connection, YoctoCommand cmd, IProgressMonitor monitor){
		try {
			CommandRunnable cmdRun = new CommandRunnable(connection, cmd, monitor);
			cmdRun.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static IHostShell runCommandRemote(IHost connection, YoctoCommand cmd,
			IProgressMonitor monitor) throws CoreException {

		monitor.beginTask(NLS.bind(Messages.RemoteShellExec_1,
				cmd, cmd.getArguments()), 10);

		String remoteCommand = cmd.getCommand() + " " + cmd.getArguments() + " ; echo " + TERMINATOR + "; exit ;";

		IShellService shellService;
		try {
			shellService = (IShellService) getConnectedShellService(connection, SubMonitor.convert(monitor, 7));

			String env[] = getRemoteMachine(connection).prepareEnvString(monitor);

			try {
				IHostShell hostShell = shellService.runCommand(cmd.getInitialDirectory(), remoteCommand, env, SubMonitor.convert(monitor, 3));
				return hostShell;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public static IHostFile getRemoteHostFile(IHost connection, String remoteFilePath, IProgressMonitor monitor){
		assert(connection != null);
		monitor.beginTask(Messages.InfoDownload, 100);

		try {
			IFileService fileService = getConnectedRemoteFileService(connection, SubMonitor.convert(monitor, 10));
			Path remotePath = new Path(remoteFilePath);
			IHostFile remoteFile = fileService.getFile(remotePath.removeLastSegments(1).toString(), remotePath.lastSegment(), SubMonitor.convert(monitor, 5));
			return remoteFile;
		} catch (Exception e) {
			e.printStackTrace();
	    }finally {
			monitor.done();
		}
		return null;
	}

	public static InputStream getRemoteInputStream(IHost connection, String parentPath, String remoteFilePath, IProgressMonitor monitor){
		assert(connection != null);
		monitor.beginTask(Messages.InfoDownload, 100);

		try {
			IFileService fileService = getConnectedRemoteFileService(connection, SubMonitor.convert(monitor, 10));

			return fileService.getInputStream(parentPath, remoteFilePath, false, monitor);
		} catch (Exception e) {
			e.printStackTrace();
	    }finally {
			monitor.done();
		}
		return null;
	}

	public static URI createNewURI(URI oldURI, String name) {
		try {
			String sep = oldURI.getPath().endsWith("/") ? "" : "/";
			return new URI(oldURI.getScheme(), oldURI.getHost(), oldURI.getPath() + sep + name, oldURI.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean fileExistsRemote(IHost conn, IProgressMonitor monitor, String path) {
		try {
			IFileService fs = getConnectedRemoteFileService(conn, monitor);
			int nameStart = path.lastIndexOf("/");
			String parentPath = path.substring(0, nameStart);
			String name = path.substring(nameStart + 1);
			IHostFile hostFile = fs.getFile(parentPath, name, monitor);

			return hostFile.exists();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static IHost createLocalConnection() {
		IHost createdHost = null;
		try {
			
			ISubSystemConfigurator[] configurators = getSubSystemConfigurators();
			
			IRSESystemType systemType = getSSHSystemType();
			ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
			IHost[] connections = sr.getHosts();
			for (IHost conn : connections) {
				if (conn.getName().equals(LOCAL_CONN_NAME)) {
					ISystemHostPool pool = conn.getHostPool();
					createdHost = sr.createHost(pool.getSystemProfile().getName(), systemType, CONNECTION_NAME, HOST_NAME, "", "",IRSEUserIdConstants.USERID_LOCATION_NOTSET,configurators);
					break;
				}
			}

			// a tweak that is the result of UCD feedback. Phil
			if ((createdHost != null) && SystemPerspectiveHelpers.isRSEPerspectiveActive()) {
				if (systemType.getId().equals(IRSESystemType.SYSTEMTYPE_ISERIES_ID)) {
					ISubSystem[] objSubSystems = sr.getSubSystemsBySubSystemConfigurationCategory("nativefiles", createdHost); //$NON-NLS-1$
					if ((objSubSystems != null) && (objSubSystems.length > 0))// might be in product that doesn't have iSeries plugins
						RSEUIPlugin.getTheSystemRegistryUI().expandSubSystem(objSubSystems[0]);
					else
						RSEUIPlugin.getTheSystemRegistryUI().expandHost(createdHost);
				} else
					RSEUIPlugin.getTheSystemRegistryUI().expandHost(createdHost);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return createdHost;
	}
	
	private static IRSESystemType getSSHSystemType() {
		IRSECoreRegistry coreReg = RSECorePlugin.getTheCoreRegistry();
		return coreReg.getSystemTypeById("org.eclipse.rse.systemtype.ssh");
	}
	
	private static ISubSystemConfigurator[] getSubSystemConfigurators()
	{		
		// what kind of subsystems do we have here?
		ISystemRegistry sr = RSECorePlugin.getTheSystemRegistry();
		ISubSystemConfiguration[] configurations = sr.getSubSystemConfigurationsBySystemType(getSSHSystemType(), true);
		
		ArrayList<ISubSystemConfigurator> configList = new ArrayList<ISubSystemConfigurator>();
		for (int i = 0; i < configurations.length; i++){
			ISubSystemConfiguration configuration = configurations[i];
			
			class DefaultConfigurator implements ISubSystemConfigurator {
				private ISubSystemConfiguration _configuration;
				public DefaultConfigurator(ISubSystemConfiguration configuration){
					_configuration = configuration;
				}
					
				public boolean applyValues(ISubSystem ss) {
					return true;
				}

				public ISubSystemConfiguration getSubSystemConfiguration() {
					return _configuration;
				}						
			}
			configList.add(new DefaultConfigurator(configuration));
		}				
			
		return (ISubSystemConfigurator[])configList.toArray(new ISubSystemConfigurator[configList.size()]);

	}
}

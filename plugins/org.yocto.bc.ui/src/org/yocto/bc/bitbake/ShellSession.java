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
package org.yocto.bc.bitbake;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
	
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.files.IHostFile;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.remote.utils.ICommandResponseHandler;
import org.yocto.remote.utils.RemoteHelper;
import org.yocto.remote.utils.YoctoCommand;

/**
 * A class for Linux shell sessions.
 * @author kgilmer
 *
 */
public class ShellSession {
	private volatile boolean interrupt = false;
	/**
	 * String used to isolate command execution
	 */
	public static final String TERMINATOR = "#234o987dsfkcqiuwey18837032843259d";
	public static final String LT = System.getProperty("line.separator");
	public static final String exportCmd = "export BB_ENV_EXTRAWHITE=\\\"DISABLE_SANITY_CHECKS $BB_ENV_EXTRAWHITE\\\"";
	public static final String exportColumnsCmd = "export COLUMNS=1000";
	private static final String BUILD_DIR = "/build/";

	
	public static String getFilePath(String file) throws IOException {
		File f = new File(file);
		
		if (!f.exists() || f.isDirectory()) {
			throw new IOException("Path passed is not a file: " + file);
		}
		
		StringBuffer sb = new StringBuffer();
		
		String elems[] = file.split("//");
		
		for (int i = 0; i < elems.length - 1; ++i) {
			sb.append(elems[i]);
			sb.append("//");
		}
		
		return sb.toString();
	}
	private Process process;

	private OutputStream pos = null;
	private String shellPath = null;
	private final String initCmd;
	private final IHostFile root;
	private ProjectInfo projectInfo;

	public ProjectInfo getProjectInfo() {
		return projectInfo;
	}

	public void setProjectInfo(ProjectInfo projectInfo) {
		this.projectInfo = projectInfo;
	}

	public ShellSession(ProjectInfo pInfo, IHostFile root, String initCmd) throws IOException {
		this.projectInfo = pInfo;
		this.root = root;
		this.initCmd  = initCmd;

		initializeShell(new NullProgressMonitor());
	}

	private void initializeShell(IProgressMonitor monitor) throws IOException {
		try {
			if (root != null) {
				IHost connection = projectInfo.getConnection();
				RemoteHelper.handleRunCommandRemote(connection, new YoctoCommand("source " + initCmd, root.getAbsolutePath(), ""), monitor);
				RemoteHelper.handleRunCommandRemote(connection,  new YoctoCommand(exportCmd, root.getAbsolutePath(), ""), monitor);
			} else {
				throw new Exception("Root file not found!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	synchronized 
	public String execute(String command) throws IOException {
		return execute(command, false);
	}

	synchronized 
	public String execute(String command, boolean hasErrors) throws IOException {
		try {
			if (projectInfo.getConnection() != null) {
				command = getInitCmd() + command;
				RemoteHelper.handleRunCommandRemote(projectInfo.getConnection(), new YoctoCommand(command, getBuildDirAbsolutePath(), ""), new NullProgressMonitor());
				return getBuildDirAbsolutePath();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getBuildDirAbsolutePath() {
		return root.getAbsolutePath() + BUILD_DIR;
	}

	private String getInitCmd() {
		return "source " + initCmd + " " + getBuildDirAbsolutePath()
				+ " > tempsf; rm -rf tempsf;" + exportCmd + ";"
				+ exportColumnsCmd + ";" + "cd " + getBuildDirAbsolutePath()
				+ ";";
	}
	
	synchronized 
	public void execute(String command, String terminator, ICommandResponseHandler handler) throws IOException {
		interrupt = false;
		InputStream errIs = process.getErrorStream();
		if (errIs.available() > 0) {
			clearErrorStream(errIs);
		}
		sendToProcessAndTerminate(command);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String std = null;

		do {		
			if (errIs.available() > 0) {
				byte[] msg = new byte[errIs.available()];

				errIs.read(msg, 0, msg.length);
				handler.response(new String(msg), true);
			} 
			
			std = br.readLine();
			
			if (std != null && !std.endsWith(terminator)) {
				handler.response(std, false);
			} 
			
		} while (std != null && !std.endsWith(terminator) && !interrupt);
		
		if (interrupt) {
			process.destroy();
			initializeShell(null);
			interrupt = false;
		}
	}
	
	private void clearErrorStream(InputStream is) {
	
		try {
			byte b[] = new byte[is.available()];
			is.read(b);			
			System.out.println("clearing: " + new String(b));
		} catch (IOException e) {
			e.printStackTrace();
			//Ignore any error
		}
	}

	/**
	 * Send command string to shell process and add special terminator string so
	 * reader knows when output is complete.
	 * 
	 * @param command
	 * @throws IOException
	 */
	private void sendToProcessAndTerminate(String command) throws IOException {
		pos.write(command.getBytes());
		pos.write(LT.getBytes());
		pos.flush();
		pos.write("echo $?".getBytes());
		pos.write(TERMINATOR.getBytes());
		pos.write(LT.getBytes());
		pos.flush();
	}

	/**
	 * Interrupt any running processes.
	 */
	public void interrupt() {
		interrupt = true;
	}
	
	public void printError(String errorLines) {
		RemoteHelper.getCommandHandler(projectInfo.getConnection()).response(errorLines, true);
	}
}

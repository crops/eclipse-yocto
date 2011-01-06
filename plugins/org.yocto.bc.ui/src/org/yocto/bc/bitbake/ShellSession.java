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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;

/**
 * A class for Linux shell sessions.
 * @author kgilmer
 *
 */
public class ShellSession {
	/**
	 * Bash shell
	 */
	public static final int SHELL_TYPE_BASH = 1;
	/**
	 * sh shell
	 */
	public static final int SHELL_TYPE_SH = 2;
	private volatile boolean interrupt = false;
	/**
	 * String used to isolate command execution
	 */
	public static final String TERMINATOR = "234o987dsfkcqiuwey18837032843259d";
	public static final String LT = System.getProperty("line.separator");
	
	public static String getFilePath(String file) throws IOException {
		File f = new File(file);
		
		if (!f.exists() || f.isDirectory()) {
			throw new IOException("Path passed is not a file: " + file);
		}
		
		StringBuffer sb = new StringBuffer();
		
		String elems[] = file.split(File.separator);
		
		for (int i = 0; i < elems.length - 1; ++i) {
			sb.append(elems[i]);
			sb.append(File.separator);
		}
		
		return sb.toString();
	}
	private Process process;

	private OutputStream pos = null;
	//private File initFile = null;
	private String shellPath = null;
	private final String initCmd;
	private final File root;
	private final Writer out;
	

	public ShellSession(int shellType, File root, String initCmd, Writer out) throws IOException {
		this.root = root;
		this.initCmd  = initCmd;
		if (out == null) {
			this.out = new NullWriter();
		} else {
			this.out = out;
		}
		if (shellType == SHELL_TYPE_SH) {
			shellPath = "/bin/sh";
		}
		shellPath  = "/bin/bash";
		
		initializeShell();
	}

	private void initializeShell() throws IOException {
		process = Runtime.getRuntime().exec(shellPath);
		pos = process.getOutputStream();
		
		if (root != null) {
			out.write(execute("cd " + root.getAbsolutePath()));
		}
		
		if (initCmd != null) {
			out.write(execute("source " + initCmd));
		}
	}

	synchronized 
	public String execute(String command) throws IOException {
		String errorMessage = null;
		interrupt = false;
		out.write(command);
		out.write(LT);
		
		sendToProcessAndTerminate(command);

		if (process.getErrorStream().available() > 0) {
			byte[] msg = new byte[process.getErrorStream().available()];

			process.getErrorStream().read(msg, 0, msg.length);
			out.write(new String(msg));
			out.write(LT);
			errorMessage = "Error while executing: " + command + LT + new String(msg);
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(process
				.getInputStream()));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while (((line = br.readLine()) != null) && !line.equals(TERMINATOR) && !interrupt) {
			sb.append(line);
			sb.append(LT);
			out.write(line);
			out.write(LT);
		}
		
		if (interrupt) {
			process.destroy();
			initializeShell();
			interrupt = false;
		}
		
		if (errorMessage != null) {
			throw new IOException(errorMessage);
		}

		return sb.toString();
	}

synchronized 
	public void execute(String command, ICommandResponseHandler handler) throws IOException {
		System.out.println(command);
		execute(command, TERMINATOR, handler);
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
				out.write(new String(msg));
				handler.response(new String(msg), true);
			} 
			
			std = br.readLine();
			
			if (std != null && !std.equals(terminator)) {
				out.write(std);
				handler.response(std, false);
			} 
			
		} while (std != null && !std.equals(terminator) && !interrupt);
		
		if (interrupt) {
			process.destroy();
			initializeShell();
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
		pos.write("echo ".getBytes());
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
	
	private class NullWriter extends Writer {

		@Override
		public void close() throws IOException {			
		}

		@Override
		public void flush() throws IOException {			
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {			
		}
		
	}
}

/*****************************************************************************
 * Copyright (c) 2013 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Jessica Zhang - Adopt for Yocto Tools plugin
 *******************************************************************************/
package org.yocto.remote.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;

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
	public static final String TERMINATOR = "build$";
	public static final String LT = System.getProperty("line.separator");
	private Process process;

	private OutputStream pos = null;

	private String shellPath = null;
	private final String initCmd;
	private final File root;

	private OutputStreamWriter out;

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

	public ShellSession(int shellType, File root, String initCmd, OutputStream out) throws IOException {
		this.root = root;
		this.initCmd  = initCmd;
		if (out == null) {
			this.out = new OutputStreamWriter(null);
		} else {
			this.out = new OutputStreamWriter(out);
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
			execute("cd " + root.getAbsolutePath());
		}

		if (initCmd != null) {
			execute("source " + initCmd);
		}
	}

	synchronized
	public String execute(String command, int[] retCode) throws IOException {
		String errorMessage = null;

		interrupt = false;
		out.write(command);
		out.write(LT);

		sendToProcessAndTerminate(command);

		if (process.getErrorStream().available() > 0) {
			byte[] msg = new byte[process.getErrorStream().available()];

			process.getErrorStream().read(msg, 0, msg.length);
			String msg_str = new String(msg);
			out.write(msg_str);
			out.write(LT);
			if (!msg_str.contains("WARNING"))
				errorMessage = "Error while executing: " + command + LT + new String(msg);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(process
				.getInputStream()));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while (true) {
			line = br.readLine();
			if (line != null) {
				sb.append(line);
				sb.append(LT);
				out.write(line);
				out.write(LT);
			}
			if (line.endsWith(TERMINATOR))
				break;
		}
 		if (interrupt) {
			process.destroy();
			initializeShell();
			interrupt = false;
		}else if (line != null && retCode != null) {
			try {
				retCode[0]=Integer.parseInt(line.substring(0,line.lastIndexOf(TERMINATOR)));
			}catch (NumberFormatException e) {
				throw new IOException("Can NOT get return code" + command + LT + line);
			}
		}
		out.flush();
		if (errorMessage != null) {
			throw new IOException(errorMessage);
		}
		return sb.toString();
	}

	synchronized
	public void execute(String command) throws IOException {
		interrupt = false;
		String errorMessage = null;

		sendToProcessAndTerminate(command);
		boolean cancel = false;
		try {
			InputStream is = process.getInputStream();
			InputStream es = process.getErrorStream();
			String info;
			while (!cancel) {
				info = null;
				StringBuffer buffer = new StringBuffer();
				int c;
				while (is.available() > 0) {
					c = is.read();
					char ch = (char) c;
					buffer.append(ch);
					if (ch == '\n') {
						info = buffer.toString();
						if (!info.trim().endsWith(TERMINATOR)) {
							out.write(info);
							out.write(LT);
							buffer.delete(0, buffer.length());
						} else {
							cancel = true;
							break;
						}
					}
				}
				while (es.available() > 0) {
					c = es.read();
					char ch = (char) c;
					buffer.append(ch);
					if (ch == '\n') {
						info = buffer.toString();
						if (!info.contains("WARNING"))
							errorMessage += info;
						out.write(info);
						out.write(LT);
						buffer.delete(0, buffer.length());
					}
				}
			}
		} catch (IOException e) {
			try {
				throw new InvocationTargetException(e);
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}
		out.flush();
		if (errorMessage != null) {
			throw new IOException(errorMessage);
		}
		if (interrupt) {
			process.destroy();
			initializeShell();
			interrupt = false;
		}
	}
	synchronized
	public boolean ensureKnownHostKey(String user, String host) throws IOException {

		boolean loadKeysMatch = false;
		boolean accepted = false;
		Process proc = Runtime.getRuntime().exec("ssh -o LogLevel=DEBUG3 " + user + "@" + host);
		Pattern patternLoad = Pattern.compile("^debug3: load_hostkeys: loaded (\\d+) keys");
		Pattern patternAuthSucceeded = Pattern.compile("^debug1: Authentication succeeded.*");

		try {
			InputStream es = proc.getErrorStream();
			String info;
			while (!loadKeysMatch) {
				info = null;
				StringBuffer buffer = new StringBuffer();
				int c;
				while (es.available() > 0) {
					c = es.read();
					char ch = (char) c;
					buffer.append(ch);
					if (ch == '\r') {
						info = buffer.toString().trim();
						Matcher m = patternLoad.matcher(info);
						if(m.matches()) {
							int keys = new Integer(m.group(1));
							if (keys == 0) {
								proc.destroy();
								accepted = MessageDialog.openQuestion(null, "Host authenticity", "The authenticity of host '" + host + "(" + host + ")' can't be established.\nAre you sure you want to continue connecting ?");
								if (accepted){
									proc = Runtime.getRuntime().exec("ssh -o StrictHostKeyChecking=no " + user + "@" + host);//add host key to known_hosts
									try {
										Thread.sleep(2000); //wait for process to finish
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									proc.destroy();
								} else {
									MessageDialog.openError(null, "Host authenticity", "Host key verification failed.");
								}
							} else {
								String errorMsg = "";
								// wait to check if key is the same and authentication succeeds
								while (es.available() > 0) {
									c = es.read();
									ch = (char) c;
									buffer.append(ch);
									if (ch == '\r') {
										info = buffer.toString().trim();
										Matcher mAuthS = patternAuthSucceeded.matcher(info);
										if(mAuthS.matches()) {
											accepted = true;
											break;
										} else {
											if (!info.startsWith("debug"))
												errorMsg += info + "\n";
										}
										try {
											Thread.sleep(100);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										buffer.delete(0, buffer.length());
									}
								}
								if (!accepted && !errorMsg.isEmpty())
									MessageDialog.openError(null, "Host authenticity", errorMsg);
							}
							loadKeysMatch = true;
							break;
						}
						buffer.delete(0, buffer.length());
					}
				}
			}
			es.close();
		} catch (IOException e) {
			try {
				throw new InvocationTargetException(e);
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}
		return accepted;
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
}

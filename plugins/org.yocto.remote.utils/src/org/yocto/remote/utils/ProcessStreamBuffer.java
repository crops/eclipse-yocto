/*******************************************************************************
 * Copyright (c) 2013 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.remote.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProcessStreamBuffer {
	private static final String WHITESPACES = "\\s+";
	List<String> errorLines;
	List<String> outputLines;
	boolean isTerminal;

	ProcessStreamBuffer(boolean isTerminal){
		this.isTerminal = isTerminal;
		errorLines = new ArrayList<String>();
		outputLines = new ArrayList<String>();
	}

	public void addErrorLine(String line){
		errorLines.add(line);
	}
	public void addOutputLine(String line){
		outputLines.add(line);
	}

	public List<String> getOutputLines(){
		return outputLines;
	}

	public List<String> getErrorLines(){
		return errorLines;
	}

	public String getMergedOutputLines(){
		String returnVal = "";
		for (int i = 0; i < outputLines.size(); i++) {
			String line = outputLines.get(i);
			returnVal += line;
			if (outputLines.size() > 1 && i != outputLines.size() - 1)
				returnVal += "\n";
		}
		return returnVal;
	}

	public boolean hasErrors() {
		return errorLines.size() != 0;
	}

	public String getLastOutputLineContaining(String str) {
		if (!errorLines.isEmpty())
			return null;
		for (int i = outputLines.size() - 1; i >= 0; i--){
			String line = outputLines.get(i);
			if (line.replaceAll(WHITESPACES, "").contains(str.replaceAll(WHITESPACES, "")))
				return line;
		}
		return null;
	}

	public String getOutputLineContaining(String arg, String pattern) {
		List<String> lines = null;
		if (isTerminal)
			lines = errorLines;
		else
			lines = outputLines;
		for (int i = lines.size() - 1; i >= 0; i--){
			String line = lines.get(i);
			if (line.contains(arg)) {
				String[] tokens = line.split("\\s+");
				if (Pattern.matches(pattern,  tokens[0])) {
					return tokens[0];
				}
			}
		}
		return "";
	}
}

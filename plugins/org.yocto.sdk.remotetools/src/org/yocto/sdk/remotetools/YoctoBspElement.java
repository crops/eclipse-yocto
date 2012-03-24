/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.remotetools;

public class YoctoBspElement {
	private String metadataLoc;
	private String buildLoc;
	private String bspName;
	private String bspOutLoc;
	private String karch;
	private String qarch;
	private boolean validPropertiesFile;

	public YoctoBspElement()
	{
		this.metadataLoc = "";
		this.buildLoc = "";
		this.bspName = "";
		this.bspOutLoc = "";
		this.karch = "";
		this.qarch = "";
		this.validPropertiesFile = false;
	}

	public String getMetadataLoc() {
		return metadataLoc;
	}
	
	public void setMetadataLoc(String value) {
		metadataLoc = value;
	}
	
	public String getBuildLoc() {
		return buildLoc;
	}
	
	public void setBuildLoc(String value) {
		buildLoc = value;
	}
	
	public String getBspName() {
		return bspName;
	}
	
	public void setBspName(String value) {
		this.bspName = value;
	}
	
	public String getBspOutLoc() {
		return bspOutLoc;
	}
	
	public void setBspOutLoc(String value) {
		this.bspOutLoc = value;
	}
	
	public String getKarch() {
		return karch;
	}
	
	public void setKarch(String value) {
		this.karch = value;
	}
	
	public String getQarch() {
		return qarch;
	}
	
	public void setQarch(String value) {
		this.qarch = value;
	}
	
	public boolean getValidPropertiesFile() {
		return validPropertiesFile;
	}
	
	public void setValidPropertiesFile(boolean value) {
		this.validPropertiesFile = value;
	}
}

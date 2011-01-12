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
package org.yocto.bc.ui.model;


public class ProjectInfo implements IModelElement {
	private String name;
	private String location;
	private String init;
	
	public ProjectInfo() {
	}
	
	public String getInitScriptPath() {
		return init;
	}
	public String getProjectName() {
		return name;
	}
	public String getRootPath() {
		return location;
	}
	public void initialize() throws Exception {
		name = new String();
		location = new String();
		init = new String();
	}

	public void setInitScriptPath(String init) {
		this.init = init;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}

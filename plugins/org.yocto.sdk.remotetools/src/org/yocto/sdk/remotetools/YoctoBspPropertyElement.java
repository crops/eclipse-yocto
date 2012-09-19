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

public class YoctoBspPropertyElement implements Comparable<YoctoBspPropertyElement>{
	private String name;
	private String type;
	private String value;
	private String defaultValue;

	public YoctoBspPropertyElement()
	{
		this.name = "";
		this.type = "";
		this.value = "";
		this.defaultValue = "";
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String value) {
		type = value;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String value) {
		this.defaultValue = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int compareTo(YoctoBspPropertyElement o) {
		return type.compareTo(o.type);
	}


}

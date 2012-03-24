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

import java.io.IOException;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.FileReader;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class YoctoJSONHelper {
	private static final String PROPERTIES_FILE = "/tmp/properties.json";
	private static final String PROPERTY_VALUE_FILE = "/tmp/propertyvalues.json";

	private static HashSet<YoctoBspPropertyElement> properties;

	public static HashSet<YoctoBspPropertyElement> getProperties() throws Exception {
		
		properties = new HashSet<YoctoBspPropertyElement>(); 
		try {
			
			JSONObject obj = (JSONObject)JSONValue.parse(new FileReader(PROPERTIES_FILE));
			Set<String> keys = obj.keySet();
			if (!keys.isEmpty()) {
				Iterator<String> iter = keys.iterator();
			    while (iter.hasNext()) {
			      String key = (String)iter.next();
			      if (validKey(key)) {
			    	  JSONObject value_obj = (JSONObject)obj.get(key);
			    	  YoctoBspPropertyElement elem = new YoctoBspPropertyElement();
			    	  elem.setName(key);
			    	  String type = (String)value_obj.get("type");
			    	  elem.setType(type);
			    	  if (type.contentEquals("boolean")) {
			    		  elem.setDefaultValue((String)value_obj.get("default"));
			    	  }
			    	  properties.add(elem);
			      }
			    }
			}
			
		} catch (Exception e) {
			throw e;
		} 
		return properties;
	}

	public static void createBspJSONFile(HashSet<YoctoBspPropertyElement> properties) {
		try {
			JSONObject obj = new JSONObject();
			if (!properties.isEmpty()) {
				Iterator<YoctoBspPropertyElement> it = properties.iterator();
				while (it.hasNext()) {
					// Get property
					YoctoBspPropertyElement propElem = (YoctoBspPropertyElement)it.next();
					obj.put(propElem.getName(), propElem.getValue());
				}
			}
		 
			FileWriter file = new FileWriter(PROPERTY_VALUE_FILE);
			file.write(obj.toJSONString());
			file.flush();
			file.close();
		 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean validKey(String key) {
		if (key.contains("kernel"))
			return false;
		if (key.contentEquals("qemuarch"))
			return false;
		return true;
	}
}

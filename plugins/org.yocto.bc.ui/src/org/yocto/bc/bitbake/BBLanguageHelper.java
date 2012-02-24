/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Lianhao Lu (Intel) - add more bitbake keywords and functions
 *******************************************************************************/
package org.yocto.bc.bitbake;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Here is where all BitBake-related information is centralized.
 * @author kgilmer
 *
 */
public class BBLanguageHelper {
	
	public static final String[] BITBAKE_KEYWORDS = new String[] { "inherit", "require", "export", "addtask", "python", "include", "fakeroot", "addhandler", "def"};
	public static final String[] SHELL_KEYWORDS = new String[] { "while", "do", "if", "fi", "ln", "export", "install", "oe_libinstall", "for", "in", "done", "echo", "then", "cat", "rm", "rmdir", "mkdir", "printf", "exit", "test", "cd", "cp"};
	public static final String[] BITBAKE_STANDARD_FUNCTIONS = new String[] { "fetch", "unpack", "patch", "configure", "compile", "install", "populate_sysroot", "package"};
	public static final String BITBAKE_RECIPE_FILE_EXTENSION = "bb";

	/**
	 * @return A map of names and descriptions of commonly used BitBake variables.
	 */
	public static Map<String, String> getCommonBitbakeVariables() {
		Map<String, String> m = new TreeMap<String, String>(new Comparator<Object>() {

			public int compare(Object o1, Object o2) {

				return ((String) o1).compareTo(((String) o2));
			}
			
		});
		
		m.put("SECTION", "Category of package");
		m.put("PR", "Package Release Number");
		m.put("SRC_URI", "Location of package sources");
		m.put("DESCRIPTION", "Description of package");
		m.put("EXTRA_OEMAKE", "Extra flags to pass to the package makefile");
		m.put("EXTRA_OECONF", "Extra configuration flags for the package makefile");
		m.put("DEPENDS", "The set of build-time dependent packages");
		m.put("RDEPENDS", "The set of run-time dependent packages");
		m.put("HOMEPAGE", "Homepage of the package");
		m.put("LICENSE", "License of the package");
		m.put("FILES_${PN}", "Full file path of files on target.");
		m.put("S", "Package source directory");
		m.put("PV", "Package version");
		m.put("AUTHOR", "Author or maintainer of package");
		m.put("PRIORITY", "Priority of package");
		
		return m;
	}

}

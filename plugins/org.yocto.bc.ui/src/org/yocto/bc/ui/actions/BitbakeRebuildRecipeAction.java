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
package org.yocto.bc.ui.actions;

/**
 * Rebuild a recipe.
 * @author kgilmer
 *
 */
public  class BitbakeRebuildRecipeAction extends AbstractBitbakeCommandAction {

	@Override
	public String [] getCommands() {
		return new String[] {"bitbake -c rebuild -b " + recipe.getLocationURI().getPath()};
	}

	@Override
	public String getJobTitle() {
		return "Rebuilding " + recipe.getName();
	}
}
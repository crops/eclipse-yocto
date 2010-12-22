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
package org.yocto.bc.ui.editors.bitbake;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class RecipeEditorActionContributor extends TextEditorActionContributor {

	private RetargetTextEditorAction fContentAssist;

	public RecipeEditorActionContributor() {
		fContentAssist= new RetargetTextEditorAction(RecipeEditorMessages.getBundle(), "contentAssist."); //$NON-NLS-1$
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	}
	
	@Override
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		
		IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fContentAssist);
		}
	}
	
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		if (part instanceof ITextEditor) {
			fContentAssist.setAction(getAction((ITextEditor) part, BitBakeFileEditor.CONTENT_ASSIST));
		}
	}
}

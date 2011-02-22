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

import java.io.IOException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.yocto.bc.ui.Activator;

/**
 * Editor for BB Recipe
 * @author kgilmer
 *
 */
public class BitBakeFileEditor extends AbstractDecoratedTextEditor {

	public static final String EDITOR_ID = "org.yocto.bc.ui.editors.BitBakeFileEditor";
	static final String CONTENT_ASSIST= "ContentAssist";
	private BitBakeSourceViewerConfiguration viewerConfiguration;
	private String targetFilePath;
	
	public BitBakeFileEditor() {
		super();
		viewerConfiguration = new BitBakeSourceViewerConfiguration(getSharedColors(), getPreferenceStore());
		setSourceViewerConfiguration(viewerConfiguration);
		setDocumentProvider(new BitBakeDocumentProvider());
	}
	
	@Override
	protected void createActions() {
		super.createActions();
		
		ResourceBundle bundle= RecipeEditorMessages.getBundle();
		ContentAssistAction action= new ContentAssistAction(bundle, "contentAssist.", this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(CONTENT_ASSIST, action);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
			
		if (input instanceof FileEditorInput) {
			IProject p = ((FileEditorInput)input).getFile().getProject();
			targetFilePath = ((FileEditorInput)input).getFile().getLocationURI().getPath();
			viewerConfiguration.setTargetFilePath(targetFilePath);
			
			//llu detach BBSession
			/*
			try {
				viewerConfiguration.setBBSession(Activator.getBBSession(p.getLocationURI().getPath()));
			} catch (IOException e) {
				e.printStackTrace();
				throw new PartInitException(Status.CANCEL_STATUS);
			}
			*/
		}
		super.init(site, input);
	}
}
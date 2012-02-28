/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Lianhao Lu (Intel) - remove compile warnings
 *******************************************************************************/
package org.yocto.bc.ui.editors.bitbake;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.PlatformUI;

import org.yocto.bc.bitbake.BBLanguageHelper;
import org.yocto.bc.ui.Activator;

class RecipeCompletionProcessor implements IContentAssistProcessor {

	private static final String CONTEXT_ID= "bitbake_variables"; //$NON-NLS-1$
	private final TemplateContextType fContextType= new TemplateContextType(CONTEXT_ID, "Common BitBake Variables"); //$NON-NLS-1$
	//private final TemplateContextType fKeywordContextType= new TemplateContextType("bitbake_keywords", "BitBake Keywords"); //$NON-NLS-1$
	private final TemplateContextType fFunctionContextType = new TemplateContextType("bitbake_functions", "BitBake Functions");

	RecipeCompletionProcessor() {
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document= viewer.getDocument();
		Region region= new Region(offset, 0);
		
		TemplateContext templateContext= new DocumentTemplateContext(fContextType, document, offset, 0);
		//TemplateContext keywordContext = new DocumentTemplateContext(fKeywordContextType, document, offset, 0);
		TemplateContext functionContext = new DocumentTemplateContext(fFunctionContextType, document, offset, 0);
		
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
		
		getVariableTemplateProposals(templateContext, region, proposals);
		// getKeywordTemplateProposals(keywordContext, region, proposals);
		getAddTaskTemplateProposals(templateContext, region, proposals);
		getFunctionTemplateProposals(functionContext, region, proposals);
		
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
	
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}
	
	private Template generateVariableTemplate(String name, String description) {

		return new Template(name, description, CONTEXT_ID, name + " = \"${" + name.toLowerCase() + "}\"", false);
	}

	private void getAddTaskTemplateProposals(TemplateContext templateContext, Region region, List<ICompletionProposal> p) {
			p.add(new TemplateProposal(new Template("addtask", "addtask statement", CONTEXT_ID, "addtask ${task_name} after ${do_previous_task} before ${do_next_task}", false),templateContext, region, PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJS_BKMRK_TSK)));
	}


	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}
	
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	private void getFunctionTemplateProposals(TemplateContext templateContext, Region region, List<ICompletionProposal> p) {
		String [] keywords = BBLanguageHelper.BITBAKE_STANDARD_FUNCTIONS;
		Image img = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_FUNCTION);
		Arrays.sort(keywords);
		
		for (int i = 0; i < keywords.length; ++i) {
			p.add(new TemplateProposal(new Template(keywords[i], keywords[i] + " function", CONTEXT_ID, "do_" + keywords[i] + "() {\n\n}", false), templateContext, region, img));
		}
	}
	/*
	private void getKeywordTemplateProposals(TemplateContext templateContext, Region region, List<TemplateProposal> p) {
		String [] keywords = BBLanguageHelper.BITBAKE_KEYWORDS;
		
		Arrays.sort(keywords);
		
		for (int i = 0; i < keywords.length; ++i) {
			p.add(new TemplateProposal(new Template(keywords[i], keywords[i] + " keyword", CONTEXT_ID, keywords[i] + " ", false),templateContext, region, null));
		}
	}
	*/

	private void getVariableTemplateProposals(TemplateContext templateContext, Region region, List<ICompletionProposal> p) {
		Map<String, String> n = BBLanguageHelper.getCommonBitbakeVariables();
		Image img = Activator.getDefault().getImageRegistry().get(Activator.IMAGE_VARIABLE);
		for (Iterator<String> i = n.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			String description = (String) n.get(name);
			p.add(new TemplateProposal(generateVariableTemplate(name, description), templateContext, region, img));	
		}	
	}
}
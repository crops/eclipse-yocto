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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import org.yocto.bc.bitbake.BBLanguageHelper;
import org.yocto.bc.bitbake.BBSession;

public class BitBakeSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private static final class WordDetector implements IWordDetector {
		public boolean isWordPart(char c) {
			return !Character.isWhitespace(c);
		}

		public boolean isWordStart(char c) {
			return !Character.isWhitespace(c);
		}
	}

	private final ISharedTextColors fSharedColors;
	private BBSession session;
	private String targetFilePath;
	private BBVariableTextHover textHover;

	public BitBakeSourceViewerConfiguration(ISharedTextColors sharedColors, IPreferenceStore store) {
		super(store);
		fSharedColors = sharedColors;		
	}
	
	protected void setTargetFilePath(String targetFilePath) {
		this.targetFilePath = targetFilePath;
	}

	public ITextHover getTextHover(ISourceViewer sv, String contentType) {
		if (textHover == null) {
			//llu, disable text hover temporarily.
			//textHover = new BBVariableTextHover(session, targetFilePath);
		}
		
		return textHover;
	}

	private void addDamagerRepairer(PresentationReconciler reconciler, RuleBasedScanner commentScanner, String contentType) {
		DefaultDamagerRepairer commentDamagerRepairer = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(commentDamagerRepairer, contentType);
		reconciler.setRepairer(commentDamagerRepairer, contentType);
	}

	private RuleBasedScanner createCommentScanner() {
		Color green = fSharedColors.getColor(new RGB(16, 96, 16));
		RuleBasedScanner commentScanner = new RuleBasedScanner();
		commentScanner.setDefaultReturnToken(new Token(new TextAttribute(green, null, SWT.ITALIC)));
		return commentScanner;
	}

	private IRule createCustomFunctionRule() {
		Color blue = fSharedColors.getColor(new RGB(130, 0, 0));
		IRule rule = new CustomFunctionRule(new Token(new TextAttribute(blue, null, SWT.BOLD)));

		return rule;
	}

	private SingleLineRule createFunctionNameRule() {
		Color red = fSharedColors.getColor(new RGB(150, 0, 96));
		SingleLineRule stepRule = new SingleLineRule("do_", ")", new Token(new TextAttribute(red, null, SWT.BOLD))); //$NON-NLS-1$ //$NON-NLS-2$
		stepRule.setColumnConstraint(0);
		return stepRule;
	}

	private SingleLineRule createInlineVariableRule() {
		Color blue = fSharedColors.getColor(new RGB(50, 50, 100));
		SingleLineRule stepRule = new SingleLineRule("${", "}", new Token(new TextAttribute(blue, null, SWT.BOLD))); //$NON-NLS-1$ //$NON-NLS-2$
		return stepRule;
	}

	private WordRule createKeywordRule() {
		WordRule keywordRule = new WordRule(new WordDetector());
		IToken token = new Token(new TextAttribute(fSharedColors.getColor(new RGB(96, 96, 0)), null, SWT.NONE));

		for (int i = 0; i < BBLanguageHelper.BITBAKE_KEYWORDS.length; ++i) {

			keywordRule.addWord(BBLanguageHelper.BITBAKE_KEYWORDS[i], token);
			keywordRule.setColumnConstraint(0);
		}

		return keywordRule;
	}

	private RuleBasedScanner createRecipeScanner() {
		RuleBasedScanner recipeScanner = new RuleBasedScanner();

		IRule[] rules = { createKeywordRule(), createShellKeywordRule(), createStringLiteralRule(), createVariableRule(), createFunctionNameRule(), createCustomFunctionRule(),
				createInlineVariableRule() };
		recipeScanner.setRules(rules);
		return recipeScanner;
	}

	private WordRule createShellKeywordRule() {
		WordRule keywordRule = new WordRule(new WordDetector());
		IToken token = new Token(new TextAttribute(fSharedColors.getColor(new RGB(0, 64, 92)), null, SWT.NONE));

		for (int i = 0; i < BBLanguageHelper.SHELL_KEYWORDS.length; ++i) {
			keywordRule.addWord(BBLanguageHelper.SHELL_KEYWORDS[i], token);
		}

		return keywordRule;
	}

	private SingleLineRule createStringLiteralRule() {
		Color red = fSharedColors.getColor(new RGB(50, 50, 100));
		SingleLineRule rule = new SingleLineRule("\"", "\"", new Token(new TextAttribute(red, null, SWT.NONE)), '\\');

		return rule;
	}

	private IRule createVariableRule() {
		Color blue = fSharedColors.getColor(new RGB(0, 0, 200));
		IRule rule = new VariableRule(new Token(new TextAttribute(blue, null, SWT.NONE)));

		return rule;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { BitBakeDocumentProvider.RECIPE_CODE, BitBakeDocumentProvider.RECIPE_COMMENT };
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return BitBakeDocumentProvider.RECIPE_PARTITIONING;
	}

	@Override
	public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		// assistant.setContentAssistProcessor(new HippieProposalProcessor(),
		// BitBakeDocumentProvider.RECIPE_COMMENT);
		assistant.setContentAssistProcessor(new RecipeCompletionProcessor(), BitBakeDocumentProvider.RECIPE_CODE);

		return assistant;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		return null;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		addDamagerRepairer(reconciler, createCommentScanner(), BitBakeDocumentProvider.RECIPE_COMMENT);
		addDamagerRepairer(reconciler, createRecipeScanner(), BitBakeDocumentProvider.RECIPE_CODE);

		return reconciler;
	}

	public void setBBSession(BBSession session) {
		this.session = session;		
	}
}

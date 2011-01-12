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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * Document provider for BB recipe.
 * @author kgilmer
 *
 */
public class BitBakeDocumentProvider extends FileDocumentProvider {
	/**
	 * The recipe partitioning. It contains two partition types: {@link #RECIPE_CODE} and
	 * {@link #RECIPE_COMMENT}.
	 */
	public static final String RECIPE_PARTITIONING= "org.recipeeditor.recipepartitioning"; //$NON-NLS-1$

	public static final String RECIPE_CODE= IDocument.DEFAULT_CONTENT_TYPE;
	public static final String RECIPE_COMMENT= "RECIPE_COMMENT"; //$NON-NLS-1$
	
	private static final String[] CONTENT_TYPES= {
			RECIPE_CODE,
			RECIPE_COMMENT
	};

	private IDocumentPartitioner createRecipePartitioner() {
		IPredicateRule[] rules= { new SingleLineRule("#", null, new Token(RECIPE_COMMENT), (char) 0, true, false) }; //$NON-NLS-1$

		RuleBasedPartitionScanner scanner= new RuleBasedPartitionScanner();
		scanner.setPredicateRules(rules);
		
		return new FastPartitioner(scanner, CONTENT_TYPES);
	}

	@Override
	protected void setupDocument(Object element,IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 ext= (IDocumentExtension3) document;
			IDocumentPartitioner partitioner= createRecipePartitioner();
			ext.setDocumentPartitioner(RECIPE_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}

}

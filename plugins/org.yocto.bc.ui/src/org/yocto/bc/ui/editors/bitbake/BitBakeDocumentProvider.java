/*****************************************************************************
 * Copyright (c) 2013 Ken Gilmer, Intel Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Ioana Grigoropol (Intel) - adapt class for remote support
 *******************************************************************************/
package org.yocto.bc.ui.editors.bitbake;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.yocto.remote.utils.RemoteHelper;

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

	private IHost connection;

	private final BitBakeSourceViewerConfiguration viewerConfiguration;

	private static final String[] CONTENT_TYPES= {
			RECIPE_CODE,
			RECIPE_COMMENT
	};

	public BitBakeDocumentProvider(BitBakeSourceViewerConfiguration viewerConfiguration) {
		this.viewerConfiguration = viewerConfiguration;
	}

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

	@Override
	public boolean isDeleted(Object element) {
		if (element instanceof IFileEditorInput) {
			IFileEditorInput input= (IFileEditorInput) element;

		    URI root = viewerConfiguration.getBBSession().getProjInfoRoot();
		    String relPath = input.getFile().getProjectRelativePath().toPortableString();
		    try {
				URI fileURI = new URI(root.getScheme(), root.getHost(), root.getPath() + "/" + relPath, root.getFragment());
				if (connection == null)
					connection = viewerConfiguration.getBBSession().getProjectInfo().getConnection();
				return !RemoteHelper.fileExistsRemote(connection, new NullProgressMonitor(), fileURI.getPath());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		return super.isDeleted(element);
	}

	public void setActiveConnection(IHost connection) {
		this.connection = connection;
	}
}

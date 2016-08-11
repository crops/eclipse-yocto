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
package org.yocto.bc.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

public class BitbakeCommanderPerspective implements IPerspectiveFactory {

	private IPageLayout factory;

	public BitbakeCommanderPerspective() {
		super();
	}

	private void addActionSets() {
		factory.addActionSet("org.yocto.bc.ui.actionSet");
		factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); // NON-NLS-1
	}

	private void addNewWizardShortcuts() {
		factory.addNewWizardShortcut("org.yocto.bc.ui.wizards.NewRecipeWizard");// NON-NLS-1
		//factory.addNewWizardShortcut("org.yocto.bc.ui.wizards.newproject.NewBBCProjectWizard");// NON-NLS-1
		factory.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");// NON-NLS-1
	}

	private void addPerspectiveShortcuts() {
		//factory.addPerspectiveShortcut("org.eclipse.team.ui.TeamSynchronizingPerspective"); //NON-NLS-1
		// TODO: add egit perspective instead
		//factory.addPerspectiveShortcut("org.eclipse.team.cvs.ui.cvsPerspective"); //$NON-NLS-1$
		factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); // NON-NLS-1
	}

	private void addViews() {
		IFolderLayout bottom = factory.createFolder("bottomRight", // NON-NLS-1
				IPageLayout.BOTTOM, 0.75f, factory.getEditorArea());

		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView("org.eclipse.team.ui.GenericHistoryView"); // NON-NLS-1
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		
		IFolderLayout topLeft = factory.createFolder("topLeft", // NON-NLS-1
				IPageLayout.LEFT, 0.25f, factory.getEditorArea());
		topLeft.addView(IPageLayout.ID_PROJECT_EXPLORER);
		//llu detach RecipeView
		//topLeft.addView(RecipeView.ID_VIEW); // NON-NLS-1
		
	}

	private void addViewShortcuts() {
		// factory.addShowViewShortcut("org.eclipse.ant.ui.views.AntView");
		// //NON-NLS-1
		// factory.addShowViewShortcut("org.eclipse.team.ccvs.ui.AnnotateView");
		// //NON-NLS-1
		// factory.addShowViewShortcut("org.eclipse.pde.ui.DependenciesView");
		// //NON-NLS-1
		// factory.addShowViewShortcut("org.eclipse.jdt.junit.ResultView");
		// //NON-NLS-1
		factory.addShowViewShortcut("org.eclipse.team.ui.GenericHistoryView"); // NON-NLS-1
		factory.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		// factory.addShowViewShortcut(JavaUI.ID_PACKAGES);
		factory.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
		// factory.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		// factory.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}

	public void createInitialLayout(IPageLayout factory) {
		this.factory = factory;
		addViews();
		addActionSets();
		addNewWizardShortcuts();
		addPerspectiveShortcuts();
		addViewShortcuts();
	}

}

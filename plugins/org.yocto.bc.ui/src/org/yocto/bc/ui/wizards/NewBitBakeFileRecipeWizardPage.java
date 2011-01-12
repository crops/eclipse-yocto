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
package org.yocto.bc.ui.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class NewBitBakeFileRecipeWizardPage extends WizardPage {
	private Text containerText;

	private Text fileText;

	private ISelection selection;

	private Text descriptionText;

	private Text licenseText;

	private Text homepageText;

	private Text authorText;

	private Text srcuriText;
	public NewBitBakeFileRecipeWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("BitBake Recipe");
		setDescription("Create a new BitBake recipe.");
		this.selection = selection;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		createField(container, "&Recipe Name:", (fileText = new Text(container, SWT.BORDER | SWT.SINGLE)));

		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText("&Directory:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		label = new Label(container, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		// label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		createField(container, "&Package Description:", (descriptionText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&License:", (licenseText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&Homepage:", (homepageText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "Package &Author:", (authorText = new Text(container, SWT.BORDER | SWT.SINGLE)));

		// label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

		createField(container, "SRC_URI:", (srcuriText = new Text(container, SWT.BORDER | SWT.SINGLE)));

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void createField(Composite container, String title, Text control) {
		Label label = new Label(container, SWT.NONE);
		label.setText(title);
		label.moveAbove(control);
	
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
		control.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}

		});
	}

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus("Directory must be specified");
			return;
		}
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}

		if (getDescriptionText().length() == 0) {
			updateStatus("Recipe must have a description");
			return;
		}

		updateStatus(null);
	}

	public String getAuthorText() {
		return authorText.getText();
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getDescriptionText() {
		return descriptionText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}

	public String getHomepageText() {
		return homepageText.getText();
	}

	public String getLicenseText() {
		return licenseText.getText();
	}

	public String getSrcuriText() {
		return srcuriText.getText();
	}

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select project directory");
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}
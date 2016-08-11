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
package org.yocto.bc.ui.wizards.importProject;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import org.yocto.bc.ui.wizards.FiniteStateWizardPage;

/**
 * Main property page for new project wizard.
 * @author kgilmer
 *
 */
public class BBCProjectPage extends FiniteStateWizardPage {

	private class FileOpenSelectionAdapter extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			FileDialog fd = new FileDialog(PlatformUI.getWorkbench()
					.getDisplay().getActiveShell(), SWT.OPEN);

			fd.setText("Open Configuration Script");
			fd.setFilterPath(txtProjectLocation.getText());

			String selected = fd.open();

			if (selected != null) {
				txtInit.setText(selected);
				updateModel();
			}
		}
	}
	public static final String PAGE_TITLE = "Yocto Project BitBake Commander Project";
	private Text txtProjectLocation;

	private Text txtInit;
	private ValidationListener validationListener;
	private Text txtProjectName;

	public BBCProjectPage(Map<String, Object> model) {
		super(PAGE_TITLE, model);
		setTitle("Create new Yocto Project BitBake Commander project");
		setMessage("Enter information to create a BitBake Commander project.");
	}

	public void createControl(Composite parent) {
		GridData gdFillH = new GridData(GridData.FILL_HORIZONTAL);
		GridData gdVU = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		top.setLayout(new GridLayout());

		Composite projectNameComp = new Composite(top, SWT.NONE);
		GridData gdProjName = new GridData(GridData.FILL_HORIZONTAL);
		projectNameComp.setLayoutData(gdProjName);
		projectNameComp.setLayout(new GridLayout(2, false));
		Label lblProjectName = new Label(projectNameComp, SWT.NONE);
		lblProjectName.setText("N&ame:");

		txtProjectName = new Text(projectNameComp, SWT.BORDER);
		txtProjectName.setLayoutData(gdFillH);
		txtProjectName.setFocus();
		validationListener = new ValidationListener();
		
		txtProjectName.addModifyListener(validationListener);

		Label lblProjectLocation = new Label(projectNameComp, SWT.None);
		lblProjectLocation.setText("&Location:");

		Composite locComposite = new Composite(projectNameComp, SWT.NONE);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_END
				| GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 0;
		locComposite.setLayoutData(gd);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		locComposite.setLayout(gl);

		txtProjectLocation = new Text(locComposite, SWT.BORDER);
		txtProjectLocation.setLayoutData(gdFillH);
		txtProjectLocation.addModifyListener(validationListener);

		Button button = new Button(locComposite, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		Label lblInit = new Label(projectNameComp, SWT.NONE);
		lblInit.setText("Init Script:");

		Composite initComposite = new Composite(projectNameComp, SWT.NONE);
		gd = new GridData(GridData.VERTICAL_ALIGN_END
				| GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 0;
		initComposite.setLayoutData(gd);
		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		initComposite.setLayout(gl);

		txtInit = new Text(initComposite, SWT.BORDER);
		GridData gdi = new GridData(GridData.FILL_HORIZONTAL);
		txtInit.setLayoutData(gdi);
		txtInit.addModifyListener(validationListener);

		Button btnLoadInit = new Button(initComposite, SWT.PUSH);
		btnLoadInit.setLayoutData(gdVU);
		btnLoadInit.setText("Choose...");
		btnLoadInit.addSelectionListener(new FileOpenSelectionAdapter());

		if (System.getenv("OEROOT") != null) {
			txtProjectLocation.setText(System.getenv("OEROOT"));
		}

		setControl(top);
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.None);
		String dir = dialog.open();
		if (dir != null) {
			txtProjectLocation.setText(dir);
		}
	}

	private String getFileSegment(String initScriptPath) {
		//return the first segment of " " seperated array, or full string if no " " exists
		return initScriptPath.split(" ")[0];
	}

	private boolean isValidProjectName(String projectName) {
		if (projectName.indexOf('$') > -1) {
			return false;
		}

		return true;
	}


	@Override
	public void pageCleanup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pageDisplay() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateModel() {
		model.put(ImportYoctoProjectWizard.KEY_NAME, txtProjectName.getText());
		model.put(ImportYoctoProjectWizard.KEY_LOCATION, txtProjectLocation.getText());
		model.put(ImportYoctoProjectWizard.KEY_INITPATH, txtInit.getText());
	}
	

	@Override
	protected boolean validatePage() {
		IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();

		IStatus validate = ResourcesPlugin.getWorkspace().validateName(txtProjectName.getText(), IResource.PROJECT);

		if (!validate.isOK() || !isValidProjectName(txtProjectName.getText())) {
			setErrorMessage("Invalid project name: " + txtProjectName.getText());
			return false;
		}

		IProject proj = wsroot.getProject(txtProjectName.getText());
		if (proj.exists()) {
			setErrorMessage("A project with the name " + txtProjectName.getText()
					+ " already exists");
			return false;
		}

		if (txtProjectLocation.getText().trim().length() == 0) {
			setErrorMessage("Set directory that contains Poky tree");
			return false;
		}

		File f = new File(txtProjectLocation.getText());
		if (!f.exists() || !f.isDirectory()) {
			setErrorMessage("Invalid Directory");
			return false;
		}
		
		if (txtInit.getText().length() == 0) {
			setErrorMessage("Set configuration file before BitBake is launched.");
			return false;
		}
		
		File f2 = new File(getFileSegment(txtInit.getText()));
		if (!f2.exists() || f2.isDirectory()) {
			setErrorMessage("The configuration file is invalid.");
			return false;
		}

		setErrorMessage(null);
		setMessage("All the entries are valid, press \"Finish\" to create the new yocto bitbake project,"+
		"this will take a while. Please don't interrupt till there's output in the Yocto Console window...");
		return true;
	}
}

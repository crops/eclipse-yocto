package org.yocto.bc.ui.wizards.install;

import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import org.yocto.bc.ui.wizards.FiniteStateWizard;
import org.yocto.bc.ui.wizards.FiniteStateWizardPage;
import org.yocto.bc.ui.wizards.FiniteStateWizardPage.ValidationListener;

/**
 * Select which flavor of OE is to be installed.
 * 
 * @author kgilmer
 * 
 * Setting up the parameters for creating the new Yocto Bitbake project
 * 
 * @modified jzhang
 */
public class OptionsPage extends FiniteStateWizardPage {

	private Map vars;
	private Composite c1;
	private Composite top;
	
	private List controlList;
	private boolean controlsCreated = false;
	
	private Text txtProjectLocation;

	private Text txtInit;
	private ValidationListener validationListener;
	private Text txtProjectName;
	private Button gitButton;

	protected OptionsPage(Map model) {
		super("Options", model);
		//setTitle("Create new yocto bitbake project");
		setMessage("Enter these parameters to create new Yocto Project BitBake commander project");
	}

	@Override
	public void createControl(Composite parent) {
		top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout());
		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridData gdFillH = new GridData(GridData.FILL_HORIZONTAL);
		GridData gdVU = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		
		Composite projectNameComp = new Composite(top, SWT.NONE);
		GridData gdProjName = new GridData(GridData.FILL_HORIZONTAL);
		projectNameComp.setLayoutData(gdProjName);
		projectNameComp.setLayout(new GridLayout(2, false));
		Label lblProjectName = new Label(projectNameComp, SWT.NONE);
		lblProjectName.setText("Project N&ame:");

		txtProjectName = new Text(projectNameComp, SWT.BORDER);
		txtProjectName.setLayoutData(gdFillH);
		txtProjectName.setFocus();
		validationListener = new ValidationListener();
		
		txtProjectName.addModifyListener(validationListener);

		Label lblProjectLocation = new Label(projectNameComp, SWT.None);
		lblProjectLocation.setText("&Project Location:");

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

		//Label lblGit = new Label(projectNameComp, SWT.None);
		//lblGit.setText("Clone from &Git Repository?");

		Composite gitComposite = new Composite(projectNameComp, SWT.NONE);
		gd = new GridData(GridData.VERTICAL_ALIGN_END
				| GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 0;
		gitComposite.setLayoutData(gd);
		gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gitComposite.setLayout(gl);

		gitButton = new Button(gitComposite, SWT.CHECK);
		gitButton.setText("Clone from Yocto Project &Git Repository");
		gitButton.setEnabled(true);
		gitButton.addSelectionListener(validationListener);

		setControl(top);
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.None);
		String dir = dialog.open();
		if (dir != null) {
			txtProjectLocation.setText(dir);
		}
	}
	
	@Override
	public void pageCleanup() {

	}

	@Override
	public void pageDisplay() {
	}

	@Override
	
	protected void updateModel() {
		model.put(InstallWizard.INSTALL_DIRECTORY, txtProjectLocation.getText()+File.separator+txtProjectName.getText());
		model.put(InstallWizard.PROJECT_NAME, txtProjectName.getText());
		model.put(InstallWizard.GIT_CLONE, new Boolean(gitButton.getSelection()));
	}

	private boolean isValidProjectName(String projectName) {
		if (projectName.indexOf('$') > -1) {
			return false;
		}

		return true;
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
		
		String projectLoc = txtProjectLocation.getText();
		File checkProject_dir = new File(projectLoc);
		if (!checkProject_dir.isDirectory()) {
			setErrorMessage("The project location directory " + txtProjectLocation.getText() + " is not valid");
			return false;
		}
		
		String projectPath = projectLoc + File.separator+txtProjectName.getText();
		if(!gitButton.getSelection()) {
			File git_dir=new File(projectPath);
			if(!git_dir.isDirectory()) {
				setErrorMessage("Directory " + txtProjectLocation.getText()+File.separator+txtProjectName.getText() + " does not exist, please select git clone.");
				return false;
			}
		}
		
		// Check whether the project directory contains build dir, if so prompt user to move out
		String build_dir = projectPath+File.separator+"build";
		File checkBuild_dir = new File(build_dir);
		if (checkBuild_dir.isDirectory()) {
			setErrorMessage("Project path "+projectPath+ " contains build sub-directory, which Eclipse IProject tree view won't be able to handle due to its size." +
					"Please move it outside the project directory through \"oe-init-build-env build_path\" if you still want to create a bitbake commander project!");
			return false;
		}
		try {
			URI location = new URI("file://" + txtProjectLocation.getText()+File.separator+txtProjectName.getText());
		
			IStatus status = ResourcesPlugin.getWorkspace().validateProjectLocationURI(proj, location);
			if (!status.isOK()) {
				setErrorMessage(status.getMessage());
				return false;
			}
		} catch (Exception e) {
			setErrorMessage("Run into error while trying to validate entries!");
			return false;
		}
		setErrorMessage(null);
		setMessage("All the entries are valid, press \"Finish\" to start the process, "+
				"this will take a while. Please don't interrupt till there's output in the Yocto Console window...");
		return true;
	}
	
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

}

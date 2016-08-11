package org.yocto.bc.ui.wizards.install;

import java.io.File;
import java.net.URI;
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
 * Select which flavor of OE is to be installed.
 * 
 * @author kgilmer
 * 
 * Setting up the parameters for creating the new Yocto Bitbake project
 * 
 * @modified jzhang
 */
public class OptionsPage extends FiniteStateWizardPage {

	private Composite top;
	private Text txtProjectLocation;
	private Text txtInit;
	private ValidationListener validationListener;
	private Text txtProjectName;

	protected OptionsPage(Map<String, Object> model) {
		super("Options", model);
		setMessage("Enter these parameters to create new Yocto Project BitBake commander project");
	}

	@Override
	public void createControl(Composite parent) {
		top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout());
		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridData gdFillH = new GridData(GridData.FILL_HORIZONTAL);
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

		String projectPath = new File(projectLoc,txtProjectName.getText()).getPath();
		File prj_dir=new File(projectPath);
		if(!prj_dir.isDirectory() || !prj_dir.exists()) {
			if(!new File(projectPath + File.separator + InstallWizard.VALIDATION_FILE).exists()) {
				setErrorMessage("Directory " + projectPath + " is an invalid poky directory.");
				return false;
			}
		}

		try {
			URI location = new URI("file://" + projectPath);
			IStatus status = ResourcesPlugin.getWorkspace().validateProjectLocationURI(proj, location);
			if (!status.isOK()) {
				setErrorMessage(status.getMessage());
				return false;
			}
		} catch (Exception e) {
			setErrorMessage("Run into error while trying to validate entries!");
			return false;
		}
		setErrorMessage("Press the 'Finish' button to create your project");
		return true;
	}
	
	@SuppressWarnings("unused")
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

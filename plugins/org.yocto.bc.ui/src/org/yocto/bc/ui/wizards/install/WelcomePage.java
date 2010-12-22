package org.yocto.bc.ui.wizards.install;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.yocto.bc.ui.wizards.FiniteStateWizardPage;

public class WelcomePage extends FiniteStateWizardPage  {

	public static final String ACTION_INSTALL = "ACTION_INSTALL";
	public static final String ACTION_USE = "ACTION_USE";
	private Button installButton;
	private Button useButton;

	protected WelcomePage(Map model) {
		super("Introduction", model);
		setTitle("Select Project Type");
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout());
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		ValidationListener listener = new ValidationListener();
		
		installButton = new Button(top, SWT.RADIO | SWT.WRAP);
		installButton.setText("Install a flavor of OpenEmbedded on your computer.");
		Composite lc = new Composite(top, SWT.None);
		lc.setLayout(new GridLayout(2, false));
		lc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label spacer = new Label(lc, SWT.None);
		spacer.setText("    ");
		Label installLabel = new Label(lc, SWT.WRAP);
		installLabel.setText(
				"This will install a flavor of OpenEmbedded in your Eclipse workspace.  It is the " +
				"recommended option for new projects in Eclipse."
				);
		installLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		installButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		installButton.addSelectionListener(listener);
		
		useButton = new Button(top, SWT.RADIO | SWT.WRAP);
		useButton.setText("Use an existing local copy of OpenEmbedded.");
		lc = new Composite(top, SWT.None);
		lc.setLayout(new GridLayout(2, false));
		lc.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		spacer = new Label(lc, SWT.None);
		spacer.setText("    ");
		installLabel = new Label(lc, SWT.WRAP);
		installLabel.setText(
				"A working install " +
				"of a flavor of OpenEmbedded is required.  An init script will need to be selected to initialize " +
				"the environment.");		
		installLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		useButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		useButton.addSelectionListener(listener);
	
		setControl(top);
	}

	@Override
	public void pageCleanup() {		
	}

	@Override
	public void pageDisplay() {
		setMessage("Choose to install a new instance or an existing one.");
	}

	@Override
	protected void updateModel() {
		model.remove(ACTION_INSTALL);
		model.remove(ACTION_USE);
		
		if (installButton.getSelection()) {
			model.put(ACTION_INSTALL, ACTION_INSTALL);
		} else if (useButton.getSelection()) {
			model.put(ACTION_USE, ACTION_USE);
		}
	}

	@Override
	protected boolean validatePage() {
		return useButton.getSelection() || installButton.getSelection();
	}
}

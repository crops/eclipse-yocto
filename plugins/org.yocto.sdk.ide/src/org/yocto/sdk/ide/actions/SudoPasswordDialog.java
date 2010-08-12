package org.yocto.sdk.ide.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SudoPasswordDialog extends Dialog {
  private Text passwordField;
  private String password;

  public SudoPasswordDialog(Shell parentShell) {
    super(parentShell);
  }

  protected Control createDialogArea(Composite parent) {
    Composite comp = (Composite) super.createDialogArea(parent);

    GridLayout layout = (GridLayout) comp.getLayout();
    layout.numColumns = 2;

    Label passwordLabel = new Label(comp, SWT.RIGHT);
    passwordLabel.setText("Sudo Password: ");

    passwordField = new Text(comp, SWT.SINGLE | SWT.PASSWORD);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    passwordField.setLayoutData(data);

    return comp;
  }

  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
  }

  protected void buttonPressed(int buttonId) {
	  if (buttonId == IDialogConstants.OK_ID) {
			password = passwordField.getText();
		}
		super.buttonPressed(buttonId);
  }
	
  public String getSudoPassword() {
	  return password;
  }
}

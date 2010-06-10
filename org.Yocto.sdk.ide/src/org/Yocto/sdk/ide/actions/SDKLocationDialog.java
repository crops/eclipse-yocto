package org.Yocto.sdk.ide.actions;

import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.Yocto.sdk.ide.YoctoSDKChecker;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;

public class SDKLocationDialog extends Dialog {
	private String title;
	private String sdk_location_name;
	private String sdk_location_value;
	private String toolchain_triplet_name;
	private String toolchain_triplet_value;
	private String sdk_location_ret_value = null;
	private String toolchain_triplet_ret_value = null;
	private String qemu_kernel_name;
	private String qemu_kernel_value;
	private String qemu_rootfs_name;
	private String qemu_rootfs_value;
	private String qemu_kernel_ret_value = null;
	private String qemu_rootfs_ret_value = null;
	private String env_script_name;
	private String env_script_value;
	private String env_script_ret_value = null;
	
	private Text location_value;
	private Text triplet_value;
	private Text kernel_value;
	private Text rootfs_value;
	private Text script_value;

	private Text errorMessageText;

	public SDKLocationDialog(Shell parentShell, String dialogTitle, String location_name, String location_value, 
							 String triplet_name, String triplet_value, String kernel_name, String kernel_value,
							 String rootfs_name, String rootfs_value, String script_name, String script_value, 
							 IInputValidator validator) {
        super(parentShell);
        this.sdk_location_name  = location_name;
        this.sdk_location_value = location_value;
        this.toolchain_triplet_name   = triplet_name;
        this.toolchain_triplet_value  = triplet_value;
        this.qemu_kernel_name = kernel_name;
        this.qemu_kernel_value = kernel_value;
        this.qemu_rootfs_name = rootfs_name;
        this.qemu_rootfs_value = rootfs_value;
        this.env_script_name = script_name;
        this.env_script_value = script_value;
        
        this.title = dialogTitle;
        setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		point.x = 640;
		return point;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		Label location_label = new Label(composite, SWT.LEFT);
		location_label.setText(sdk_location_name);
		location_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		location_label.setFont(parent.getFont());
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		Composite textConatiner1 = new Composite(composite, SWT.NONE);
		textConatiner1.setLayout(new GridLayout(2, false));
		textConatiner1.setLayoutData(data);

		//Label location_label = new Label(textConatiner1, SWT.LEFT);
		//location_label.setText(toolchain_location_name);
		//location_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		//location_label.setFont(parent.getFont());
		location_value = new Text(textConatiner1, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		location_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		location_value.setText(sdk_location_value);
		location_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Button button1 = new Button(textConatiner1, SWT.PUSH | SWT.LEAD);
		button1.setText(InputUIElement.BROWSELABEL);
		button1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new DirectoryDialog(composite.getShell()).open();
				if (dirName != null) {
					location_value.setText(dirName);
				}
			}
		});

		Label triplet_label = new Label(composite, SWT.LEAD);
		triplet_label.setText(toolchain_triplet_name);
		triplet_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		triplet_label.setFont(parent.getFont());

		Composite textConatiner2 = new Composite(composite, SWT.NONE);
		textConatiner2.setLayout(new GridLayout(1, false));
		textConatiner2.setLayoutData(data);
		//Label triplet_label = new Label(textConatiner2, SWT.LEFT);
		//triplet_label.setText(toolchain_triplet_name);
		//triplet_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		//triplet_label.setFont(parent.getFont());
		//textConatiner2.setLayoutData(new GridData());
		triplet_value = new Text(textConatiner2, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		triplet_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		triplet_value.setText(toolchain_triplet_value);
		triplet_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Label kernel_label = new Label(composite, SWT.LEAD);
		kernel_label.setText(qemu_kernel_name);
		kernel_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		kernel_label.setFont(parent.getFont());
		
		GridData data2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data2.widthHint = 100;
		Composite textConatiner3 = new Composite(composite, SWT.NONE);
		textConatiner3.setLayout(new GridLayout(2, false));
		textConatiner3.setLayoutData(data2);
		//Label kernel_label = new Label(textConatiner3, SWT.LEFT);
		//kernel_label.setText(qemu_kernel_name);
		//kernel_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		//kernel_label.setFont(parent.getFont());
		kernel_value = new Text(textConatiner3, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		kernel_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		kernel_value.setText(qemu_kernel_value);
		kernel_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Button button2 = new Button(textConatiner3, SWT.PUSH | SWT.LEAD);
		button2.setText(InputUIElement.BROWSELABEL);
		button2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new FileDialog(composite.getShell()).open();
				if (dirName != null) {
					kernel_value.setText(dirName);
				}
			}
		});
		
		Label rootfs_label = new Label(composite, SWT.LEAD);
		rootfs_label.setText(qemu_rootfs_name);
		rootfs_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		rootfs_label.setFont(parent.getFont());
		
		//GridData data3 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		//data3.widthHint = 100;
		Composite textConatiner4 = new Composite(composite, SWT.NONE);
		textConatiner4.setLayout(new GridLayout(2, false));
		textConatiner4.setLayoutData(data);

		rootfs_value = new Text(textConatiner4, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		rootfs_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		rootfs_value.setText(qemu_rootfs_value);
		rootfs_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Button button3 = new Button(textConatiner4, SWT.PUSH | SWT.LEAD);
		button3.setText(InputUIElement.BROWSELABEL);
		button3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new FileDialog(composite.getShell()).open();
				if (dirName != null) {
					rootfs_value.setText(dirName);
				}
			}
		});
		
		Label script_label = new Label(composite, SWT.LEAD);
		script_label.setText(env_script_name);
		script_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		script_label.setFont(parent.getFont());
		
		//GridData data3 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		//data3.widthHint = 100;
		Composite textConatiner5 = new Composite(composite, SWT.NONE);
		textConatiner5.setLayout(new GridLayout(2, false));
		textConatiner5.setLayoutData(data);

		script_value = new Text(textConatiner5, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		script_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		script_value.setText(env_script_value);
		script_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Button button4 = new Button(textConatiner5, SWT.PUSH | SWT.LEAD);
		button4.setText(InputUIElement.BROWSELABEL);
		button4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new FileDialog(composite.getShell()).open();
				if (dirName != null) {
					script_value.setText(dirName);
				}
			}
		});
		
		Composite textConatiner6 = new Composite(composite, SWT.NONE);
		textConatiner6.setLayout(new GridLayout(1, false));
		textConatiner6.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		errorMessageText = new Text(textConatiner5, SWT.READ_ONLY);
		errorMessageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        setErrorMessage(null);

		return composite;
	}

	protected void validateInput() {
        String errorMessage = null;
        String sdk_location = location_value.getText();
        String toolchain_triplet  = triplet_value.getText();
        String qemu_kernel = kernel_value.getText();
        String qemu_rootfs = rootfs_value.getText();
        String env_script = script_value.getText();
		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdk_location, 
															toolchain_triplet,
															qemu_kernel,
															qemu_rootfs,
															env_script);
		if (result != SDKCheckResults.SDK_PASS) {
			errorMessage = YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Menu);
		}
        setErrorMessage(errorMessage);
    }

    public void setErrorMessage(String errorMessage) {
    	if (errorMessageText != null && !errorMessageText.isDisposed()) {
    		errorMessageText.setText(errorMessage == null ? " \n " : errorMessage);
    		boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
    		errorMessageText.setEnabled(hasError);
    		errorMessageText.setVisible(hasError);
    		errorMessageText.getParent().update();
    		Control button = getButton(IDialogConstants.OK_ID);
    		if (button != null) {
    			button.setEnabled(errorMessage == null);
    		}
    	}
    }

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			sdk_location_ret_value = location_value.getText();
			toolchain_triplet_ret_value  = triplet_value.getText();
			qemu_kernel_ret_value = kernel_value.getText();
			qemu_rootfs_ret_value = rootfs_value.getText();
			env_script_ret_value = script_value.getText();
		}
		super.buttonPressed(buttonId);
	}
	
	public String getSDKLocation() {
		return sdk_location_ret_value;
	}

	public String getToolchainTriplet() {
		return toolchain_triplet_ret_value;
	}
	
	public String getQEMUKernel() {
		return qemu_kernel_ret_value;
	}
	
	public String getQEMURootfs() {
		return qemu_rootfs_ret_value;
	}
	
	public String getEnvScript() {
		return env_script_ret_value;
	}
	
}

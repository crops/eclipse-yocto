package org.Yocto.sdk.ide.actions;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.Yocto.sdk.ide.YoctoSDKChecker;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.Yocto.sdk.ide.preferences.PreferenceConstants;
import org.Yocto.sdk.ide.preferences.YoctoSDKPreferencePage;

public class SDKLocationDialog extends Dialog {
	private String title;
	private String sdkroot_name;
	private String sdkroot_value;
	private String sdkroot_ret_value;
	private String toolchain_name;
	private String toolchain_value;
	private String toolchain_ret_value = null;
	private String target_name;
	private String target_value;
	private String target_ret_value = null;
	private String target_qemu_name;
	private String target_qemu_value;
	private String target_qemu_ret_value;
	private String qemu_kernel_name;
	private String qemu_kernel_value;
	//private String qemu_rootfs_name;
	//private String qemu_rootfs_value;
	private String qemu_kernel_ret_value = null;
	//private String qemu_rootfs_ret_value = null;

	private String ip_addr_name;
	private String ip_addr_value;
	private String ip_addr_ret_value = null;
	
	private Text root_value;
	private Text kernel_value;
	//private Text rootfs_value;
	private Text ip_value;

	private Text errorMessageText;

	private ArrayList<Button> fRadioButtons;
	private ArrayList<Text> fTextControls;

	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;

	private Combo targetArchCombo;

	private Button SDKRootButton;
	private Button PokyRootButton;
	private Button QEMUButton;
	private Button RealHWButton;
	private Button kernel_button;
	//private Button rootfs_button;
	
	private Button root_button;

	private Label root_label;
	private Label kernel_label;
	//private Label rootfs_label;
	private Label ip_label;
	

	public SDKLocationDialog(Shell parentShell, String dialogTitle, String sdkroot_name, String sdkroot_value,
							String location_name, String location_value, 
							String target_name, String target_value,
							String targetQemu_name, String targetQemu_value,
							 String kernel_name, String kernel_value,
							 //String rootfs_name, String rootfs_value, 
							 String script_name, String script_value, String ip_name, String ip_value,IInputValidator validator) {
        super(parentShell);
        this.sdkroot_name = sdkroot_name;
        this.sdkroot_value = sdkroot_value;
        this.toolchain_name  = location_name;
        this.toolchain_value = location_value;
        this.target_name   = target_name;
        this.target_value  = target_value;
        this.target_qemu_name = targetQemu_name;
        this.target_qemu_value = targetQemu_value;
        this.qemu_kernel_name = kernel_name;
        this.qemu_kernel_value = kernel_value;
        //this.qemu_rootfs_name = rootfs_name;
        //this.qemu_rootfs_value = rootfs_value;
        this.ip_addr_name = ip_name;
        this.ip_addr_value = ip_value;
        
        this.title = dialogTitle;
        setShellStyle(getShellStyle() | SWT.RESIZE);
        
        fRadioButtons= new ArrayList<Button>();		
		fTextControls= new ArrayList<Text>();

		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		};

		fModifyListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlModified(e.widget);
			}
		};
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		point.x = 640;
		return point;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite result = (Composite) super.createDialogArea(parent);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		result.setLayout(layout);
		
		Group crossCompilerGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		crossCompilerGroup.setLayout(layout);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		crossCompilerGroup.setLayoutData(gd);
		crossCompilerGroup.setText("Cross Compiler Options:");
		
		if (sdkroot_value.equals(IPreferenceStore.TRUE)) {
			SDKRootButton= addRadioButton(crossCompilerGroup, "SDK", PreferenceConstants.TOOLCHAIN_ROOT, IPreferenceStore.TRUE);
			SDKRootButton.addSelectionListener(fSelectionListener);

			PokyRootButton = addRadioButton(crossCompilerGroup, "Poky", PreferenceConstants.TOOLCHAIN_ROOT, IPreferenceStore.FALSE);
			PokyRootButton.addSelectionListener(fSelectionListener);
		} else {
			SDKRootButton= addRadioButton(crossCompilerGroup, "SDK", PreferenceConstants.TOOLCHAIN_ROOT, IPreferenceStore.FALSE);
			SDKRootButton.addSelectionListener(fSelectionListener);

			PokyRootButton = addRadioButton(crossCompilerGroup, "Poky", PreferenceConstants.TOOLCHAIN_ROOT, IPreferenceStore.TRUE);
			PokyRootButton.addSelectionListener(fSelectionListener);
		}

		root_label = new Label(crossCompilerGroup, SWT.NONE);
		root_label.setText("Root Location: ");
		Composite textContainer = new Composite(crossCompilerGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		root_value = addTextControlText(textContainer, root_label, PreferenceConstants.TOOLCHAIN_ROOT, toolchain_value);
		root_value.addModifyListener(fModifyListener);
		root_button = addTextControlButton(textContainer, root_value, PreferenceConstants.TOOLCHAIN_ROOT);

		Label targetArchLabel= new Label(result, SWT.NONE);
		targetArchLabel.setText("Target Architecture: ");
		targetArchLabel.setLayoutData(new GridData());

		String[] targetList = YoctoSDKPreferencePage.getTargetList();
		targetArchCombo= new Combo(result, SWT.READ_ONLY);
		targetArchCombo.setItems(targetList);
		int index = getTargetIndex(targetList, target_value);
		targetArchCombo.select(index);
		targetArchCombo.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));

		//Target Options
		GridData gd2= new GridData(GridData.FILL_HORIZONTAL);
		gd2.horizontalSpan= 2;
		Group targetGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		targetGroup.setLayout(layout);
		targetGroup.setLayoutData(gd2);
		targetGroup.setText("Target Options:");

		if (target_qemu_value.equals(IPreferenceStore.TRUE)) {
			QEMUButton= addRadioButton(targetGroup, "QEMU", PreferenceConstants.TARGET_QEMU, IPreferenceStore.TRUE);
			QEMUButton.addSelectionListener(fSelectionListener);
		} else {
			QEMUButton= addRadioButton(targetGroup, "QEMU", PreferenceConstants.TARGET_QEMU, IPreferenceStore.FALSE);
			QEMUButton.addSelectionListener(fSelectionListener);
		}
		
		//QEMU Setup
		kernel_label= new Label(targetGroup, SWT.NONE);
		kernel_label.setText("Kernel: ");
		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		kernel_value= addTextControlText(textContainer, kernel_label, PreferenceConstants.QEMU_KERNEL,qemu_kernel_value);
		kernel_value.addModifyListener(fModifyListener);
		kernel_button = addTextControlButton(textContainer, kernel_value, PreferenceConstants.QEMU_KERNEL);

		/*
		rootfs_label= new Label(targetGroup, SWT.NONE);
		rootfs_label.setText("Root Filesystem: ");
		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rootfs_value= addTextControlText(textContainer, rootfs_label, PreferenceConstants.QEMU_ROOTFS, qemu_rootfs_value);
		rootfs_value.addModifyListener(fModifyListener);
		rootfs_button = addTextControlButton(textContainer, rootfs_value, PreferenceConstants.QEMU_ROOTFS);
		*/
		if (target_qemu_value.equals(IPreferenceStore.TRUE)) {
			RealHWButton = addRadioButton(targetGroup, "External HW", PreferenceConstants.TARGET_QEMU, IPreferenceStore.FALSE);
			RealHWButton.addSelectionListener(fSelectionListener);
		} else {
			RealHWButton = addRadioButton(targetGroup, "External HW", PreferenceConstants.TARGET_QEMU, IPreferenceStore.TRUE);
			RealHWButton.addSelectionListener(fSelectionListener);
		}
		textContainer = new Composite(parent, SWT.NONE);
		textContainer.setLayout(new GridLayout(1, false));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		textContainer.setLayoutData(data);
		ip_label= new Label(targetGroup, SWT.NONE);
		ip_label.setText("IP Address: ");
		ip_value= addTextControlText(targetGroup, ip_label, PreferenceConstants.IP_ADDR, ip_addr_value);
		ip_value.addModifyListener(fModifyListener);
		
		

		Composite textConatiner6 = new Composite(result, SWT.NONE);
		textConatiner6.setLayout(new GridLayout(1, false));
		textConatiner6.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		errorMessageText = new Text(textConatiner6, SWT.READ_ONLY);
		errorMessageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        setErrorMessage(null);
        validateInput();
		return result;
	}

	private Button addRadioButton(Composite parent, String label, String key, String value) {
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;

		Button button= new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setData(new String[] { key, value });
		button.setLayoutData(gd);

		button.setSelection(value.equals(IPreferenceStore.TRUE));

		fRadioButtons.add(button);
		return button;
	}
	
	private Text addTextControlText(final Composite parent, Label labelControl, final String key, String value) {
		final Text text;
			
		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setData(key);
		text.setText(value);

		fTextControls.add(text);
		return text;
	}
	
	private Button addTextControlButton(final Composite parent, final Text text, final String key) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText(InputUIElement.BROWSELABEL);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName;
				if (key.equals(PreferenceConstants.TOOLCHAIN_ROOT))
					dirName = new DirectoryDialog(parent.getShell()).open();
				else
					dirName = new FileDialog(parent.getShell()).open();
				if (dirName != null) {
					text.setText(dirName);
				}
			}
		});
		return button;
	}
	
	protected boolean validateInput() {
  		String sdkroot;
		String target_qemu;
		if (SDKRootButton.getSelection()) 
			sdkroot = IPreferenceStore.TRUE;
		else
			sdkroot = IPreferenceStore.FALSE;
		
		boolean qemuSelection = QEMUButton.getSelection();

		if (qemuSelection)
			target_qemu = IPreferenceStore.TRUE;
		else
			target_qemu = IPreferenceStore.FALSE;
		
		kernel_value.setEnabled(qemuSelection);
		//rootfs_value.setEnabled(qemuSelection);
		//rootfs_value.setEnabled(qemuSelection);
		//rootfs_button.setEnabled(qemuSelection);
		
		ip_value.setEnabled(!qemuSelection);
		
        String errorMessage = null;
        String toolchain_location = root_value.getText();
        String target  = targetArchCombo.getText();
        
        String qemu_kernel = null;
        //String qemu_rootfs = null;
        String ip_addr = null;
        if (qemuSelection) {
        	qemu_kernel = kernel_value.getText();
        	//qemu_rootfs = rootfs_value.getText();
        } else 
        	ip_addr = ip_value.getText();
        
		/*SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, 
															toolchain_location, 
															target,
															target_qemu,
															qemu_kernel,
															qemu_rootfs,
															ip_addr);
															*/
        SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, 
				toolchain_location, 
				target,
				target_qemu,
				qemu_kernel,
				ip_addr);
		boolean pass = true;
		if (result != SDKCheckResults.SDK_PASS) {
			errorMessage = YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Menu);
			pass = false;
		}
        setErrorMessage(errorMessage);
        return pass;

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
			if (!validateInput()) {
				Button ok_button = getButton(IDialogConstants.OK_ID);
				ok_button.setEnabled(true);
				return;
			}
			if (SDKRootButton.getSelection())
				sdkroot_ret_value = IPreferenceStore.TRUE;
			else
				sdkroot_ret_value = IPreferenceStore.FALSE;
			toolchain_ret_value = root_value.getText();
			target_ret_value  = targetArchCombo.getText();
			if (QEMUButton.getSelection())
				target_qemu_ret_value = IPreferenceStore.TRUE;
			else 
				target_qemu_ret_value = IPreferenceStore.FALSE;
			qemu_kernel_ret_value = kernel_value.getText();
			//qemu_rootfs_ret_value = rootfs_value.getText();
			ip_addr_ret_value = ip_value.getText();
		}
		super.buttonPressed(buttonId);
	}
	
	public String getSDKRoot() {
		return sdkroot_ret_value;
	}
	
	public String getToolchainLocation() {
		return toolchain_ret_value;
	}

	public String getTarget() {
		return target_ret_value;
	}
	
	public String getTargetQemu() {
		return target_qemu_ret_value;
	}
	
	public String getQEMUKernel() {
		return qemu_kernel_ret_value;
	}
	/*
	public String getQEMURootfs() {
		return qemu_rootfs_ret_value;
	}
	*/
	public String getIPAddr() {
		return ip_addr_ret_value;
	}
	
	private void controlChanged(Widget widget) {
		validateInput();
	}

	private void controlModified(Widget widget) {
		validateInput();
	}
	
	private int getTargetIndex(String[] targetList, String target) {
		for (int i = 0; i < targetList.length; i++) {
			String value = targetList[i];
			if (value.equals(target)) {
				return i;
			}
		}
		return -1;
	}
}

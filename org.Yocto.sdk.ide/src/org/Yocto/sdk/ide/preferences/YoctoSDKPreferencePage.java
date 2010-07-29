package org.Yocto.sdk.ide.preferences;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.cdt.utils.spawner.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.cdt.internal.core.ProcessClosure;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.Yocto.sdk.ide.YoctoSDKChecker;
import org.Yocto.sdk.ide.YoctoSDKMessages;
import org.Yocto.sdk.ide.YoctoSDKPlugin;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;


public class YoctoSDKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private static final String PREFERENCES_Yocto_CONFIG  = "Preferences.Yocto.Config.Name";
	private static final String PREFERENCES_SDK_TARGET = "Preference.SDK.Target.Name";
    private static final String PREFERENCES_INFORM_TITLE   = "Preferences.SDK.Informing.Title";
    private static final String PREFERENCES_INFORM_MESSAGE = "Preferences.SDK.Informing.Message";
    private static final String PREFERENCES_TOOLCHAIN_ROOT = "Preferences.SDK.Root.Name";
    private static final String PREFERENCES_TOOLCHAIN_TRIPLET  = "Preferences.Toolchain.Triplet.Name";
    private static final String PREFERENCES_QEMU_KERNEL = "Preferences.QEMU.Kernel.Name";
    private static final String PREFERENCES_QEMU_ROOTFS = "Preferences.QEMU.ROOTFS.Name";
    private static final String PREFERENCES_SETUP_ENV_SCRIPT = "Preferences.SetupEnv.Script.Name";
    private static final String PREFERENCES_CROSS_COMPILER_ROOT_LOC = "Preferences.Cross.Compiler.Root.Location";
    private static final String PREFERENCES_TARGET_SELECTION = "Preferences.Target.Selection";
    
	private ArrayList<Button> fRadioButtons;
	private ArrayList<Text> fTextControls;

	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;

	private Text fBinFolderNameText;
	private Text fSrcFolderNameText;

	private Combo targetArchCombo;

	private Button SDKRootButton;
	private Button PokyRootButton;
	private Button QEMUButton;
	private Button RealHWButton;
	private Button kernel_button;
	private Button rootfs_button;
	private Button root_button;

	private Label root_label;
	private Label kernel_label;
	private Label rootfs_label;
	private Label ip_label;
	
	private Text root_value;
	private Text kernel_value;
	private Text rootfs_value;
	private Text ip_value;

	public YoctoSDKPreferencePage() {
		//super(GRID);
        setPreferenceStore(YoctoSDKPlugin.getDefault().getPreferenceStore());
        //setDescription(YoctoSDKMessages.getString(PREFERENCES_Yocto_CONFIG));

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
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);

		final Composite result= new Composite(parent, SWT.NONE);
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

		SDKRootButton= addRadioButton(crossCompilerGroup, "SDK", PreferenceConstants.SDK_ROOT, IPreferenceStore.TRUE);
		SDKRootButton.addSelectionListener(fSelectionListener);

		PokyRootButton = addRadioButton(crossCompilerGroup, "Poky", PreferenceConstants.SDK_ROOT, IPreferenceStore.FALSE);
		PokyRootButton.addSelectionListener(fSelectionListener);

		root_label = new Label(crossCompilerGroup, SWT.NONE);
		root_label.setText("Root Location: ");
		Composite textContainer = new Composite(crossCompilerGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		root_value = addTextControlText(textContainer, root_label, PreferenceConstants.TOOLCHAIN_ROOT);
		root_value.addModifyListener(fModifyListener);
		root_button = addTextControlButton(textContainer, root_value, PreferenceConstants.TOOLCHAIN_ROOT);
		
		Label targetArchLabel= new Label(result, SWT.NONE);
		targetArchLabel.setText("Target Architecture: ");
		targetArchLabel.setLayoutData(new GridData());

		int index= getPreferenceStore().getInt(PreferenceConstants.TARGET_ARCH_INDEX);
		targetArchCombo= new Combo(result, SWT.READ_ONLY);
		targetArchCombo.setItems(getTargetList());
		targetArchCombo.select(index);
		targetArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//Target Options
		GridData gd2= new GridData(GridData.FILL_HORIZONTAL);
		gd2.horizontalSpan= 2;
		Group targetGroup= new Group(result, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		targetGroup.setLayout(layout);
		targetGroup.setLayoutData(gd2);
		targetGroup.setText("Target Options:");

		QEMUButton= addRadioButton(targetGroup, "QEMU", PreferenceConstants.TARGET_QEMU, IPreferenceStore.TRUE);
		QEMUButton.addSelectionListener(fSelectionListener);
		
		//QEMU Setup
		kernel_label= new Label(targetGroup, SWT.NONE);
		kernel_label.setText("Kernel: ");
		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		kernel_value= addTextControlText(textContainer, kernel_label, PreferenceConstants.QEMU_KERNEL);
		kernel_value.addModifyListener(fModifyListener);
		kernel_button = addTextControlButton(textContainer, kernel_value, PreferenceConstants.QEMU_KERNEL);

		rootfs_label= new Label(targetGroup, SWT.NONE);
		rootfs_label.setText("Root Filesystem: ");
		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rootfs_value= addTextControlText(textContainer, rootfs_label, PreferenceConstants.QEMU_ROOTFS);
		rootfs_value.addModifyListener(fModifyListener);
		rootfs_button = addTextControlButton(textContainer, rootfs_value, PreferenceConstants.QEMU_ROOTFS);
	
		RealHWButton = addRadioButton(targetGroup, "External HW", PreferenceConstants.TARGET_QEMU, IPreferenceStore.FALSE);
		RealHWButton.addSelectionListener(fSelectionListener);
		
		textContainer = new Composite(parent, SWT.NONE);
		textContainer.setLayout(new GridLayout(1, false));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		textContainer.setLayoutData(data);
		ip_label= new Label(targetGroup, SWT.NONE);
		ip_label.setText("IP Address: ");
		ip_value= addTextControlText(targetGroup, ip_label, PreferenceConstants.IP_ADDR);
		ip_value.addModifyListener(fModifyListener);
		
		validateFolders();

		Dialog.applyDialogFont(result);
		return result;
	}
	
	private Button addRadioButton(Composite parent, String label, String key, String value) {
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
	
		Button button= new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setData(new String[] { key, value });
		button.setLayoutData(gd);

		button.setSelection(value.equals(getPreferenceStore().getString(key)));

		fRadioButtons.add(button);
		return button;
	}
	
	private boolean validateFolders() {
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
		rootfs_value.setEnabled(qemuSelection);
		rootfs_value.setEnabled(qemuSelection);
		rootfs_button.setEnabled(qemuSelection);
		
		ip_value.setEnabled(!qemuSelection);
		
        String errorMessage = null;
        String toolchain_location = root_value.getText();
        String target  = targetArchCombo.getText();
        
        String qemu_kernel = null;
        String qemu_rootfs = null;
        String ip_addr = null;
        if (qemuSelection) {
        	qemu_kernel = kernel_value.getText();
        	qemu_rootfs = rootfs_value.getText();
        } else 
        	ip_addr = ip_value.getText();
        //String env_script = script_value.getText();
        
		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, 
															toolchain_location, 
															target,
															target_qemu,
															qemu_kernel,
															qemu_rootfs,
															ip_addr);
		boolean pass = true;
		if (result != SDKCheckResults.SDK_PASS) {
			errorMessage = YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Preferences);
			pass = false;
		}
        setErrorMessage(errorMessage);
        return pass;
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		if (!validateFolders()) {
			return false;
		}
		IPreferenceStore store= YoctoSDKPlugin.getDefault().getPreferenceStore();
		
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			if (button.getSelection()) {
				String[] info= (String[]) button.getData();
				store.setValue(info[0], info[1]);
			}
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			store.setValue(key, text.getText());
		}

		if (targetArchCombo != null) {
			store.setValue(PreferenceConstants.TARGET_ARCH_INDEX, targetArchCombo.getSelectionIndex());
			store.setValue(PreferenceConstants.TARGET, targetArchCombo.getText());
		}
		return super.performOk();
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			String[] info= (String[]) button.getData();
			button.setSelection(info[1].equals(store.getDefaultString(info[0])));
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			text.setText(store.getDefaultString(key));
		}
		if (targetArchCombo != null) {
			targetArchCombo.select(store.getDefaultInt(PreferenceConstants.TARGET_ARCH_INDEX));
		}

		validateFolders();
		super.performDefaults();
	}

	private void controlChanged(Widget widget) {
		if (widget == QEMUButton || widget == RealHWButton) {
			validateFolders();
		}
	}

	private void controlModified(Widget widget) {
		if (widget == fSrcFolderNameText || widget == fBinFolderNameText) {
			validateFolders();
		}
	}
	
	private Text addTextControlText(final Composite parent, Label labelControl, final String key) {
		GridData gd= new GridData(SWT.END, SWT.LEFT, false, false);
	
		labelControl.setLayoutData(gd);

		final Text text;
			
		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setData(key);
		text.setText(getPreferenceStore().getString(key));

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
	
	public static String[] getTargetList() {
		
		File toolchain_file = new File("/opt/poky");
		String[] files = toolchain_file.list();
		final String env_script_prefix = "environment-setup-";
		ArrayList<String> targetList = new ArrayList<String>();
		
		for (int i = 0; i < files.length; i++) {
			String file_str = files[i];
			if (file_str.startsWith(env_script_prefix)) {
				String target_triplet_substr = file_str.substring(env_script_prefix.length());
				StringTokenizer str_tok = new StringTokenizer(target_triplet_substr, "-");
				while (str_tok.hasMoreElements()) {
					targetList.add(str_tok.nextToken());
					break;
				}
			}
		}
        	
        return (String[])targetList.toArray(new String[targetList.size()]);
	}
}

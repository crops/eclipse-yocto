package org.Yocto.sdk.ide.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;

import org.Yocto.sdk.ide.YoctoSDKChecker;
import org.Yocto.sdk.ide.YoctoSDKMessages;
import org.Yocto.sdk.ide.YoctoSDKPlugin;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;

public class YoctoSDKPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	private static final String PREFERENCES_Yocto_CONFIG  = "Preferences.Yocto.Config.Name";
	private static final String PREFERENCES_SDK_TARGET = "Preference.SDK.Target.Name";
    private static final String PREFERENCES_INFORM_TITLE   = "Preferences.SDK.Informing.Title";
    private static final String PREFERENCES_INFORM_MESSAGE = "Preferences.SDK.Informing.Message";
    private static final String PREFERENCES_SDK_ROOT = "Preferences.SDK.Root.Name";
    private static final String PREFERENCES_TOOLCHAIN_TRIPLET  = "Preferences.Toolchain.Triplet.Name";
    private static final String PREFERENCES_QEMU_KERNEL = "Preferences.QEMU.Kernel.Name";
    private static final String PREFERENCES_QEMU_ROOTFS = "Preferences.QEMU.ROOTFS.Name";
    private static final String PREFERENCES_SETUP_ENV_SCRIPT = "Preferences.SetupEnv.Script.Name";

    private DirectoryFieldEditor dirEditor;
    private StringFieldEditor strEditor;
    private FileFieldEditor fileEditor;

	public YoctoSDKPreferencePage() {
		super(GRID);
        setPreferenceStore(YoctoSDKPlugin.getDefault().getPreferenceStore());
        //setDescription(YoctoSDKMessages.getString(PREFERENCES_Yocto_CONFIG));
	}

	public YoctoSDKPreferencePage(int style) {
		super(style);
		// TODO Auto-generated constructor stub
	}

	public YoctoSDKPreferencePage(String title, int style) {
		super(title, style);
		// TODO Auto-generated constructor stub
	}

	public YoctoSDKPreferencePage(String title, ImageDescriptor image, int style) {
		super(title, image, style);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createFieldEditors() {
		dirEditor = new DirectoryFieldEditor(PreferenceConstants.SDK_LOCATION,
		        YoctoSDKMessages.getString(PREFERENCES_SDK_ROOT), getFieldEditorParent());
		dirEditor.setEmptyStringAllowed(false);
		addField(dirEditor);
		
		strEditor = new StringFieldEditor(PreferenceConstants.TOOLCHAIN_TRIPLET,
                YoctoSDKMessages.getString(PREFERENCES_TOOLCHAIN_TRIPLET), getFieldEditorParent());
		strEditor.setEmptyStringAllowed(false);
		addField(strEditor);
		
		fileEditor = new FileFieldEditor(PreferenceConstants.QEMU_KERNEL,
		        YoctoSDKMessages.getString(PREFERENCES_QEMU_KERNEL), getFieldEditorParent());
		fileEditor.setEmptyStringAllowed(true);
		addField(fileEditor);
		
		fileEditor = new FileFieldEditor(PreferenceConstants.QEMU_ROOTFS,
		        YoctoSDKMessages.getString(PREFERENCES_QEMU_ROOTFS), getFieldEditorParent());
		fileEditor.setEmptyStringAllowed(true);
		addField(fileEditor);
		
		fileEditor = new FileFieldEditor(PreferenceConstants.SETUP_ENV_SCRIPT,
		        YoctoSDKMessages.getString(PREFERENCES_SETUP_ENV_SCRIPT), getFieldEditorParent());
		fileEditor.setEmptyStringAllowed(true);
		addField(fileEditor);
	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

}

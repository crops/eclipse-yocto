/*******************************************************************************
 * Copyright (c) 2010 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.ide;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckResults;
import org.yocto.sdk.ide.preferences.PreferenceConstants;

public class YoctoUISetting {

	private static final String ENV_SCRIPT_FILE_PREFIX = "environment-setup-";
	private ArrayList<Control> fControls;

	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;
	private YoctoUIElement yoctoUIElement;

	private Button btnSDKRoot;
	private Button btnQemu;
	private Button btnPokyRoot;
	private Button btnDevice;

	private Button btnKernelLoc;
	private Button btnSysrootLoc;
	private Button btnToolChainLoc;

	private Text textKernelLoc;
	private Text textQemuOption;
	private Text textSysrootLoc;
	private Text textRootLoc;
	private Combo targetArchCombo;

	public YoctoUISetting(YoctoUIElement elem)
	{
		fControls = new ArrayList<Control>();
		yoctoUIElement = elem;
		elem.setStrTargetsArray(getTargetArray(elem));

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

	private Control addControls(Control fControl, final String sKey, String sValue)
	{

		fControl.setData(new String[]{sKey,sValue});
		fControls.add(fControl);
		return fControl;

	}

	private Control addRadioButton(Composite parent, String label, String key, boolean bSelected) {
		GridData gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
		String sValue;
		Button button= new Button(parent, SWT.RADIO);

		gd.horizontalSpan= 2;
		button.setText(label);
		button.setLayoutData(gd);
		button.setSelection(bSelected);

		if (bSelected)
			sValue = IPreferenceStore.TRUE;
		else
			sValue = IPreferenceStore.FALSE;
		return addControls((Control)button, key, sValue);

	}

	private Control addTextControl(final Composite parent, String key, String value) {
		final Text text;

		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		text.setSize(10, 150);

		return addControls((Control)text, key, value);
	}

	private Button addFileSelectButton(final Composite parent, final Text text, final String key) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText(InputUIElement.BROWSELABEL);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName;
				if (key.equals(PreferenceConstants.TOOLCHAIN_ROOT)|| key.equals(PreferenceConstants.SYSROOT))
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

	private void createQemuSetup(final Group targetGroup) throws YoctoGeneralException
	{
		Label kernel_label;
		Label option_label;
		
		
		//QEMU Setup
		kernel_label= new Label(targetGroup, SWT.NONE);
		kernel_label.setText("Kernel: ");
		kernel_label.setAlignment(SWT.RIGHT);
		
		Composite textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		textKernelLoc= (Text)addTextControl(textContainer, PreferenceConstants.QEMU_KERNEL, yoctoUIElement.getStrQemuKernelLoc());
		btnKernelLoc = addFileSelectButton(textContainer, textKernelLoc, PreferenceConstants.QEMU_KERNEL);


		option_label = new Label(targetGroup, SWT.NONE);
		option_label.setText("Custom Option: ");
		option_label.setAlignment(SWT.RIGHT);
		
		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		textQemuOption = (Text)addTextControl(textContainer, PreferenceConstants.QEMU_OPTION, yoctoUIElement.getStrQemuOption());
		
/*
		rootfs_label= new Label(targetGroup, SWT.NONE);
		rootfs_label.setText("Root Filesystem: ");
		rootfs_label.setAlignment(SWT.RIGHT);

		textContainer = new Composite(targetGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		textRootFSLoc= (Text)addTextControl(textContainer, PreferenceConstants.QEMU_ROOTFS, yoctoUIElement.getStrQemuRootFSLoc());
		btnRootFSLoc = addFileSelectButton(textContainer, textRootFSLoc, PreferenceConstants.QEMU_ROOTFS);
		*/
		
	}
	public void createComposite(Composite composite) throws YoctoGeneralException
	{
		Label root_label, sysroot_label;
		
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		Group crossCompilerGroup= new Group(composite, SWT.NONE);
		layout= new GridLayout(2, false);
		crossCompilerGroup.setLayout(layout);
		gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan= 2;
		crossCompilerGroup.setLayoutData(gd);
		crossCompilerGroup.setText("Cross Compiler Options:");		

		if (yoctoUIElement.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {

			btnSDKRoot = (Button)addRadioButton(crossCompilerGroup, "Standalone pre-built toolchain", PreferenceConstants.SDK_MODE + "_1", true);
			btnPokyRoot = (Button)addRadioButton(crossCompilerGroup, "Build system derived toolchain", PreferenceConstants.SDK_MODE + "_2", false);
		}
		else {
			btnSDKRoot = (Button)addRadioButton(crossCompilerGroup, "Standalone pre-built toolchain", PreferenceConstants.SDK_MODE + "_1", false);
			btnPokyRoot = (Button)addRadioButton(crossCompilerGroup, "Build system derived toolchain", PreferenceConstants.SDK_MODE + "_2", true);
		}

		root_label = new Label(crossCompilerGroup, SWT.NONE);
		root_label.setText("Toolchain Root Location: ");
		Composite textContainer = new Composite(crossCompilerGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textRootLoc = (Text)addTextControl(textContainer,
				PreferenceConstants.TOOLCHAIN_ROOT, yoctoUIElement.getStrToolChainRoot());
		btnToolChainLoc = addFileSelectButton(textContainer, textRootLoc, PreferenceConstants.TOOLCHAIN_ROOT);
		
		sysroot_label= new Label(crossCompilerGroup, SWT.NONE);
		sysroot_label.setText("Sysroot Location: ");
		sysroot_label.setAlignment(SWT.RIGHT);

		textContainer = new Composite(crossCompilerGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		textSysrootLoc= (Text)addTextControl(textContainer, PreferenceConstants.SYSROOT, yoctoUIElement.getStrSysrootLoc());
		btnSysrootLoc = addFileSelectButton(textContainer, textSysrootLoc, PreferenceConstants.SYSROOT);

		updateSDKControlState();
		Label targetArchLabel= new Label(crossCompilerGroup, SWT.NONE);
		targetArchLabel.setText("Target Architecture: ");

		targetArchCombo= new Combo(crossCompilerGroup, SWT.READ_ONLY);
		if (yoctoUIElement.getStrTargetsArray() != null)
		{
			targetArchCombo.setItems(yoctoUIElement.getStrTargetsArray());
			targetArchCombo.select(yoctoUIElement.getIntTargetIndex());
		}
		targetArchCombo.setLayout(new GridLayout(2, false));
		targetArchCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		this.addControls((Control)targetArchCombo,
				PreferenceConstants.TOOLCHAIN_TRIPLET, String.valueOf(yoctoUIElement.getIntTargetIndex()));


		//Target Options
		GridData gd2= new GridData(SWT.FILL, SWT.LEFT, true, false);
		gd2.horizontalSpan= 2;
		Group targetGroup= new Group(composite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		targetGroup.setLayout(layout);
		targetGroup.setLayoutData(gd2);
		targetGroup.setText("Target Options:");

		if (yoctoUIElement.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
			btnQemu = (Button)addRadioButton(targetGroup, "QEMU", PreferenceConstants.TARGET_MODE + "_1", true);
			createQemuSetup(targetGroup);
			
			btnDevice = (Button)addRadioButton(targetGroup, "External HW", PreferenceConstants.TARGET_MODE + "_2", false);
		}
		else {
			btnQemu = (Button)addRadioButton(targetGroup, "QEMU", PreferenceConstants.TARGET_MODE + "_1", false);
			createQemuSetup(targetGroup);			

			btnDevice = (Button)addRadioButton(targetGroup, "External HW", PreferenceConstants.TARGET_MODE + "_2", true);
		}
		
		updateQemuControlState();

		//we add the listener at the end for avoiding the useless event trigger when control
		//changed or modified.
		btnSDKRoot.addSelectionListener(fSelectionListener);
		btnPokyRoot.addSelectionListener(fSelectionListener);
		btnQemu.addSelectionListener(fSelectionListener);
		btnDevice.addSelectionListener(fSelectionListener);
		textRootLoc.addModifyListener(fModifyListener);
		textKernelLoc.addModifyListener(fModifyListener);
		textQemuOption.addModifyListener(fModifyListener);
		textSysrootLoc.addModifyListener(fModifyListener);
	}

	//Load all Control values into the YoctoUIElement
	public YoctoUIElement getCurrentInput()
	{
		YoctoUIElement elem = new YoctoUIElement();
		if (btnSDKRoot.getSelection())
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_SDK_MODE);
		else
			elem.setEnumPokyMode(YoctoUIElement.PokyMode.POKY_TREE_MODE);

		if (btnQemu.getSelection())
		{
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.QEMU_MODE);
		}
		else {
			elem.setEnumDeviceMode(YoctoUIElement.DeviceMode.DEVICE_MODE);
		}
		elem.setStrToolChainRoot(textRootLoc.getText());
		elem.setIntTargetIndex(targetArchCombo.getSelectionIndex());
		elem.setStrTargetsArray(targetArchCombo.getItems());
		elem.setStrTarget(targetArchCombo.getText());
		elem.setStrQemuKernelLoc(textKernelLoc.getText());
		elem.setStrQemuOption(textQemuOption.getText());
		elem.setStrSysrootLoc(textSysrootLoc.getText());
		return elem;
	}

	public boolean validateInput(SDKCheckRequestFrom from, boolean bPrompt) throws YoctoGeneralException {
		YoctoUIElement elem = getCurrentInput();
		boolean pass = true;
		String strErrorMessage;

		SDKCheckResults result = YoctoSDKUtils.checkYoctoSDK(elem);

		//Show Error Message on the Label to help users.
		if (result != SDKCheckResults.SDK_PASS) {
			strErrorMessage = YoctoSDKUtils.getErrorMessage(result, from);
			pass = false;
			if (bPrompt)
			{
				Display display = Display.getCurrent();
				Shell shell = new Shell(display);
				MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				msgBox.setText("Yocto Project Configuration Error");
				msgBox.setMessage(strErrorMessage);
				msgBox.open();
				if (shell != null)
					shell.dispose();
			}

			throw new YoctoGeneralException(strErrorMessage);
		}
		return pass;
	}


	public void setfControls(ArrayList<Control> fControls) {
		this.fControls = fControls;
	}

	public ArrayList<Control> getfControls() {
		return fControls;
	}

	private void updateQemuControlState()
	{
		boolean bQemuMode = btnQemu.getSelection();

		textKernelLoc.setEnabled(bQemuMode);
		//textRootFSLoc.setEnabled(bQemuMode);
		btnKernelLoc.setEnabled(bQemuMode);
		//btnRootFSLoc.setEnabled(bQemuMode);
		textQemuOption.setEnabled(bQemuMode);
	}

	private void updateSDKControlState()
	{
		if (btnSDKRoot.getSelection())
		{
		}
		else {
			if (!yoctoUIElement.getStrToolChainRoot().startsWith("/opt/poky"))
			{
				textRootLoc.setText(yoctoUIElement.getStrToolChainRoot());
			}
			textRootLoc.setEnabled(true);
			btnToolChainLoc.setEnabled(true);
		}
	}

	private void controlChanged(Widget widget) {

		if (widget == btnSDKRoot || widget == btnPokyRoot)
		{

			setTargetCombo(targetArchCombo);
			updateSDKControlState();
		}

		if (widget == btnDevice || widget == btnQemu)
			updateQemuControlState();
	}

	private void controlModified(Widget widget) {
		if (widget == textRootLoc)
		{
			setTargetCombo(targetArchCombo);
		}
	}

	/* Search current supported Target triplet from the toolchain root
	 * by parsing ENV script file name
	 */
	private static ArrayList<String> getTargetTripletList(String strFileName)
	{
		File fFile = new File(strFileName);
		if (!fFile.exists())
			return null;

		ArrayList<String> arrTarget = new ArrayList<String>();
		String[] strFileArray = fFile.list();
		for (int i = 0; i < strFileArray.length; i++)
		{
			if (strFileArray[i].startsWith(ENV_SCRIPT_FILE_PREFIX)) {
				arrTarget.add(strFileArray[i].substring(ENV_SCRIPT_FILE_PREFIX.length()));
			}
		}
		return arrTarget;

	}

	private static String[] getTargetArray(YoctoUIElement elem)
	{
		ArrayList<String> arrTarget;
		String sEnvFilePath = elem.getStrToolChainRoot();

		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE)
		{
			arrTarget = getTargetTripletList(sEnvFilePath);
		}
		else
		{
			arrTarget = getTargetTripletList(sEnvFilePath + "/tmp");
		}
		if (arrTarget == null || arrTarget.size() <= 0)
			return null;

		String [] strTargetArray = new String[arrTarget.size()];
		for (int i = 0; i < arrTarget.size(); i++)
			strTargetArray[i] = arrTarget.get(i);

		return strTargetArray;

	}

	private void setTargetCombo(Combo targetCombo)
	{
		YoctoUIElement elem = getCurrentInput();
		//re-get Combo box items according to latest input!
		elem.setStrTargetsArray(getTargetArray(elem));
		targetCombo.removeAll();
		if (elem.getStrTargetsArray() != null)
		{
			targetCombo.setItems(elem.getStrTargetsArray());
			for (int i = 0; i < targetCombo.getItemCount(); i++)
			{
				if(elem.getStrTarget().equalsIgnoreCase(targetCombo.getItem(i)))
				{
					targetCombo.select(i);
					return;
				}
				if (elem.getStrTargetsArray().length == 1)
					targetCombo.select(0);
				targetCombo.select(-1);
			}
		}
		//prompt user,if he use tree mode, maybe he hasn't bitbake meta-ide-support yet.
		else if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_TREE_MODE)
		{
			if (elem.getStrToolChainRoot().isEmpty())
				return;
			else {
				File fToolChain = new File(elem.getStrToolChainRoot());
				if (!fToolChain.exists())
					return;
			}
		}

	}

}

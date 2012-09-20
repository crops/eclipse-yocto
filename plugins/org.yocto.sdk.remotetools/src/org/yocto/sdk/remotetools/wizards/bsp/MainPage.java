/*******************************************************************************
 * Copyright (c) 2012 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.sdk.remotetools.wizards.bsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.yocto.sdk.remotetools.YoctoBspElement;

/**
 *
 * Setting up the parameters for creating the new Yocto BSP
 *
 * @author jzhang
 */
public class MainPage extends WizardPage {
	public static  final String  PAGE_NAME = "Main";
	private static final String KARCH_CMD = "yocto-bsp list karch";
	private static final String QARCH_CMD = "yocto-bsp list qemu property qemuarch";
	private static final String BSP_SCRIPT = "yocto-bsp";
	private static final String PROPERTIES_CMD_PREFIX = "yocto-bsp list ";
	private static final String PROPERTIES_CMD_SURFIX = " properties -o ";
	private static final String PROPERTIES_FILE = "/tmp/properties.json";

	private Button btnMetadataLoc;
	private Text textMetadataLoc;
	private Label labelMetadata;

	private Button btnBspOutputLoc;
	private Text textBspOutputLoc;
	private Label labelBspOutput;

	private Button btnBuildLoc;
	private Text textBuildLoc;
	private Label labelBuildLoc;

	private boolean buildDirChecked;
	private BuildLocationListener buildLocationListener;

	private Text textBspName;
	private Label labelBspName;

	private Combo comboKArch;
	private Label labelKArch;

	private Combo comboQArch;
	private Label labelQArch;

	private YoctoBspElement bspElem;

	public MainPage(YoctoBspElement element) {
		super(PAGE_NAME, "yocto-bsp Main page", null);

		setMessage("Enter the required fields(with *) to create new Yocto Project BSP!");
		this.bspElem = element;
	}

	@Override
	public void createControl(Composite parent) {
		setErrorMessage(null);
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		composite.setLayout(layout);
		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);

		labelMetadata = new Label(composite, SWT.NONE);
		labelMetadata.setText("Metadata location*: ");
		Composite textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textMetadataLoc = (Text)addTextControl(textContainer, "");
		textMetadataLoc.setEnabled(false);
		textMetadataLoc.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		setBtnMetadataLoc(addFileSelectButton(textContainer, textMetadataLoc));

		labelBuildLoc = new Label(composite, SWT.NONE);
		labelBuildLoc.setText("Build location: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		textBuildLoc = (Text)addTextControl(textContainer, "");
		buildLocationListener = new BuildLocationListener("");
		textBuildLoc.addFocusListener(buildLocationListener);

		setBtnBuilddirLoc(addFileSelectButton(textContainer, textBuildLoc));

		labelBspName = new Label(composite, SWT.NONE);
		labelBspName.setText("BSP Name*: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		textBspName = (Text)addTextControl(textContainer, "");
		textBspName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});

		labelBspOutput = new Label(composite, SWT.NONE);
		labelBspOutput.setText("BSP output location: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		textBspOutputLoc = (Text)addTextControl(textContainer, "");
		textBspOutputLoc.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		setBtnBspOutLoc(addFileSelectButton(textContainer, textBspOutputLoc));

		labelKArch = new Label(composite, SWT.NONE);
		labelKArch.setText("Kernel Architecture*: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		comboKArch = new Combo(textContainer, SWT.READ_ONLY);
		comboKArch.setLayout(new GridLayout(2, false));
		comboKArch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comboKArch.setEnabled(false);
		comboKArch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});

		labelQArch = new Label(composite, SWT.NONE);
		labelQArch.setText("Qemu Architecture(* for karch as qemu): ");
		labelQArch.setEnabled(false);

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		comboQArch = new Combo(textContainer, SWT.READ_ONLY);
		comboQArch.setLayout(new GridLayout(2, false));
		comboQArch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comboQArch.setEnabled(false);
		comboQArch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});

		setControl(composite);
		validatePage();
	}

	private Control addTextControl(final Composite parent, String value) {
		final Text text;

		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		text.setSize(10, 150);

		return text;
	}

	private Button addFileSelectButton(final Composite parent, final Text text) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new DirectoryDialog(parent.getShell()).open();
				if (dirName != null) {
					text.setText(dirName);
				}
			}
		});
		return button;
	}

	private void controlChanged(Widget widget) {
		Status status = new Status(IStatus.OK, "not_used", 0, "", null);
		setErrorMessage(null);
		String metadataLoc = textMetadataLoc.getText();

		if (widget == textMetadataLoc) {
			resetKarchCombo();
			if (metadataLoc.length() == 0) {
				status = new Status(IStatus.ERROR, "not_used", 0, "Meta data location can't be empty!", null);
			} else {
				File meta_data = new File(metadataLoc);
				if (!meta_data.exists() || !meta_data.isDirectory()) {
					status = new Status(IStatus.ERROR, "not_used", 0,
							"Invalid meta data location: Make sure it exists and is a directory!", null);
				} else {
					File bspScript = new File(metadataLoc + "/scripts/" + BSP_SCRIPT);
					if (!bspScript.exists() || !bspScript.canExecute())
						status = new Status(IStatus.ERROR, "not_used", 0,
								"Make sure yocto-bsp exists under \"" + metadataLoc + "/scripts\" and is executable!", null);
					else {
						kernelArchesHandler();
					}
				}
			}
		} else if (widget == comboKArch) {
			String selection = comboKArch.getText();
			if (!bspElem.getKarch().contentEquals(selection))
				bspElem = new YoctoBspElement();
			if (selection.matches("qemu")) {
				labelQArch.setEnabled(true);
				comboQArch.setEnabled(true);
			} else {
				labelQArch.setEnabled(false);
				comboQArch.setEnabled(false);
			}
		}

		String buildDir = textBuildLoc.getText();
		String outputDir = textBspOutputLoc.getText();
		String bspName = textBspName.getText();

		if (!outputDir.isEmpty()){
			if (outputDir.matches(buildDir)) {
				status = new Status(IStatus.ERROR, "not_used", 0,
						"You've set BSP output directory the same as build directory, please leave output directory empty for this scenario!", null);
			} else {
				File outputDirectory = new File(outputDir);
				if (outputDirectory.exists()){
					status = new Status(IStatus.ERROR, "not_used", 0,
							"Your BSP output directory points to an exiting directory!", null);
				}
			}
		} else if (buildDir.startsWith(metadataLoc) && !bspName.isEmpty()) {
			String bspDirStr = metadataLoc + "/meta-" + bspName;
			File bspDir = new File(bspDirStr);
			if (bspDir.exists()) {
				status = new Status(IStatus.ERROR, "not_used", 0,
						"Your BSP with name: " + bspName + " already exist under directory: " + bspDirStr + ", please change your bsp name!", null);
			}
		}

		if (status.getSeverity() == IStatus.ERROR)
			setErrorMessage(status.getMessage());

		getWizard().getContainer().updateButtons();
		canFlipToNextPage();
	}

	private Status checkBuildDir() {

		String metadataLoc = textMetadataLoc.getText();
		String buildLoc = textBuildLoc.getText();

		if (buildLoc.isEmpty()) {
			buildLoc = metadataLoc + "/build";
			return createBuildDir(buildLoc);
		} else {
			File buildLocDir = new File(buildLoc);
			if (!buildLocDir.exists()) {
				return createBuildDir(buildLoc);
			} else if (buildLocDir.isDirectory()) {
				return createBuildDir(buildLoc);
			} else {
				return new Status(IStatus.ERROR, "not_used", 0, "Invalid build location: Make sure the build location is a directory!", null);
			}
		}
	}

	private Status createBuildDir(String buildLoc) {
		String metadataDir = textMetadataLoc.getText();

		// if we do  not change the directory to metadata location the script will be looked into the directory indicated by user.dir system property
		// system.property usually points to the location from where eclipse was started
		String createBuildDirCmd = "cd " + metadataDir + ";source " + metadataDir + "/oe-init-build-env " + buildLoc;

		try {
			ProcessBuilder builder = new ProcessBuilder(new String[] {"sh", "-c", createBuildDirCmd});
			Process proc = builder.start();
			InputStream errorStream = proc.getErrorStream();
			InputStreamReader isr = new InputStreamReader(errorStream);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			String status = "";
			while ( (line = br.readLine()) != null) {
				status += line;
			}

			if (proc.waitFor() != 0)
				return new Status(IStatus.ERROR, "not_used", 0, status, null);;
			return new Status(IStatus.OK, "not_used", 0, "", null);
		} catch (Exception e) {
			return  new Status(IStatus.ERROR, "not_used", 0, e.getMessage(), null);
		}
	}

	public YoctoBspElement getBSPElement() {
		return this.bspElem;
	}


	private void resetKarchCombo() {
		comboKArch.deselectAll();
		comboQArch.deselectAll();
		comboKArch.setEnabled(false);
		labelQArch.setEnabled(false);
		comboQArch.setEnabled(false);
	}

	private void kernelArchesHandler() {
		BSPAction kArchesAction = getKArches();
		if (kArchesAction.getMessage() == null && kArchesAction.getItems().length != 0) {
			comboKArch.setItems(kArchesAction.getItems());
			comboKArch.setEnabled(true);
		} else if (kArchesAction.getMessage() != null){
			setErrorMessage(kArchesAction.getMessage());
			return;
		}
		BSPAction qArchesAction = getQArches();
		if (qArchesAction.getMessage() == null && qArchesAction.getItems().length != 0) {
			comboQArch.setItems(qArchesAction.getItems());
		} else if (qArchesAction.getMessage() != null)
			setErrorMessage(qArchesAction.getMessage());

	}

	@Override
	public boolean canFlipToNextPage(){
		String err = getErrorMessage();
		if (err != null)
			return false;
		else if (!validatePage())
			return false;
		return true;
	}


	public boolean validatePage() {
		String metadataLoc = textMetadataLoc.getText();
		String bspname = textBspName.getText();
		String karch = comboKArch.getText();
		String qarch = comboQArch.getText();
		if (metadataLoc.isEmpty() ||
				bspname.isEmpty() ||
				karch.isEmpty()) {
			return false;
		} else if (karch.matches("qemu") && qarch.isEmpty()) {
			return false;
		}

		bspElem.setBspName(bspname);
		if (!textBspOutputLoc.getText().isEmpty())
			bspElem.setBspOutLoc(textBspOutputLoc.getText());
		else
			bspElem.setBspOutLoc("");
		if (!textBuildLoc.getText().isEmpty())
			bspElem.setBuildLoc(textBuildLoc.getText());
		else {
			bspElem.setBuildLoc(metadataLoc + "/build");
			if (!buildDirChecked) {
				checkBuildDir();
				buildDirChecked = true;
			}
		}
		bspElem.setMetadataLoc(metadataLoc);
		bspElem.setKarch(karch);
		bspElem.setQarch(qarch);


		if (!bspElem.getValidPropertiesFile()) {
			boolean validPropertiesFile = true;
			BSPAction action = createPropertiesFile();
			if (action.getMessage() != null) {
				validPropertiesFile = false;
				setErrorMessage(action.getMessage());
			}
			bspElem.setValidPropertiesFile(validPropertiesFile);
		}
		return true;
	}

	private BSPAction createPropertiesFile() {
		String createPropertiesCmd = bspElem.getMetadataLoc() + "/scripts/" +
				PROPERTIES_CMD_PREFIX + bspElem.getKarch() +
				PROPERTIES_CMD_SURFIX + PROPERTIES_FILE;
		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(),  new ErrorCollectorThread(createPropertiesCmd), "Creating properties file ");
		progressDialog.run(false);
		return progressDialog.getBspAction();
	}

	private BSPAction getKArches() {
		String getKArchCmd = textMetadataLoc.getText() + "/scripts/" + KARCH_CMD;
		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(), new KernelArchGetter(getKArchCmd), "Loading kernel architectures ");
		progressDialog.run(false);
		return progressDialog.getBspAction();
	}

	private BSPAction getQArches() {
		String getQArchCmd = textMetadataLoc.getText() + "/scripts/" + QARCH_CMD;
		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(), new QemuArchGetter(getQArchCmd), "Loading Qemu architectures ");
		progressDialog.run(false);
		return progressDialog.getBspAction();
	}

	public Button getBtnMetadataLoc() {
		return btnMetadataLoc;
	}

	public void setBtnMetadataLoc(Button btnMetadataLoc) {
		this.btnMetadataLoc = btnMetadataLoc;
	}

	public Button getBtnBspOutLoc() {
		return btnBspOutputLoc;
	}

	public void setBtnBspOutLoc(Button btnBspOutLoc) {
		this.btnBspOutputLoc = btnBspOutLoc;
	}

	public Button getBtnBuilddirLoc() {
		return btnBuildLoc;
	}

	public void setBtnBuilddirLoc(Button btnBuilddirLoc) {
		this.btnBuildLoc = btnBuilddirLoc;
	}

	class BuildLocationListener implements FocusListener{
		String value;
		boolean changed;

		BuildLocationListener(String value){
			this.value = value;
		}
		@Override
		public void focusGained(FocusEvent e) {
			value = ((Text)e.getSource()).getText();
		}

		@Override
		public void focusLost(FocusEvent e) {
			if(!((Text)e.getSource()).getText().equals(value)) {
				checkBuildDir();
				buildDirChecked = true;
			}
		}

	}
}

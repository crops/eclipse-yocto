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
import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Event;
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

	private Text textBspName;
	private Label labelBspName;

	private Combo comboKArch;
	private Label labelKArch;

	private Combo comboQArch;
	private Label labelQArch;

	private YoctoBspElement bspElem;

	public MainPage(YoctoBspElement element) {
		super(PAGE_NAME, "yocto-bsp main page", null);

		setMessage("Enter the required fields(with *) to create new Yocto Project BSP!");
		this.bspElem = element;
	}

	public void createControl(Composite parent) {
		setErrorMessage(null);
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		gd.horizontalSpan = 2;
		composite.setLayoutData(gd);	

		labelMetadata = new Label(composite, SWT.NONE);
		labelMetadata.setText("Meta_data location*: ");
		Composite textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textMetadataLoc = (Text)addTextControl(textContainer, "");
		textMetadataLoc.setEnabled(false);
		textMetadataLoc.addModifyListener(new ModifyListener() {
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
		textBuildLoc.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});

		setBtnBuilddirLoc(addFileSelectButton(textContainer, textBuildLoc));

		labelBspName = new Label(composite, SWT.NONE);
		labelBspName.setText("BSP Name*: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		textBspName = (Text)addTextControl(textContainer, "");
		textBspName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});

		labelBspOutput = new Label(composite, SWT.NONE);
		labelBspOutput.setText("Bsp output location: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		textBspOutputLoc = (Text)addTextControl(textContainer, "");
		textBspOutputLoc.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		setBtnBspOutLoc(addFileSelectButton(textContainer, textBspOutputLoc));

		labelKArch= new Label(composite, SWT.NONE);
		labelKArch.setText("kernel Architecture*: ");

		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		comboKArch= new Combo(textContainer, SWT.READ_ONLY);
		comboKArch.setLayout(new GridLayout(2, false));
		comboKArch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comboKArch.setEnabled(false);
		comboKArch.addModifyListener(new ModifyListener() {
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

		return (Control)text;
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

		 checkBuildDir();
		 
		 String build_dir = textBuildLoc.getText();
		 String output_dir = textBspOutputLoc.getText();
		 String bsp_name = textBspName.getText();
		 
		 if (!output_dir.isEmpty() && output_dir.matches(build_dir)) {
			 status = new Status(IStatus.ERROR, "not_used", 0,
					 "You've set BSP output directory the same as build directory, please leave output directory empty for this scenario!", null);
		 }
		 
		 if (build_dir.startsWith(metadataLoc) && output_dir.isEmpty() && !bsp_name.isEmpty()) {
			 String bsp_dir_str = metadataLoc + "/meta-" + bsp_name;
			 File bsp_dir = new File(bsp_dir_str);
			 if (bsp_dir.exists()) {
				 status = new Status(IStatus.ERROR, "not_used", 0,
						 "Your BSP with name: " + bsp_name + " already exist under directory: " + bsp_dir_str + ", please change your bsp name!", null);
			 }
		 }
		 validatePage();
		 
		 if (status.getSeverity() == IStatus.ERROR)
			 setErrorMessage(status.getMessage());
		 
		 getWizard().getContainer().updateButtons();
		 canFlipToNextPage();
	}

	private void checkBuildDir() {
		String metadata_dir = textMetadataLoc.getText();
		String builddir_str = textBuildLoc.getText();

		File build_dir = null;
		if ((builddir_str == null) || builddir_str.isEmpty()) 
			builddir_str = metadata_dir + "/build";

		build_dir = new File(builddir_str);

		if (!build_dir.exists()) {
			String create_builddir_cmd = metadata_dir + "/oe-init-build-env " + builddir_str;
			try {
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(new String[] {"sh", "-c", create_builddir_cmd});
				proc.waitFor();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public YoctoBspElement getBSPElement() {
		return this.bspElem;
	}

	public void handleEvent(Event event) {
		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
	}

	private void resetKarchCombo() {
		comboKArch.deselectAll();
		comboQArch.deselectAll();
		comboKArch.setEnabled(false);
		labelQArch.setEnabled(false);
		comboQArch.setEnabled(false);
	}

	private void kernelArchesHandler() {
		ArrayList<String> karches = getKArches();
		if (!karches.isEmpty()) {
			String[] kitems = new String[karches.size()];
			kitems = karches.toArray(kitems);
			comboKArch.setItems(kitems);
			comboKArch.setEnabled(true);
		}
		ArrayList<String> qarches = getQArches();
		if (!qarches.isEmpty()) {
			String[] qitems = new String[qarches.size()];
			qitems = qarches.toArray(qitems);
			comboQArch.setItems(qitems);
		}
	}

	public boolean canFlipToNextPage(){
		String err = getErrorMessage();
		if (err != null) 
			return false;
		else if (!validatePage())
			return false;
		return true;
	}


	public boolean validatePage() {
		String metadata_loc = textMetadataLoc.getText();
		String bspname = textBspName.getText();
		String karch = comboKArch.getText();
		String qarch = comboQArch.getText();
		if (metadata_loc.isEmpty() ||
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
		else
			bspElem.setBuildLoc("");
		bspElem.setMetadataLoc(metadata_loc);
		bspElem.setKarch(karch);
		bspElem.setQarch(qarch);
		bspElem.setValidPropertiesFile(createPropertiesFile());

		return true;
	}

	private boolean createPropertiesFile() {
		String create_properties_cmd = bspElem.getMetadataLoc() + "/scripts/" + 
										PROPERTIES_CMD_PREFIX + bspElem.getKarch() + 
										PROPERTIES_CMD_SURFIX + PROPERTIES_FILE;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(create_properties_cmd);
			int exitVal = proc.waitFor();
			if (exitVal != 0)
				return false;
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

	private ArrayList<String> getKArches() {
		ArrayList<String> karches = new ArrayList<String>();

		String get_karch_cmd = textMetadataLoc.getText() + "/scripts/" + KARCH_CMD;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(get_karch_cmd);
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while ( (line = br.readLine()) != null) {
				if (line.contains(":"))
					continue;
				line = line.replaceAll("^\\s+", "");
				line = line.replaceAll("\\s+$", "");
				karches.add(line);
			}

			proc.waitFor();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return karches;
	}

	private ArrayList<String> getQArches() {
		ArrayList<String> qarches = new ArrayList<String>();

		String get_qarch_cmd = textMetadataLoc.getText() + "/scripts/" + QARCH_CMD;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(get_qarch_cmd);
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			while ( (line = br.readLine()) != null) {
				if (!line.startsWith("["))
					continue;
				String[] values = line.split(",");

				String value = values[0];
				value = value.replace("[\"", "");
				value = value.replaceAll("\"$", "");
				qarches.add(value);
			}
			proc.waitFor();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return qarches;
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
}

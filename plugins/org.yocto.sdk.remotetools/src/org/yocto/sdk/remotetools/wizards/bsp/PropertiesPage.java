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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.yocto.sdk.remotetools.YoctoBspElement;
import org.yocto.sdk.remotetools.YoctoBspPropertyElement;
import org.yocto.sdk.remotetools.YoctoJSONHelper;
/**
 *
 * Setting up the parameters for creating the new Yocto BSP
 *
 * @author jzhang
 */
public class PropertiesPage extends WizardPage {
	private static final String PAGE_NAME = "Properties";
	private static final String VALUES_CMD_PREFIX = "yocto-bsp list ";
	private static final String VALUES_CMD_SURFIX = " property  ";
	private static final String KERNEL_CHOICE = "kernel_choice";
	private static final String DEFAULT_KERNEL = "use_default_kernel";
	private static final String SMP_NAME = "smp";
	private static final String EXISTING_KBRANCH_NAME = "existing_kbranch";
	private static final String NEED_NEW_KBRANCH_NAME = "need_new_kbranch";
	private static final String NEW_KBRANCH_NAME = "new_kbranch";
	private static final String QARCH_NAME = "qemuarch";

	private static final String KERNEL_CHOICES = "choices";
	private static final String KERNEL_BRANCHES = "branches";

	private Hashtable<YoctoBspPropertyElement, Control> propertyControlMap;
	HashSet<YoctoBspPropertyElement> properties;

	private ScrolledComposite composite;
	private Composite controlContainer = null;

	private YoctoBspElement bspElem = null;
	private boolean kArchChanged = false;

	private Combo kernelCombo;
	private Combo branchesCombo;

	private Button newBranchButton;
	private Button existingBranchButton;

	private Button smpButton;

	private Group kGroup = null;
	private Group kbGroup = null;
//	private Group otherSettingsGroup = null;
	private Group propertyGroup = null;

	public PropertiesPage(YoctoBspElement element) {
		super(PAGE_NAME, "yocto-bsp Properties page", null);
		this.bspElem = element;
	}

	public void onEnterPage(YoctoBspElement element) {
		if (!element.getValidPropertiesFile()) {
			setErrorMessage("There's no valid properties file created, please choose \"Back\" to reselect kernel architecture!");
			return;
		}

		if (this.bspElem == null || this.bspElem.getKarch().isEmpty() || !this.bspElem.getKarch().contentEquals(element.getKarch())) {
			kArchChanged = true;
		} else
			kArchChanged = false;

		this.bspElem = element;
		try {
			if (kArchChanged) {
				updateKernelValues(KERNEL_CHOICES, KERNEL_CHOICE);
				
				if (propertyGroup != null) {
					for (Control cntrl : propertyGroup.getChildren()) {
						cntrl.dispose();
					}
				}

				properties = YoctoJSONHelper.getProperties();

				if (!properties.isEmpty()) {

					if (!element.getQarch().isEmpty()) {
						YoctoBspPropertyElement qarch_elem = new YoctoBspPropertyElement();
						qarch_elem.setName(QARCH_NAME);
						qarch_elem.setValue(element.getQarch());
						properties.add(qarch_elem);
					}

					propertyControlMap = new Hashtable<YoctoBspPropertyElement, Control>();

					ArrayList<YoctoBspPropertyElement> propertiesList = new ArrayList<YoctoBspPropertyElement>(properties);
					Collections.sort(propertiesList, Collections.reverseOrder());

					Iterator<YoctoBspPropertyElement> it = propertiesList.iterator();
					Composite comp = new Composite(propertyGroup, SWT.FILL);
					GridLayout layout = new GridLayout(2, false);
					GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
					comp.setLayoutData(data);
					comp.setLayout(layout);

					while (it.hasNext()) {
						// Get property
						YoctoBspPropertyElement propElem = it.next();
						String type = propElem.getType();
						String name = propElem.getName();
						if (type.contentEquals("edit")) {
							new Label (propertyGroup, SWT.FILL).setText(name + ":");

							Composite textContainer = new Composite(propertyGroup, SWT.NONE);
							textContainer.setLayout(new GridLayout(1, false));
							textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
							Text text = new Text(textContainer, SWT.BORDER | SWT.SINGLE);
							text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
							propertyControlMap.put(propElem, text);

						} else if (type.contentEquals("boolean")) {
							String default_value = propElem.getDefaultValue();
							Composite labelContainer = new Composite(propertyGroup, SWT.NONE);
							labelContainer.setLayout(new GridLayout(2, false));
							labelContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.FILL_VERTICAL, true, false, 2, 1));
							Button button = new Button(propertyGroup, SWT.CHECK);
							button.setText(name);
							if (default_value.equalsIgnoreCase("y")) {
								button.setSelection(true);
							} else
								button.setSelection(false);
							propertyControlMap.put(propElem, button);
						} else if (type.contentEquals("choicelist")) {
							new Label (propertyGroup, SWT.NONE).setText(name + ":");

							Composite textContainer = new Composite(propertyGroup, SWT.NONE);
							textContainer.setLayout(new GridLayout(1, false));
							textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
							Combo combo = new Combo(textContainer, SWT.READ_ONLY);
							combo.setLayout(new GridLayout(2, false));
							combo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
							combo.setItems(getBSPComboProperties(name));
							propertyControlMap.put(propElem, combo);
						}
					}
				}
				composite.setMinSize(controlContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
				composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				controlContainer.pack();
				this.composite.layout(true, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	@Override
	public void createControl(Composite parent) {
		this.composite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		GridLayout layout = new GridLayout(2, true);
		this.composite.setLayout(layout);

		gd= new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		this.composite.setLayoutData(gd);

		setControl(this.composite);

		controlContainer = new Composite(composite, SWT.NONE);
		controlContainer.setLayout(new GridLayout(1, true));
		controlContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		kGroup = new Group(controlContainer, SWT.FILL);
		kGroup.setLayout(new GridLayout(2, false));
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		kGroup.setLayoutData(data);
		kGroup.setText("Kernel Settings:");

		new Label (kGroup, SWT.NONE).setText("Kernel:");
		Composite textContainer = new Composite(kGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(1, false));
		textContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));

		kernelCombo = new Combo(textContainer, SWT.READ_ONLY);
		kernelCombo.setLayout(new GridLayout(2, false));
		kernelCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		kernelCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		kbGroup = new Group(kGroup, SWT.FILL);
		kbGroup.setLayout(new GridLayout(2, true));
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalSpan = 2;
		kbGroup.setLayoutData(data);
		kbGroup.setText("Branch Settings:");

		textContainer = new Composite(kbGroup, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));
		
		new Label(textContainer, SWT.NONE).setText("Kernel branch:");
		
		branchesCombo = new Combo(textContainer, SWT.READ_ONLY);
		branchesCombo.setLayout(new GridLayout(1, false));
		branchesCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		branchesCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		branchesCombo.setSize(200, 200);
		
		newBranchButton = new Button(kbGroup, SWT.RADIO);
		newBranchButton.setText("Create a new branch from an existing one");
		newBranchButton.setSelection(true);
		newBranchButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		SelectionListener listener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}

			@Override
			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		};

		newBranchButton.addSelectionListener(listener);

		existingBranchButton = new Button(kbGroup, SWT.RADIO);
		existingBranchButton.setText("Use existing branch");
		existingBranchButton.setSelection(false);
		existingBranchButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		existingBranchButton.addSelectionListener(listener);

//		otherSettingsGroup = new Group(controlContainer, SWT.FILL);
//		otherSettingsGroup.setLayout(new GridLayout(2, true));
//		data = new GridData(SWT.FILL, SWT.FILL, true, false);
//		data.horizontalSpan = 2;
//		otherSettingsGroup.setLayoutData(data);
//		otherSettingsGroup.setText("Other Settings:");

		smpButton = new Button(kGroup, SWT.CHECK);
		smpButton.setText("Enable SMP support");
		smpButton.setSelection(true);
		
		propertyGroup = new Group(controlContainer, SWT.NONE);
		propertyGroup.setLayout(new GridLayout(2, false));
		data = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
		propertyGroup.setLayoutData(data);
		propertyGroup.setText("BSP specific settings:");

		this.composite.layout(true, true);

		composite.setContent(controlContainer);
		composite.setExpandHorizontal(true);
		composite.setExpandVertical(true);
		composite.setMinSize(controlContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		controlContainer.pack();
		composite.pack();
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	public HashSet<YoctoBspPropertyElement> getProperties() {
		String kcSelection = kernelCombo.getText();
		String kbSelection = branchesCombo.getText();
		YoctoBspPropertyElement kcElement = new YoctoBspPropertyElement();
		kcElement.setName(KERNEL_CHOICE);
		kcElement.setValue(kcSelection);
		properties.add(kcElement);
		YoctoBspPropertyElement defaultElement = new YoctoBspPropertyElement();
		defaultElement.setName(DEFAULT_KERNEL);
		defaultElement.setValue("n");
		properties.add(defaultElement);

		YoctoBspPropertyElement smpElement = new YoctoBspPropertyElement();
		smpElement.setName(SMP_NAME);
		if (smpButton.getSelection())
			smpElement.setValue("y");
		else
			smpElement.setValue("n");
		properties.add(smpElement);

		YoctoBspPropertyElement newKbElement = new YoctoBspPropertyElement();
		YoctoBspPropertyElement kbElement = new YoctoBspPropertyElement();

		newKbElement.setName(NEED_NEW_KBRANCH_NAME);
		if (newBranchButton.getSelection()) {
			newKbElement.setValue("y");
			properties.add(newKbElement);
			kbElement.setName(NEW_KBRANCH_NAME);
			kbElement.setValue(kbSelection);
			properties.add(kbElement);
		} else {
			newKbElement.setValue("n");
			properties.add(newKbElement);
			kbElement.setName(EXISTING_KBRANCH_NAME);
			kbElement.setValue(kbSelection);
			properties.add(kbElement);
		}

		return properties;
	}

	public boolean validatePage() {
		if (kernelCombo == null)
			return false;

		if ((kernelCombo != null) && (branchesCombo != null)) {
			String kcSelection = kernelCombo.getText();
			String kbSelection = branchesCombo.getText();
			if ((kcSelection == null) || (kbSelection == null) || (kcSelection.isEmpty()) || (kbSelection.isEmpty())) {
				setErrorMessage("Please choose a kernel and a specific branch!");
				return false;
			}
		}
		if ((propertyControlMap != null)) {
			if (!propertyControlMap.isEmpty()) {
				Enumeration<YoctoBspPropertyElement> keys = propertyControlMap.keys();
				while (keys.hasMoreElements()) {
					YoctoBspPropertyElement key = keys.nextElement();
					Control control = propertyControlMap.get(key);
					String type = key.getType();

					if (type.contentEquals("edit")) {
						String text_value = ((Text)control).getText();
						if (text_value == null) {
							setErrorMessage("Field "+ key.getName() +" is not set.  All of the field on this screen must be set!");
							return false;
						} else {
							key.setValue(text_value);
						}
					} else if (type.contentEquals("choicelist")) {
						String choice_value = ((Combo)control).getText();
						if (choice_value == null) {
							setErrorMessage("Field "+ key.getName() +" is not set.  All of the field on this screen must be set!");
							return false;
						} else {
							key.setValue(choice_value);
						}
					} else {
						boolean button_select = ((Button)control).getSelection();
						if (button_select)
							key.setValue("y");
						else
							key.setValue("n");
					}
					updateProperties(key);
				}
			}
		}
		return true;
	}

	private void updateProperties(YoctoBspPropertyElement element) {
		Iterator<YoctoBspPropertyElement> it = properties.iterator();

		while (it.hasNext()) {
			YoctoBspPropertyElement propElem = it.next();
			if (propElem.getName().contentEquals(element.getName())) {
				properties.remove(propElem);
				properties.add(element);
				break;
			} else
				continue;
		}
	}
	private void controlChanged(Widget widget) {
		setErrorMessage(null);

		String kernel_choice = kernelCombo.getText();
		if ((kernel_choice == null) || (kernel_choice.isEmpty())) {
			setErrorMessage("Please choose kernel !");
			return;
		}
		if (widget == kernelCombo) {
			updateKernelValues(KERNEL_BRANCHES, "\\\"" + kernel_choice + "\\\"." + NEW_KBRANCH_NAME);
		} else if (widget == branchesCombo) {
			setErrorMessage(null);
			branchesCombo.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		} else if (widget == newBranchButton || widget == existingBranchButton) {
			if (newBranchButton.getSelection()) {
				updateKernelValues(KERNEL_BRANCHES, "\"" + kernel_choice + "\"." + NEW_KBRANCH_NAME);
			} else {
				updateKernelValues(KERNEL_BRANCHES, "\"" + kernel_choice + "\"." + EXISTING_KBRANCH_NAME);
			}
			branchesCombo.deselectAll();
		}
		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
		this.composite.layout(true, true);
		composite.pack();
	}

	private void updateKernelValues(final String value, String property) {
		@SuppressWarnings("unused")
		String build_dir = "";
		if ((bspElem.getBuildLoc() == null) || bspElem.getBuildLoc().isEmpty())
			build_dir = bspElem.getMetadataLoc()+"/build";
		else
			build_dir = bspElem.getBuildLoc();

		String metadataLoc = bspElem.getMetadataLoc();
		String valuesCmd = "source " + metadataLoc + "/oe-init-build-env;" + metadataLoc + "/scripts/" + VALUES_CMD_PREFIX + bspElem.getKarch() + VALUES_CMD_SURFIX + property;
		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(),  new KernelBranchesGetter(valuesCmd), "Loading Kernel " + value);
		if (value.equals(KERNEL_CHOICES))
			progressDialog.run(false);
		else if (value.equals(KERNEL_BRANCHES))
			progressDialog.run(true);
		
		BSPAction action = progressDialog.getBspAction();
		if (action.getItems() != null) {
			if (value.equals(KERNEL_CHOICES)) {
				kernelCombo.setItems(action.getItems());
				kernelCombo.pack();
				kernelCombo.deselectAll();
				branchesCombo.setEnabled(false);
				branchesCombo.deselectAll();
			} else if (value.equals(KERNEL_BRANCHES)) {
				branchesCombo.setItems(action.getItems());
				branchesCombo.pack();
				branchesCombo.setEnabled(true);
			}
			composite.setMinSize(controlContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		} else if (action.getMessage() != null)
			MessageDialog.openError(getShell(), "Yocto-BSP", action.getMessage());
		composite.setMinSize(controlContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
	}

	private String[] getBSPComboProperties(String property) {
		String build_dir = "";
		if ((bspElem.getBuildLoc() == null) || bspElem.getBuildLoc().isEmpty())
			build_dir = bspElem.getMetadataLoc()+"/build";
		else
			build_dir = bspElem.getBuildLoc();

		String valuesCmd = "export BUILDDIR=" + build_dir + ";" + bspElem.getMetadataLoc() + "/scripts/" + VALUES_CMD_PREFIX + bspElem.getKarch() + VALUES_CMD_SURFIX + property;
		BSPProgressDialog progressDialog = new BSPProgressDialog(getShell(),  new KernelBranchesGetter(valuesCmd), "Loading property " + property + "values");
		progressDialog.run(false);
		BSPAction action = progressDialog.getBspAction();

		if (action.getItems() != null) {
			return action.getItems();
		} else if (action.getMessage() != null) {
			MessageDialog.openError(getShell(), "Yocto-BSP", action.getMessage());
			return new String[]{};
		}
		return new String[]{};
	}
}

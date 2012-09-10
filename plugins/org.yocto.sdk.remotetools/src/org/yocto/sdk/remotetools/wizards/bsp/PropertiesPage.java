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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
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
 * Setting up the parameters for creating the new Yocto Bitbake project
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
	private Composite composite;
	private YoctoBspElement bspElem = null;
	private boolean karch_changed = false;
	private Combo kcCombo;
	private Combo kbCombo;
	private Button newButton;
	private Button existingButton;
	private Button smpButton;
	private Composite kcContainer = null;
	private Group kbGroup = null;
	private ScrolledComposite sc = null;
	private Composite controlContainer = null;
	private Group propertyGroup = null;

	public PropertiesPage(YoctoBspElement element) {
		super(PAGE_NAME, "yocto-bsp properties page", null);
		this.bspElem = element;
	}

	public void onEnterPage(YoctoBspElement element) {
		if (!element.getValidPropertiesFile()) {
			setErrorMessage("There's no valid properties file created, please choose \"Back\" to reselect kernel architecture!");
			return;
		}

		if (this.bspElem == null || this.bspElem.getKarch().isEmpty() || !this.bspElem.getKarch().contentEquals(element.getKarch())) {
			karch_changed = true;
		} else
			karch_changed = false;

		this.bspElem = element;
		this.composite.setLayout(new FillLayout());
		if (sc == null) {

			sc = new ScrolledComposite(this.composite, SWT.H_SCROLL | SWT.V_SCROLL);

			controlContainer = new Composite(sc, SWT.NONE);
			controlContainer.setLayout(new GridLayout(1, false));
			controlContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (kcContainer == null) {
				kcContainer = new Composite(controlContainer, SWT.NONE);

				kcContainer.setLayout(new FillLayout());
				new Label (kcContainer, SWT.NONE).setText(KERNEL_CHOICE+":");
				kcCombo = new Combo(kcContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
				smpButton = new Button(controlContainer, SWT.CHECK);
				smpButton.setText("SMP Support");
				smpButton.setSelection(true);

				kbGroup= new Group(controlContainer, SWT.NONE);		
				kbGroup.setLayout(new FillLayout(SWT.VERTICAL));	
				kbGroup.setText("Kernel Branch Settings:");
				newButton = new Button(kbGroup, SWT.RADIO);
				newButton.setText("New");
				newButton.setSelection(true);
		    
				existingButton = new Button(kbGroup, SWT.RADIO);
				existingButton.setText("Existing");
				existingButton.setSelection(false);
		    
		    	kbCombo = new Combo(kbGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		    	
			} 
		}
		if (karch_changed) {
			kbCombo.removeAll();
			newButton.setSelection(true);
			existingButton.setSelection(false);

			updateKernelValues(KERNEL_CHOICES, KERNEL_CHOICE);
		}

		try {
			if (karch_changed) {
				properties = YoctoJSONHelper.getProperties();

				if (!properties.isEmpty()) {

					if (propertyGroup != null) {
						propertyGroup.dispose();
					}
					propertyGroup= new Group(controlContainer, SWT.NONE);
					propertyGroup.setLayout(new FillLayout(SWT.VERTICAL));
					propertyGroup.setText("Other Properties Settings:");

					if (!element.getQarch().isEmpty()) {
						YoctoBspPropertyElement qarch_elem = new YoctoBspPropertyElement();
						qarch_elem.setName(QARCH_NAME);
						qarch_elem.setValue(element.getQarch());
						properties.add(qarch_elem);
					}

					propertyControlMap = new Hashtable<YoctoBspPropertyElement, Control>();
					Iterator<YoctoBspPropertyElement> it = properties.iterator();

				    while (it.hasNext()) {
				        // Get property
				        YoctoBspPropertyElement propElem = (YoctoBspPropertyElement)it.next();
				        String type  = propElem.getType();
				        String name = propElem.getName();
				        if (type.contentEquals("edit")) {
				        	Composite textContainer = new Composite(propertyGroup, SWT.NONE);
				        	
				        	textContainer.setLayout(new FillLayout());
				        	new Label (textContainer, SWT.NONE).setText(name+":");
				        	Text text = new Text(textContainer, SWT.BORDER | SWT.SINGLE);
				    		propertyControlMap.put(propElem, (Control)text);
				    		
				        } else if (type.contentEquals("boolean")) {
				        	Composite booleanContainer = new Composite(propertyGroup, SWT.NONE);
				        	
				        	booleanContainer.setLayout(new FillLayout(SWT.VERTICAL));
				        	
				        	String default_value = propElem.getDefaultValue();
				        	Button button = new Button(booleanContainer, SWT.CHECK);
				    		button.setText(name);
				    		if (default_value.equalsIgnoreCase("y")) {
				    			button.setSelection(true);	
				    		} else 
				    			button.setSelection(false);
				    		propertyControlMap.put(propElem, (Control)button);
				    		
				        } else if (type.contentEquals("choicelist")) {
				        	Composite choiceContainer = new Composite(propertyGroup, SWT.NONE);
				        	
				        	choiceContainer.setLayout(new FillLayout());
				        	
				        	new Label (choiceContainer, SWT.NONE).setText(name+":");
				        	Combo combo = new Combo(choiceContainer, SWT.BORDER | SWT.READ_ONLY);
				    		combo.setLayout(new FillLayout());

						updateKernelValues(KERNEL_CHOICES, name);

				    		propertyControlMap.put(propElem, (Control)combo);
				        }
				    }
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sc.setContent(controlContainer);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(controlContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		controlContainer.pack();

		this.composite.layout(true, true);
		kcCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});


	    newButton.addSelectionListener(new SelectionListener() {
	    	public void widgetDefaultSelected(SelectionEvent e) {}
	    	
	    	public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		});
	    
	   
	    existingButton.addSelectionListener(new SelectionListener() {
	    	public void widgetDefaultSelected(SelectionEvent e) {}
	    	
	    	public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		});
	    
	    
		kbCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});


	}


	public void createControl(Composite parent) {
		this.composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		this.composite.setLayout(layout);

		gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan= 2;
		this.composite.setLayoutData(gd);

		setControl(this.composite);
	}

	public boolean canFlipToNextPage() {
		return false;
	}

	public HashSet<YoctoBspPropertyElement> getProperties() {
		String kcSelection = kcCombo.getText();
		String kbSelection = kbCombo.getText();
		YoctoBspPropertyElement kc_element = new YoctoBspPropertyElement();
		kc_element.setName(KERNEL_CHOICE);
		kc_element.setValue(kcSelection);
		properties.add(kc_element);
		YoctoBspPropertyElement default_element = new YoctoBspPropertyElement();
		default_element.setName(DEFAULT_KERNEL);
		default_element.setValue("n");
		properties.add(default_element);

		YoctoBspPropertyElement smp_element = new YoctoBspPropertyElement();
		smp_element.setName(SMP_NAME);
		if (smpButton.getSelection())
			smp_element.setValue("y");
		else
			smp_element.setValue("n");
		properties.add(smp_element);

		YoctoBspPropertyElement newkb_element = new YoctoBspPropertyElement();
		YoctoBspPropertyElement kb_element = new YoctoBspPropertyElement();

		newkb_element.setName(NEED_NEW_KBRANCH_NAME);
		if (newButton.getSelection()) {
			newkb_element.setValue("y");
			properties.add(newkb_element);
			kb_element.setName(NEW_KBRANCH_NAME);
			kb_element.setValue(kbSelection);
			properties.add(kb_element);
		} else {
			newkb_element.setValue("n");
			properties.add(newkb_element);
			kb_element.setName(EXISTING_KBRANCH_NAME);
			kb_element.setValue(kbSelection);
			properties.add(kb_element);
		}

		return properties;
	}

	public boolean validatePage() {
		if (kcCombo == null)
			return false; 

		if ((kcCombo != null) && (kbCombo != null)) {
			String kcSelection = kcCombo.getText();
			String kbSelection = kbCombo.getText();
			if ((kcSelection == null) || (kbSelection == null) || (kcSelection.isEmpty()) || (kbSelection.isEmpty())) {
				setErrorMessage("Please select kernel_choice and kernel_branch!");
				return false;
			}
		}
		if ((propertyControlMap != null)) { 
			if (!propertyControlMap.isEmpty()) {
				Enumeration<YoctoBspPropertyElement> keys = propertyControlMap.keys();
				while( keys.hasMoreElements() ) {
					YoctoBspPropertyElement key = (YoctoBspPropertyElement)keys.nextElement();
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
			YoctoBspPropertyElement propElem = (YoctoBspPropertyElement)it.next();
			if (propElem.getName().contentEquals(element.getName())) {
				properties.remove((Object) propElem);
				properties.add(element);
				break;
			} else 
				continue;
		}
 	}
	private void controlChanged(Widget widget) {
		 setErrorMessage(null);
		 
		 String kernel_choice = kcCombo.getText();
		 if ((kernel_choice == null) || (kernel_choice.isEmpty())) {
			 setErrorMessage("Please selecte kernel_choice!");
			 return;
		 }
		 if (widget == kcCombo) {
			 newButton.setSelection(true);
			 existingButton.setSelection(false);
			 kbCombo.removeAll();
			 
			 updateKernelValues(KERNEL_BRANCHES, "\\\"" + kernel_choice + "\\\"." + NEW_KBRANCH_NAME);
		 } else if (widget == kbCombo) {
			 setErrorMessage(null);
		 } else if (widget == newButton) {
			 
			 boolean newBranch = newButton.getSelection();
			 
			 if (newBranch) {
				 kbCombo.removeAll();
				 updateKernelValues(KERNEL_BRANCHES, "\\\"" + kernel_choice + "\\\"." + NEW_KBRANCH_NAME);
			 }
		 } else if (widget == existingButton) {
			 boolean existingBranch = existingButton.getSelection();

			 if (existingBranch) {
				 kbCombo.removeAll();
				 updateKernelValues(KERNEL_BRANCHES, "\\\"" + kernel_choice + "\\\"." + EXISTING_KBRANCH_NAME);
			 }
		 }
		 canFlipToNextPage();
		 getWizard().getContainer().updateButtons();
	}

	private void updateKernelValues(final String value, String property) {
		final ValuesGetter runnable  = new ValuesGetter(property);

		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress(){
			     public void run(IProgressMonitor monitor) {
			         monitor.beginTask("Loading Kernel " + value + " ...", 100);
			         runnable.run();
			         monitor.done();
			     }
			 });
		} catch (Exception e) {
			runnable.getBspAction().setMessage(e.getMessage());
		}

		BSPAction action = runnable.getBspAction();
		if (action.getItems() != null) {
			if (value == KERNEL_CHOICES)
				kcCombo.setItems(action.getItems());
			else if (value == KERNEL_BRANCHES)
				kbCombo.setItems(action.getItems());
		} else if (action.getMessage() != null)
			MessageDialog.openError(getShell(), "Yocto-BSP", action.getMessage());
	}

	class ValuesGetter implements Runnable {
		private String property;
		private BSPAction bspAction;

		public ValuesGetter(String property) {
			this.property = property;
			this.bspAction = new BSPAction(null, null);
		}

		public void run() {
			ArrayList<String> values = new ArrayList<String>();

			String build_dir = "";
			if ((bspElem.getBuildLoc() == null) || bspElem.getBuildLoc().isEmpty())
				build_dir = bspElem.getMetadataLoc()+"/build";
			else
				build_dir = bspElem.getBuildLoc();

			String values_cmd = "export BUILDDIR=" + build_dir + ";" + bspElem.getMetadataLoc() + "/scripts/" + VALUES_CMD_PREFIX + bspElem.getKarch() + VALUES_CMD_SURFIX + property;
			try {
				ProcessBuilder builder = new ProcessBuilder(new String[] {"sh", "-c", values_cmd});
				builder.redirectErrorStream(true);
				Process process = builder.start();
				BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = null;
				String error_message = "";
				while ( (line = br.readLine()) != null) {
					if (!line.startsWith("[")) {
						error_message += line + "\n";
						continue;
					}
					String[] items = line.split(",");

					String value = items[0];
					value = value.replace("[\"", "");
					value = value.replaceAll("\"$", "");
					values.add(value);
				}
				int exitVal = process.waitFor();
				if (exitVal != 0) {
					bspAction.setMessage(error_message);
					bspAction.setItems(null);
				}
			} catch (Exception e) {
				bspAction.setItems(null);
				bspAction.setMessage(e.getMessage());
			}
			if (!values.isEmpty()) {
				bspAction.setItems(values.toArray(new String[values.size()]));
				bspAction.setMessage(null);
			}
		}

		public BSPAction getBspAction() {
			return bspAction;
		}

		public void setBspAction(BSPAction bspAction) {
			this.bspAction = bspAction;
		}
	}

	class BSPAction {
		private String[] items;
		private String message;

		BSPAction(String[] items, String message){
			this.setItems(items);
			this.setMessage(message);
		}

		public String[] getItems() {
			return items;
		}

		public void setItems(String[] items) {
			this.items = items;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}

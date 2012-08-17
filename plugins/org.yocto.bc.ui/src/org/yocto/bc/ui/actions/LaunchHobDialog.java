/*******************************************************************************
 * Copyright (c) 2011 Intel Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.ui.actions;

import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ICommand;

import org.yocto.bc.ui.builder.BitbakeBuilder;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;

public class LaunchHobDialog extends Dialog {
	private String title;
	private Button buildButton;
	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;
	private Combo build_dir_combo;
	
	private IProject project;
	private Shell shell;
	private String build_dir;

	public LaunchHobDialog(Shell parentShell, String dialogTitle, IProject project) {
        super(parentShell);
        this.shell = parentShell;
        this.project = project;
        this.title = dialogTitle;
        setShellStyle(getShellStyle() | SWT.RESIZE);
       
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

	public String getBuildDir() {
		return build_dir;
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite result = (Composite) super.createDialogArea(parent);
				
		try {
			createComposite(result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Have you ever set the project specific Yocto Settings?");
			System.out.println(e.getMessage());
		}

		return result;
	}

	private void createComposite(Composite composite) throws Exception{
		Label root_label, sysroot_label;
		
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);	

		Label build_dir_label = new Label(composite, SWT.NONE);
		build_dir_label.setText("Bitbake build directory: ");
		Composite textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		build_dir_combo = new Combo(textContainer, SWT.DROP_DOWN);
		initializeBuildCombo();
		
		Button buildButton = addDirSelectButton(textContainer, build_dir_combo);
		
		//we add the listener at the end for avoiding the useless event trigger when control
		//changed or modified.
		buildButton.addSelectionListener(fSelectionListener);
		build_dir_combo.addModifyListener(fModifyListener);
	}

	private Button addDirSelectButton(final Composite parent, final Combo combo) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText("Browse");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new DirectoryDialog(parent.getShell()).open();
				
				if (dirName != null) {
					combo.add(dirName);
					combo.setText(dirName);
				}
			}
		});
		return button;
	}		
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			try {
				build_dir = build_dir_combo.getText().toString();
				updateBuildSpec(build_dir);
				super.buttonPressed(buttonId); 
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e.getMessage());
			}
		}
		else if (buttonId == IDialogConstants.CANCEL_ID)
		{
			super.buttonPressed(buttonId);
		}			
	}
	
	private boolean validateInput() {
		boolean valid = false;
		String build_dir = build_dir_combo.getText().toString();
		if ((build_dir == null) || build_dir.isEmpty()) {
			Display display = Display.getCurrent();
			Shell shell = new Shell(display);
			MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			msgBox.setText("Yocto Configuration Error");
			msgBox.setMessage("The specified build directory is empty!");
			msgBox.open();
			if (shell != null)
				shell.dispose();
			return valid;
		}
		String project_path = project.getLocation().toString();
		File project_dir_file = new File(project_path);
		File build_dir_file = new File(build_dir);
		try {
			if (isSubDirectory(project_dir_file, build_dir_file)) {
				Display display = Display.getCurrent();
				Shell shell = new Shell(display);
				MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				msgBox.setText("Yocto Configuration Error");
				msgBox.setMessage("The specified build directory is a sub-dir of project path: " + project_path);
				msgBox.open();
				if (shell != null)
					shell.dispose();
			} else
				valid = true;
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return valid;
	}
	
	private boolean isSubDirectory(File baseDir, File subDir) throws IOException {
		baseDir = baseDir.getCanonicalFile();
		subDir = subDir.getCanonicalFile();

		File parentFile = subDir;
		while (parentFile != null) {
			if (baseDir.equals(parentFile)) {
				return true;
			}
			parentFile = parentFile.getParentFile();
		}
		return false;
	}
	
	private void controlChanged(Widget widget) {

		if (widget == buildButton)
		{
		}
	}

	private void controlModified(Widget widget) {
		if (widget == build_dir_combo)
		{
			
		}
	}
	
	private void initializeBuildCombo()
	{
		ArrayList<String> items = new ArrayList<String> ();
		
		try {
			IProjectDescription desc = project.getDescription();
		    
			ICommand[] buildSpec = desc.getBuildSpec();
			if ((buildSpec != null) && (buildSpec.length != 0))
			{
				for (int i = 0; i < buildSpec.length; i++) {
					ICommand cmd = buildSpec[i];
					if (cmd.getBuilderName().equalsIgnoreCase(BitbakeBuilder.HOB_BUILD_ID))
					{
						Map<String, String> args = cmd.getArguments();
						if ((args != null) && !args.isEmpty())
						{
							Iterator entries = args.entrySet().iterator();
							while (entries.hasNext()) {
								Entry thisEntry = (Entry) entries.next();
								String key = (String)thisEntry.getKey();
								if (key.equalsIgnoreCase(BitbakeCommanderNature.BUILD_DIR_KEY)) {
									build_dir_combo.removeAll();
									build_dir_combo.setItems(getValues((String)thisEntry.getValue()));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private String[] getValues(String value) {
	
		if ((value != null) && !value.isEmpty()) 
		{
			String[] pieces = value.split(",");
			for (int i = 0; i < pieces.length; i++) 
			{
				int start = pieces[i].indexOf("[");
				if (start >= 0)
					pieces[i] = pieces[i].substring(start+1);
				int end = pieces[i].indexOf("]");
				if (end >= 0)
					pieces[i] = pieces[i].substring(0, end);
				pieces[i] = pieces[i].trim();
			}
			return pieces;
	    }
		return null;
	}
	
	private void updateBuildSpec(String build_dir)
	{
		try {
			String[] items = build_dir_combo.getItems();
			HashSet values = new HashSet();
			Map<String, String> args = new HashMap<String, String>();
			values.add(build_dir);
			for (int i = 0; i < items.length; i++) {
				values.add(items[i]);
			}
			args.put(BitbakeCommanderNature.BUILD_DIR_KEY, values.toString());
			IProjectDescription desc = project.getDescription();
			ICommand[] buildSpec = desc.getBuildSpec();
			boolean found = false;
			if ((buildSpec != null) || (buildSpec.length != 0)) {
				for (int i = 0; i < buildSpec.length; i++) {
					ICommand cmd = buildSpec[i];
					if (cmd.getBuilderName().equalsIgnoreCase(BitbakeBuilder.HOB_BUILD_ID)) {
						cmd.setArguments(args);
						desc.setBuildSpec(buildSpec);
						found = true;
						break;
					}
				}
			}
			if (!found) {
				ICommand[] newBuildSpec = new ICommand[buildSpec.length + 1];
				System.arraycopy(buildSpec, 0, newBuildSpec, 0, buildSpec.length);
				ICommand cmd = desc.newCommand();
				cmd.setBuilderName(BitbakeBuilder.HOB_BUILD_ID);
				cmd.setArguments(args);
				newBuildSpec[newBuildSpec.length - 1] = cmd;
				desc.setBuildSpec(newBuildSpec);		
			} 
			project.setDescription(desc, null);
		} catch (Exception e) {
				System.out.println(e.getMessage());
		}
	}
}

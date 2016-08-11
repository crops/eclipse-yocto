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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.yocto.bc.ui.builder.BitbakeBuilder;

public class LaunchToasterDialog extends Dialog {
	private Combo toaster_url;
	private IProject project;
	private URL toaster_server;

	public LaunchToasterDialog(Shell parentShell, String dialogTitle, IProject project) {
        super(parentShell);
        this.project = project;
        setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite result = (Composite) super.createDialogArea(parent);

		try {
			createComposite(result);
		} catch (Exception e) {
			System.out.println("Have you ever set the project specific Yocto Settings?");
			System.out.println(e.getMessage());
		}

		return result;
	}

	private void createComposite(Composite composite) throws Exception{
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		gd= new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);

		Label build_dir_label = new Label(composite, SWT.NONE);
		build_dir_label.setText("Toaster Server URL: ");
		toaster_url = new Combo(composite, SWT.DROP_DOWN);
		toaster_url.setText("https://www.yoctoproject.org/toaster/");
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			try {
				toaster_server = new URL(toaster_url.getText().toString()) ;
				super.buttonPressed(buttonId);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		else if (buttonId == IDialogConstants.CANCEL_ID)
		{
			super.buttonPressed(buttonId);
		}
	}

	protected URL get_toaster_url() {
		return toaster_server ;
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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
					if (cmd.getBuilderName().equalsIgnoreCase(BitbakeBuilder.TOASTER_BUILD_ID))
					{
						Map<String, String> args = cmd.getArguments();
						if ((args != null) && !args.isEmpty())
						{
							Iterator<Entry<String, String>> entries = args.entrySet().iterator();
							while (entries.hasNext()) {
								Entry<String, String> thisEntry = (Entry<String, String>) entries.next();
								String key = (String)thisEntry.getKey();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	@SuppressWarnings("unused")
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
}

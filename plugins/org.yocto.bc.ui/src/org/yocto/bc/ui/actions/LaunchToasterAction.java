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

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;


public class LaunchToasterAction  {
    private static final String DIALOG_TITLE  = "Launch Toaster";

	public void run(IAction action) {
		IResource resource = getSelectedResource();
		if (resource == null)
			return;

		IProject project = resource.getProject();
		LaunchToasterDialog toaster_dialog = new LaunchToasterDialog(new Shell(), DIALOG_TITLE, project);
		toaster_dialog.open();
		URL toaster_url = toaster_dialog.get_toaster_url();

		if (toaster_url != null) {
			try {
				final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser("yocto");
				browser.openURL(toaster_url);
			} catch (Exception e){
				System.out.println(e.getMessage());
			}
		}
	}

	private IResource getSelectedResource() {
		IWorkbench iworkbench = PlatformUI.getWorkbench();
		if (iworkbench == null){
			return null;
		}
		IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
		if (iworkbenchwindow == null) {
			return null;
		}
		IWorkbenchPage iworkbenchpage = iworkbenchwindow.getActivePage();
		if (iworkbenchpage == null) {
			return null;
		}
		ISelection sel = iworkbenchpage.getSelection();

		if (!(sel instanceof IStructuredSelection))
			return null;
		IStructuredSelection ss = (IStructuredSelection) sel;
			Object element = ss.getFirstElement();
		if (element instanceof IResource)
		    return (IResource) element;
		if (!(element instanceof IAdaptable))
		    return null;
		 IAdaptable adaptable = (IAdaptable)element;
		 Object adapter = adaptable.getAdapter(IResource.class);
		    return (IResource) adapter;
	}
}

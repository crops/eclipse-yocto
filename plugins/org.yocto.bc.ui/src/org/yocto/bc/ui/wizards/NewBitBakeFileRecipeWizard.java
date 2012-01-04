/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Jessica Zhang (Intel) - Extend to support auto-fill base on src_uri value
 *******************************************************************************/
package org.yocto.bc.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import org.yocto.bc.bitbake.BBLanguageHelper;

public class NewBitBakeFileRecipeWizard extends Wizard implements INewWizard {
	private NewBitBakeFileRecipeWizardPage page;
	private ISelection selection;

	public NewBitBakeFileRecipeWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new NewBitBakeFileRecipeWizardPage(selection);
		addPage(page);
	}

	private void doFinish(BitbakeRecipeUIElement element, IProgressMonitor monitor) throws CoreException {
		String fileName = element.getFile();
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(element.getContainer()));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + element.getContainer() + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		
		// If the extension wasn't specified, assume .bb
		if (!fileName.endsWith(".bb") && !fileName.endsWith(".inc") && !fileName.endsWith(".conf")) {
			fileName = fileName + ".bb";
		}
		
		final IFile file = container.getFile(new Path(fileName));
		try {
			InputStream stream = openContentStream(element);
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			stream.close();
		} catch (IOException e) {
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * We will initialize file contents with a sample text.
	 * @param srcuri 
	 * @param author 
	 * @param homepage 
	 * @param license 
	 * @param description 
	 * @param fileName 
	 * @param newPage 
	 */

	private InputStream openContentStream(BitbakeRecipeUIElement element) {
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("DESCRIPTION = \"" + element.getDescription() + "\"\n");
		
		if (element.getAuthor().length() > 0) {
			sb.append("AUTHOR = \"" + element.getAuthor() + "\"\n");
		}

		if (element.getHomePage().length() > 0) {
			sb.append("HOMEPAGE = \"" + element.getHomePage() + "\"\n");
		}
		
		if (element.getSection().length() > 0) {
			sb.append("SECTION = \"" + element.getSection() + "\"\n");
		}
		
		if (element.getLicense().length() > 0) {
			sb.append("LICENSE = \"" + element.getLicense() + "\"\n");
		}

		if (element.getChecksum().length() > 0) {
			sb.append("LIC_FILES_CHKSUM = \"" + element.getChecksum() + "\"\n");
		}
		
		if (element.getSrcuri().length() > 0) {
			sb.append("SRC_URI = \"" + element.getSrcuri() + "\"\n");
		}
		
		if (element.getMd5sum().length() > 0) {
			sb.append("SRC_URI[md5sum] = \"" + element.getMd5sum() + "\"\n");
		}
	
		if (element.getsha256sum().length() > 0) {
			sb.append("SRC_URI[sha256sum] = \"" + element.getsha256sum() + "\"\n");
		}
		
		ArrayList<String> inheritance = element.getInheritance();
		if (!inheritance.isEmpty()) {
			Object ia[] = inheritance.toArray();
			String inheritance_str = "inherit ";
			for(int i=0; i<ia.length; i++)
				inheritance_str += ((String) ia[i]) + " ";
			sb.append(inheritance_str); 
		}
		sb.append("\n");

		return new ByteArrayInputStream(sb.toString().getBytes());
	}

	@Override
	public boolean performFinish() {
		final BitbakeRecipeUIElement element = page.getUIElement();
		
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(element, monitor);
					File temp_dir = new File(element.getMetaDir() + "/temp");
					if (temp_dir.exists()) {
						File working_dir = new File(element.getMetaDir());
					
						String rm_cmd = "rm -rf temp";
						final Process process = Runtime.getRuntime().exec(rm_cmd, null, working_dir);
						int returnCode = process.waitFor();
						if (returnCode != 0) {
							throw new Exception("Failed to clean up the temp dir");
						}
					}
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "org.yocto.bc.ui", IStatus.OK, message, null);
		throw new CoreException(status);
	}
}
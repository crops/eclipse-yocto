/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *******************************************************************************/
package org.yocto.bc.ui.actions;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import org.yocto.bc.bitbake.BBLanguageHelper;
import org.yocto.bc.bitbake.BBSession;
import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;

public abstract class AbstractBitbakeCommandAction implements IWorkbenchWindowActionDelegate {

	private class CommandJob extends Job {

		public CommandJob() {
			super(getJobTitle());
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String cmds[] = getCommands();
			return execCommands(cmds, monitor);
		}

	}
	protected IAction action;
	protected IFile recipe;
	protected BBSession bbs;

	private Color commandColor, errorColor;
	private boolean errorOccurred = false;

	public AbstractBitbakeCommandAction() {
		commandColor = JFaceResources.getColorRegistry().get(JFacePreferences.ACTIVE_HYPERLINK_COLOR);
		JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR);
		errorColor = JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
	}

	private void checkEnabled(IFile file) {
		try {
			if (file.getFileExtension() == null || !file.getFileExtension().equals(BBLanguageHelper.BITBAKE_RECIPE_FILE_EXTENSION)) {
				action.setEnabled(false);
				return;
			}

			IProject project = file.getProject();
			if (!(project.hasNature(BitbakeCommanderNature.NATURE_ID))) {
				action.setEnabled(false);
				return;
			}

//			bbs = Activator.getBBSession(project.getLocationURI().getPath());

			if (bbs != null) {
				recipe = file;
				action.setEnabled(true);
			}

		} catch (CoreException e) {
			action.setEnabled(false);
			e.printStackTrace();
		} catch (Exception e) {
			action.setEnabled(false);
			e.printStackTrace();
		}
	}

	public void dispose() {
	}

	/**
	 * Execute array of commands with bitbake and put output in console.
	 *
	 * @param cmds
	 * @param monitor
	 * @return
	 */
	protected IStatus execCommands(String[] cmds, final IProgressMonitor monitor) {
		MessageConsole mc = bbs.getConsole();
		final MessageConsoleStream cmd = mc.newMessageStream();
		cmd.setColor(commandColor);
		final MessageConsoleStream out = mc.newMessageStream();
		final MessageConsoleStream err = mc.newMessageStream();
		err.setColor(errorColor);

		try {
			for (int i = 0; i < cmds.length; ++i) {
				cmd.println(cmds[i]);
				monitor.subTask(cmds[i]);
				bbs.getShell().execute(cmds[i]);
			}
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		} finally {
			try {
				if (errorOccurred) {
					cmd.println("At least one error occured while executing this command.  Check output for more details.");
				}
				cmd.close();
				out.close();
				err.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return Status.OK_STATUS;
	}

	protected void errorOccurred() {
		errorOccurred = true;
	}

	/**
	 * Return the command to be executed.
	 *
	 * @return
	 */
	public abstract String[] getCommands();

	public Job getJob() {
		return new CommandJob();
	}

	/**
	 * Return the title of the job.
	 *
	 * @return
	 */
	public abstract String getJobTitle();

	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		Job job = getJob();
		job.schedule();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.action = action;
		if (selection instanceof IStructuredSelection) {
			Object sel = ((IStructuredSelection) selection).getFirstElement();

			if (sel instanceof IFile) {
				checkEnabled((IFile) sel);
				return;
			}
		}

		action.setEnabled(false);
	}

}
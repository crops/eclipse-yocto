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
package org.yocto.sdk.remotetools.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.internal.terminals.ui.TerminalServiceHelper;
import org.eclipse.rse.internal.terminals.ui.views.RSETerminalConnector;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewTab;
import org.eclipse.rse.internal.terminals.ui.views.TerminalViewer;
import org.eclipse.rse.internal.terminals.ui.views.TerminalsUI;
import org.eclipse.rse.services.terminals.ITerminalShell;
import org.eclipse.rse.subsystems.terminals.core.ITerminalServiceSubSystem;
import org.eclipse.rse.subsystems.terminals.core.elements.TerminalElement;

import org.eclipse.rse.ui.SystemBasePlugin;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.yocto.sdk.remotetools.RSEHelper;
import org.yocto.sdk.remotetools.remote.RemoteApplication;
import org.yocto.sdk.remotetools.views.BaseFileView;

public class SystemtapModel extends BaseModel {
	final private String REMOTE_KO_FILE_LOC="/tmp/";

	private String KO_value;
	private String remote_KO_file;
	private IHost host;
	Display display;
	
	String localfile;
	String remotefile;
	
	public SystemtapModel(IHost host, String ko_value,Display display) {
		super(host);
		this.host=host;
		this.KO_value=ko_value;
		this.display=display;
		Path KO_file_path = new Path(KO_value);
		this.remote_KO_file=REMOTE_KO_FILE_LOC+KO_file_path.lastSegment();
	}
	
	@Override
	public void preProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		//upload KO file to remote
		try {
			RSEHelper.putRemoteFile(
					host, 
					KO_value, 
					remote_KO_file,
					monitor);
		}catch (Exception e) {
			throw new InvocationTargetException(e,e.getMessage());
		}

	}

	public void process(IProgressMonitor monitor)
	throws InvocationTargetException, InterruptedException {
	}
	
	@Override
	public void postProcess(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			RSEHelper.deleteRemoteFile(
					rseConnection,
					remote_KO_file,
					monitor);
		}catch (Exception e) {
			
		}
	}

}

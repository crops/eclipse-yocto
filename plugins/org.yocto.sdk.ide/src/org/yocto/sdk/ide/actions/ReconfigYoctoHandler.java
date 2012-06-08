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
package org.yocto.sdk.ide.actions;

import java.lang.reflect.Method;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.cdt.internal.autotools.ui.actions.AbstractAutotoolsHandler;
import org.eclipse.cdt.internal.autotools.ui.actions.InvokeAction;
import org.eclipse.swt.widgets.Display;
import org.yocto.sdk.ide.YoctoSDKPlugin;


@SuppressWarnings("restriction")
public class ReconfigYoctoHandler extends AbstractAutotoolsHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ReconfigYoctoAction action = new ReconfigYoctoAction();
		Method method = null;

		try {
			/*
			 * This is hack to workaround upstream eclipse bug #370288
			 */
			Class [] params = {ExecutionEvent.class, InvokeAction.class};
			method = AbstractAutotoolsHandler.class.getDeclaredMethod("execute",params );
		} catch (NoSuchMethodException e) {
			//no such method, old version of plugin org.eclipse.linuxtools.autotools.ui
			method = null;
		} catch (Exception e) {
			throw new ExecutionException(e.getMessage(), e);
		}

		if (method != null) {
			//new version
			Object [] params = {event, action};
			try {
				return method.invoke(this, params);
			}catch (Exception e) {
				throw new ExecutionException(e.getMessage(), e);
			}
		} else {
			//old version
			//display a dialog to warn the user
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(null, "Change Yocto Project Settings", "Please update the plugin of \"Autotools support for CDT\"!", 
							new Status(IStatus.WARNING,YoctoSDKPlugin.PLUGIN_ID,"old version of plugin \"Autotools support for CDT\" detected."));
				}
			});
			//try to display the dialog in the old way
			Object o = event.getApplicationContext();
			if (o instanceof IEvaluationContext) {
				IContainer container = getContainer((IEvaluationContext)o);
				if (container != null) {
					action.setSelectedContainer(container);
					action.run(null);
				}
			}
		}
		
		return null;
	}
}

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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.AbstractAutotoolsHandler;

@SuppressWarnings("restriction")
public class ReconfigYoctoHandler extends AbstractAutotoolsHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ReconfigYoctoAction a = new ReconfigYoctoAction();
		Object o = event.getApplicationContext();
		if (o instanceof IEvaluationContext) {
			IContainer container = getContainer((IEvaluationContext)o);
			if (container != null) {
				a.setSelectedContainer(container);
				a.run(null);
			}
		}
		return null;
	}
}

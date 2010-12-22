package org.yocto.bc.ui.wizards.importProject;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizardPage;
import org.yocto.bc.ui.wizards.newproject.BBConfigurationInitializeOperation;

/**
 * Bitbake console view.
 * @author kgilmer
 *
 */
public class ConsolePage extends FiniteStateWizardPage {

	private boolean valid = false;
	
	public ConsolePage(Map model) {
		super("Bitbake Console Page", model);
		setTitle("Extracting BitBake Environment");
		setMessage("Output of 'Bitbake -e' command");
	}

	private Text txtConsole;

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		top.setLayout(new GridLayout());
		
		txtConsole = new Text(top, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		txtConsole.setLayoutData(new GridData(GridData.FILL_BOTH));
		txtConsole.setEditable(false);
		txtConsole.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		
		setControl(top);
	}

	@Override
	public void pageCleanup() {
		Activator.resetBBSession((String) model.get(ImportYoctoProjectWizard.KEY_LOCATION));
	}

	@Override
	public void pageDisplay() {
		ProjectInfo pinfo = new ProjectInfo();
		pinfo.setInitScriptPath((String) model.get(ImportYoctoProjectWizard.KEY_INITPATH));
		pinfo.setLocation((String) model.get(ImportYoctoProjectWizard.KEY_LOCATION));
		pinfo.setName((String) model.get(ImportYoctoProjectWizard.KEY_NAME));
		
		try {
			ConsoleWriter cw = new ConsoleWriter();
			this.getContainer().run(false, false, new BBConfigurationInitializeOperation(pinfo, cw));
			txtConsole.setText(cw.getContents());
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription("Failed to create project: " + e.getMessage());
			valid = false;
			setPageComplete(valid);
			return;
		} 
		valid = true;
		model.put(ImportYoctoProjectWizard.KEY_PINFO, pinfo);
		setPageComplete(valid);
	}

	@Override
	protected void updateModel() {
		
	}

	@Override
	protected boolean validatePage() {
		return valid;
	}
	
	private class ConsoleWriter extends Writer {

		private StringBuffer sb;

		public ConsoleWriter() {
			sb = new StringBuffer();
		}
		@Override
		public void close() throws IOException {			
		}
		
		public String getContents() {
			return sb.toString();
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			txtConsole.getText().concat(new String(cbuf));
		}
		
		@Override
		public void write(String str) throws IOException {
			sb.append(str);
		}
		
	}

}

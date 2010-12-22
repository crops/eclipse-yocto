package org.yocto.bc.ui.wizards.install;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import org.yocto.bc.ui.wizards.FiniteStateWizardPage;

/**
 * Select which flavor of OE is to be installed.
 * @author kgilmer
 *
 */
public class ProgressPage extends FiniteStateWizardPage  {

	private static final String STARTED_INSTALL = "STARTED_INSTALL";
	private Text txtConsole;
	private StringBuffer consoleBuffer;
	private ProgressBar pbProgress;
	private String lastError;

	protected static final int PRINT_CMD = 1;
	protected static final int PRINT_OUT = 2;
	protected static final int PRINT_ERR = 3;
	
	protected ProgressPage(Map model) {
		super("Progress", model);
		setTitle("Installing Yocto Poky Build System");
		setMessage("");
	}

	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout());
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		txtConsole = new Text(top, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		txtConsole.setLayoutData(new GridData(GridData.FILL_BOTH));
		txtConsole.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		txtConsole.setEditable(false);
		
		Label s = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		s.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		pbProgress = new ProgressBar(top, SWT.HORIZONTAL | SWT.BORDER);
		
		setControl(top);
	}
	
	protected void printLine(String line, final int type) {
		if (consoleBuffer == null) {
			consoleBuffer = new StringBuffer();
		}
		
		if (type == PRINT_CMD) {
			consoleBuffer.append("$ ");
		} else if (type == PRINT_ERR) {
			consoleBuffer.append("ERROR: ");
			lastError = line;
		}
		
		consoleBuffer.append(line);
		consoleBuffer.append('\n');
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			
			public void run() {
				txtConsole.setText(consoleBuffer.toString());
				txtConsole.setSelection(txtConsole.getText().length() - 1);
				
				if (type == PRINT_CMD) {
					pbProgress.setSelection(pbProgress.getSelection() + 1);
				}
			}
			
		});
	}

	public void pageCleanup() {
		
	}

	public void pageDisplay() {
		if (!model.containsKey(STARTED_INSTALL)) {
			model.put(STARTED_INSTALL, new Boolean(true));
	
			try {
				pbProgress.setMaximum(getLineCount((String) model.get(FlavorPage.INSTALL_SCRIPT)));
			} catch (IOException e) {
				//TODO add logging here.
				e.printStackTrace();
				return;
			}
			
			executeInstall();
		}
	}

	private int getLineCount(String str) throws IOException {
		int count = 0;
		
		BufferedReader br = new BufferedReader(new StringReader(str));
		
		while (br.readLine() != null) {
			count++;
		}
		
		return count;
	}

	private void executeInstall() {
		InstallJob j = new InstallJob(model, this);
		j.addJobChangeListener(new IJobChangeListener() {
			
			public void aboutToRun(IJobChangeEvent event) {
			}

			
			public void awake(IJobChangeEvent event) {
			}

			
			public void done(final IJobChangeEvent event) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					
					public void run() {
						if (event.getResult().isOK()) {
							setMessage("Installation complete, next is to load into workspace.");
							pbProgress.setSelection(pbProgress.getMaximum() - 1);
							setPageComplete(true);
						} else {
							setErrorMessage("An error occurred while installing Yocto:\n");// + lastError);
						}
					}
					
				});				
			}

			public void running(IJobChangeEvent event) {
			}

			
			public void scheduled(IJobChangeEvent event) {				
			}

			
			public void sleeping(IJobChangeEvent event) {
			}
			
		});
		setMessage("Installing Yocto Poky Build System...");
		j.schedule();
	}

	
	protected void updateModel() {
	}

	
	protected boolean validatePage() {		
		
		return true;
	}

	public void printDialog(final String msg) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			
			public void run() {
				setMessage(msg);
			}
		});
	}

}

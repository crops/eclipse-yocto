package org.yocto.bc.ui.wizards.importProject;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import org.yocto.bc.ui.Activator;
import org.yocto.bc.ui.model.ProjectInfo;
import org.yocto.bc.ui.wizards.FiniteStateWizard;
import org.yocto.bc.ui.wizards.newproject.CreateBBCProjectOperation;

public class ImportYoctoProjectWizard extends FiniteStateWizard  implements IImportWizard {
	protected final static String KEY_OEROOT = "OEROOT";
	public static final String KEY_NAME = "NAME";
	public static final String KEY_LOCATION = "LOCATION";
	public static final String KEY_INITPATH = "INITPATH";
	protected static final String KEY_PINFO = "PINFO";
	
	private Map projectModel;
	private IWorkbench workbench;
	private IStructuredSelection selection;
	
	public ImportYoctoProjectWizard() {
		projectModel = new Hashtable();
	}
	
	public Map getModel() {
		return projectModel;
	}
	
	@Override
	public void addPages() {
		addPage(new BBCProjectPage(projectModel));
		addPage(new ConsolePage(projectModel));
	}

	
	public boolean performFinish() {
		ProjectInfo pinfo = (ProjectInfo) projectModel.get(KEY_PINFO);
		Activator.putProjInfo(pinfo.getRootPath(), pinfo);
		try {
			getContainer().run(false, false, new CreateBBCProjectOperation(pinfo));
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e));
			this.getContainer().getCurrentPage().setDescription("Failed to create project: " + e.getMessage());
			return false;
		} 
		
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
		this.setNeedsProgressMonitor(true);
		setWindowTitle("BitBake Commander Project");
	}
}

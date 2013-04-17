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
package org.yocto.sdk.ide.wizard;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.autotools.core.AutotoolsNewProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.cdt.core.templateengine.process.processes.Messages;
import org.eclipse.cdt.internal.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IDiscoveredPathInfo;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IPerProjectDiscoveredPathInfo;
import org.eclipse.cdt.make.internal.core.scannerconfig.util.SymbolEntry;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.templateengine.ProjectCreatedActions;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageManager;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.cdt.ui.wizards.CDTMainWizardPage;
import org.eclipse.cdt.internal.ui.wizards.ICDTCommonProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.yocto.sdk.ide.YoctoGeneralException;
import org.yocto.sdk.ide.YoctoProfileElement;
import org.yocto.sdk.ide.YoctoSDKChecker;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.yocto.sdk.ide.YoctoSDKMessages;
import org.yocto.sdk.ide.YoctoSDKPlugin;
import org.yocto.sdk.ide.YoctoUIElement;
import org.yocto.sdk.ide.natures.YoctoSDKAutotoolsProjectNature;
import org.yocto.sdk.ide.natures.YoctoSDKEmptyProjectNature;
import org.yocto.sdk.ide.natures.YoctoSDKProjectNature;
import org.yocto.sdk.ide.utils.YoctoSDKUtils;


@SuppressWarnings("restriction")
public class NewYoctoCProjectTemplate extends ProcessRunner {
	protected boolean savedAutoBuildingValue;
	protected ProjectCreatedActions pca;
	protected IManagedBuildInfo info;
	protected List<Character> illegalChars = Arrays.asList('$', '"','#','%','&','\'','(',')','*', '+', ',','.','/',':',';','<','=','>','?','@','[','\\',']','^','`','{','|','}','~');
	private static final String PROJECT_NAME_ERROR = "Wizard.SDK.Error.ProjectName";
	
	public NewYoctoCProjectTemplate() {
		pca = new ProjectCreatedActions();
	}
	private String printIllegalChars(){
		String print = "";
		for (Character ch : illegalChars)
			print += ch + ", ";
		print = print.substring(0, print.length() - 2);
		return print;
	}
	public void process(TemplateCore template, ProcessArgument[] args, String processId, IProgressMonitor monitor) throws ProcessFailureException {

		String projectName = args[0].getSimpleValue();
		String location = args[1].getSimpleValue();
		String artifactExtension = args[2].getSimpleValue();
		String isCProjectValue = args[3].getSimpleValue();
		String isEmptyProjetValue = args[4].getSimpleValue();
		String isAutotoolsProjectValue = args[5].getSimpleValue();
		boolean isCProject = Boolean.valueOf(isCProjectValue).booleanValue();
		boolean isEmptyProject = Boolean.valueOf(isEmptyProjetValue).booleanValue();
		boolean isAutotoolsProject = Boolean.valueOf(isAutotoolsProjectValue).booleanValue();

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			if (!isValidProjectName(projectName)) {
				
				IWizardPage[] pages = MBSCustomPageManager.getPages();
				if(pages != null && pages.length > 0) {
					CDTMainWizardPage cdtMainPage = (CDTMainWizardPage)pages[0];
					cdtMainPage.setPageComplete(false);
					ICDTCommonProjectWizard wizard = (ICDTCommonProjectWizard) pages[0].getWizard();
					wizard.performCancel();

					project.delete(true, null);
				}
				throw new ProcessFailureException(YoctoSDKMessages.getFormattedString(PROJECT_NAME_ERROR, new Object[]{projectName, printIllegalChars()}));
			}
			if (!project.exists()) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				turnOffAutoBuild(workspace);

				IPath locationPath = null;
				if (location != null && !location.trim().equals("")) { //$NON-NLS-1$
					locationPath = Path.fromPortableString(location);
				}

				List<?> configs = template.getTemplateInfo().getConfigurations();
				if (configs == null || configs.size() == 0) {
					throw new ProcessFailureException(Messages.getString("NewManagedProject.4") + projectName); //$NON-NLS-1$
				}

				pca.setProject(project);
				pca.setProjectLocation(locationPath);
				pca.setConfigs((IConfiguration[]) configs.toArray(new IConfiguration[configs.size()]));
				pca.setArtifactExtension(artifactExtension);
				info = pca.createProject(monitor, CCorePlugin.DEFAULT_INDEXER, isCProject);

				addNatures(project, false, isEmptyProject, isAutotoolsProject, monitor);

				info.setValid(true);
				ManagedBuildManager.saveBuildInfo(project, true);

				restoreAutoBuild(workspace);
			} else {
				
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				turnOffAutoBuild(workspace);

				YoctoSDKChecker.checkIfGloballySelectedYoctoProfileIsValid();

				addNatures(project, true, isEmptyProject, isAutotoolsProject, monitor);

				//restoreAutoBuild(workspace);
				IDiscoveredPathManager manager = MakeCorePlugin.getDefault().getDiscoveryManager();
				IDiscoveredPathInfo pathInfo = manager.getDiscoveredInfo(project);
				if (pathInfo instanceof IPerProjectDiscoveredPathInfo) {
				    IPerProjectDiscoveredPathInfo projectPathInfo =
				    	(IPerProjectDiscoveredPathInfo) pathInfo;
				    projectPathInfo.setIncludeMap(new LinkedHashMap<String, Boolean>());
				    projectPathInfo.setSymbolMap(new LinkedHashMap<String, SymbolEntry>());
				    manager.removeDiscoveredInfo(project);    
				}
			}
		}
		catch (CoreException e)
		{
			throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + e.getMessage(), e); //$NON-NLS-1$
		} 
		catch (BuildException e)
		{
			throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + e.getMessage()); //$NON-NLS-1$
		}
		catch (YoctoGeneralException e)
		{
			try {
				project.delete(true, monitor);
			} catch (CoreException err) {
				throw new ProcessFailureException(Messages.getString("NewManagedProject.3") + e.getMessage() + " " + err.getMessage()); 
			}
			throw new OperationCanceledException(Messages.getString("NewManagedProject.3") + e.getMessage());
		}
	}
	private boolean isValidProjectName(String projectName) {
		Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_\\-]*$");
		Matcher matcher = pattern.matcher(projectName);
		return matcher.find();
}

	private void addNatures(IProject project, boolean projectExists, boolean isEmptyProject,
			boolean isAutotoolsProject, IProgressMonitor monitor)
					throws CoreException, YoctoGeneralException {
		YoctoSDKProjectNature.addYoctoSDKNature(project, monitor);

		YoctoSDKChecker.checkIfGloballySelectedYoctoProfileIsValid();

		YoctoProfileElement profileElement = YoctoSDKUtils.getProfilesFromDefaultStore();
		YoctoSDKUtils.saveProfilesToProjectPreferences(profileElement, project);

		IPreferenceStore selecteProfileStore = YoctoSDKPlugin.getProfilePreferenceStore(profileElement.getSelectedProfile());
		YoctoUIElement elem = YoctoSDKUtils.getElemFromStore(selecteProfileStore);
		YoctoSDKUtils.setEnvironmentVariables(project, elem);

		if (isEmptyProject) {
			YoctoSDKEmptyProjectNature.addYoctoSDKEmptyNature(project, monitor);
		}

		if (isAutotoolsProject) {
			AutotoolsNewProjectNature.addAutotoolsNature(project, monitor);

			if(!projectExists) {
				// For each IConfiguration, create a corresponding Autotools Configuration
				for (IConfiguration cfg : pca.getConfigs()) {
					AutotoolsConfigurationManager.getInstance().getConfiguration(project, cfg.getName(), true);
				}
				AutotoolsConfigurationManager.getInstance().saveConfigs(project);
			}

			YoctoSDKAutotoolsProjectNature.addYoctoSDKAutotoolsNature(project, monitor);
			YoctoSDKAutotoolsProjectNature.configureAutotoolsOptions(project);
		}
	}

	protected final void turnOffAutoBuild(IWorkspace workspace) throws CoreException {
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		savedAutoBuildingValue = workspaceDesc.isAutoBuilding();
		workspaceDesc.setAutoBuilding(false);
		workspace.setDescription(workspaceDesc);
	}
	
	protected final void restoreAutoBuild(IWorkspace workspace) throws CoreException {
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(savedAutoBuildingValue);
		workspace.setDescription(workspaceDesc);
	}
	
	/**
	 * setOptionValue
	 * @param config
	 * @param option
	 * @param val
	 * @throws BuildException
	 */
	protected void setOptionValue(IConfiguration config, IOption option, String val) throws BuildException {
		if (val != null) {
			if (!option.isExtensionElement()) {
				option.setValue(val);
			} else {
				IOption newOption = config.getToolChain().createOption(option, option.getId() + "." + ManagedBuildManager.getRandomNumber(), option.getName(), false); //$NON-NLS-1$
				newOption.setValue(val);
			}
		}
	}
}

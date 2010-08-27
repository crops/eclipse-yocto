package org.yocto.sdk.ide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.linuxtools.cdt.autotools.core.AutotoolsNewProjectNature;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IAConfiguration;
import org.yocto.sdk.ide.YoctoSDKUtils.SDKCheckRequestFrom;

@SuppressWarnings("restriction")
public class YoctoSDKProjectNature implements IProjectNature {
	public static final  String YoctoSDK_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKNature";
	private static final String WIZARD_WARNING_TITLE = "Wizard.SDK.Warning.Title";

	private static final String DEFAULT_GDB_SURFIX = "-gdb";
	private static final String DEFAULT_USR_BIN = "/usr/bin/";
	private static final String DEFAULT_ENV_FILE_PREFIX = "environment-setup-";
	private static final String DEFAULT_TMP_PREFIX = "/tmp/";

	private static final String DEFAULT_HOST_STR = "host";
	private static final String DEFAULT_TARGET_STR = "target";
	private static final String DEFAULT_BUILD_STR = "build";
	private static final String DEFAULT_AUTOGEN_OPT_STR = "autogenOpts";


	private static final String DEFAULT_CONFIGURE_STR = "configure";
	private static final String DEFAULT_AUTOGEN_STR = "autogen";

	private IProject proj;

	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return proj;
	}

	public void setProject(IProject project) {
		this.proj = project;
	}

	public static void addYoctoSDKNature(IProject project, IProgressMonitor monitor) throws CoreException {
		@SuppressWarnings("unused")
		ICommand[] command = project.getDescription().getBuildSpec();
		AutotoolsNewProjectNature.addNature(project, YoctoSDK_NATURE_ID, monitor);
	}


	public static void setEnvironmentVariables(IProject project, YoctoUIElement elem){
		String sFileName;
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);

		if (elem.getEnumPokyMode() == YoctoUIElement.PokyMode.POKY_SDK_MODE) {
			sFileName = elem.getStrToolChainRoot()+"/" + DEFAULT_ENV_FILE_PREFIX+elem.getStrTarget();
		}
		else {
			//POKY TREE Mode
			sFileName = elem.getStrToolChainRoot() + DEFAULT_TMP_PREFIX + DEFAULT_ENV_FILE_PREFIX + elem.getStrTarget();
		}

		HashMap<String, String> envMap = YoctoSDKUtils.parseEnvScript(sFileName);
		YoctoSDKUtils.setEnvVars(cpdesc, elem, envMap);

		try {
			ILaunchManager lManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType configType = 
				lManager.getLaunchConfigurationType("org.eclipse.ui.externaltools.ProgramLaunchConfigurationType");
			ILaunchConfigurationType debug_configType = 
				lManager.getLaunchConfigurationType("org.eclipse.rse.remotecdt.RemoteApplicationLaunch");
			String sPath = envMap.get("PATH");
			createRemoteDebugLauncher(project, debug_configType, elem.getStrTarget(), sPath);

			ArrayList<String> listValue = new ArrayList<String>();
			listValue.add(new String("org.eclipse.ui.externaltools.launchGroup"));
			if (elem.getEnumDeviceMode() == YoctoUIElement.DeviceMode.QEMU_MODE) {
				createQemuLauncher(project, configType, listValue, sFileName, elem);
			} 
			CoreModel.getDefault().setProjectDescription(project,cpdesc);
		} catch (CoreException e) {
			e.printStackTrace();
		}	
	}



	public static void configureAutotoolsOptions(IProject project) {
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		String id = icfg.getId();
		String command_prefix = "CFLAGS=\" -g -O0 " + YoctoSDKUtils.getEnvValue(project, "CFLAGS") 
		+ "\" CXXFLAGS=\" -g -O0 " + YoctoSDKUtils.getEnvValue(project, "CXXFLAGS") + "\"";
		String autogen_setting = command_prefix+" autogen.sh";
		String configure_setting = command_prefix + " configure";
		IAConfiguration cfg = AutotoolsConfigurationManager.getInstance().getConfiguration(project, id);
		String strConfigure = YoctoSDKUtils.getEnvValue(project, "CONFIGURE_FLAGS");

		cfg.setOption(DEFAULT_CONFIGURE_STR, configure_setting);
		cfg.setOption(DEFAULT_BUILD_STR, YoctoSDKUtils.splitString(strConfigure, "--build="));
		cfg.setOption(DEFAULT_HOST_STR, YoctoSDKUtils.splitString(strConfigure, "--host="));
		cfg.setOption(DEFAULT_TARGET_STR, YoctoSDKUtils.splitString(strConfigure, "--target="));
		cfg.setOption(DEFAULT_AUTOGEN_STR, autogen_setting);
		cfg.setOption(DEFAULT_AUTOGEN_OPT_STR, strConfigure);

		AutotoolsConfigurationManager.getInstance().addConfiguration(project, cfg);
		AutotoolsConfigurationManager.getInstance().saveConfigs(project);
	}

	public static void configureAutotools(IProject project) {

		YoctoUIElement elem = YoctoSDKUtils.getElemFromStore();

		YoctoSDKUtils.SDKCheckResults result = YoctoSDKUtils.checkYoctoSDK(elem);

		if (result == YoctoSDKUtils.SDKCheckResults.SDK_PASS){
			setEnvironmentVariables(project, elem);
			configureAutotoolsOptions(project);
		}
		else {
			String title   =  YoctoSDKMessages.getString(WIZARD_WARNING_TITLE);		
			String message =  YoctoSDKUtils.getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			MessageDialog.openWarning(YoctoSDKPlugin.getActiveWorkbenchShell(), title, message);
		}
	}

	protected static void createRemoteDebugLauncher(IProject project, 
			ILaunchConfigurationType configType,  String sTargetTriplet,
			String strPath) {
		try {

			String sDebugSubDir = DEFAULT_USR_BIN + sTargetTriplet;
			StringTokenizer token = new StringTokenizer(strPath, ":");
			String strDebugger = "";
			while (token.hasMoreTokens())
			{
				String sTemp = token.nextToken();
				if (sTemp.endsWith(sDebugSubDir)) {
					strDebugger = sTemp + "/" + sTargetTriplet + DEFAULT_GDB_SURFIX;
					break;
				}
			}
			if (strDebugger.isEmpty())
				return;
			//If get default Debugger successfully, go ahead!

			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(null, project.getName()+"_gdb_"+sTargetTriplet);
			w_copy.setAttribute("org.eclipse.cdt.debug.mi.core.AUTO_SOLIB", true);
			w_copy.setAttribute("org.eclipse.cdt.debug.mi.core.DEBUG_NAME", strDebugger);
			String projectName = project.getName();
			String project_src = "src/"+projectName;
			w_copy.setAttribute("org.eclipse.cdt.launch.PROJECT_ATTR", projectName);
			w_copy.setAttribute("org.eclipse.cdt.launch.PROGRAM_NAME", project_src);
			w_copy.setAttribute("org.eclipse.cdt.debug.mi.core.protocol", "mi");
			w_copy.doSave();
		} catch (CoreException e) {
		}

	}

	protected static void createQemuLauncher(IProject project, 
			ILaunchConfigurationType configType, 
			ArrayList<String> listValue, String sScriptFile,
			YoctoUIElement elem) {
		try {

			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(null, "qemu_"+elem.getStrTarget());

			w_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);		
			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", "/usr/bin/xterm");

			String argument = "-e \"source " + sScriptFile + ";runqemu-nfs " +
			elem.getStrQemuKernelLoc() + " " + elem.getStrQemuRootFSLoc() + ";bash\"";

			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS", argument);
			w_copy.doSave();
		} catch (CoreException e) {
		}

	}


}

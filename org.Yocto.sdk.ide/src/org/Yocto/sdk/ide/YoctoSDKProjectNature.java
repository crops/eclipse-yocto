package org.Yocto.sdk.ide;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.internal.core.*;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.utils.spawner.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.cdt.autotools.core.AutotoolsNewProjectNature;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IAConfiguration;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IConfigureOption;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckRequestFrom;
import org.Yocto.sdk.ide.YoctoSDKChecker.SDKCheckResults;
import org.Yocto.sdk.ide.preferences.PreferenceConstants;

@SuppressWarnings("restriction")
public class YoctoSDKProjectNature implements IProjectNature {
	public static final  String YoctoSDK_NATURE_ID = YoctoSDKPlugin.getUniqueIdentifier() + ".YoctoSDKNature";
	private static final String WIZARD_WARNING_TITLE = "Wizard.SDK.Warning.Title";
	private static final String DEFAULT_WHICH_COMMAND = "which";
	private static final String DEFAULT_WHICH_OPROFILEUI = "oprofile-viewer";
	private static final String DEFAULT_HOST_NAME_COMMAND = "uname -m";
	private static final String DEFAULT_POKY_SURFIX = "-poky-linux";
	private static final String DEFAULT_NATIVE_SYSROOTS_SURFIX = "-pokysdk-linux";
	private static final String DEFAULT_HOST = "i586-poky-linux";
	private static final String DEFAULT_NATIVE_SYSROOTS = "i586-pokysdk-linux";
	private static final String DEFAULT_SYSROOTS_STR = "/sysroots/";
	private static final String DEFAULT_USER_BIN_STR = "/usr/bin/";
	private static final String DEFAULT_BIN_STR = "/bin/";
	private static final String DEFAULT_PKGCONFIG_PATH_SURFIX = "/usr/lib/pkgconfig";
	private static final String DEFAULT_HOST_STR = "host";
	private static final String DEFAULT_TARGET_STR = "target";
	private static final String DEFAULT_POKY_BUILD_PREFIX = "/build/tmp/";
	private static final String DEFAULT_LINUX_STR = "-linux";
	
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
		//AutotoolsNewProjectNature.addAutotoolsNature(project, monitor);
		@SuppressWarnings("unused")
		ICommand[] command = project.getDescription().getBuildSpec();
		AutotoolsNewProjectNature.addNature(project, YoctoSDK_NATURE_ID, monitor);		
	}
	
	public static void setEnvironmentVariables(IProject project, 
												String sdkroot,
												String toolchain_location, 
												String target,
												String target_qemu,
												String qemu_kernel,
												String qemu_rootfs,
												String env_script,
												String ip_addr){
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		String delimiter = manager.getDefaultDelimiter();

		//Add store to the per project configuration
		env.addVariable(PreferenceConstants.SDK_ROOT, sdkroot, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.TOOLCHAIN_ROOT, toolchain_location, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.TARGET, target, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.TARGET_QEMU, target_qemu, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		env.addVariable(PreferenceConstants.SETUP_ENV_SCRIPT, env_script, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		// PATH
		String sys_path    = System.getenv("PATH");
		String Yocto_path = "";
		String host = getHostName();
		host = host.substring(0, host.length() - 1);
	
		if (sdkroot.equals("true")) {
			if (host.contains("i") && host.contains("86")) {
				host = DEFAULT_NATIVE_SYSROOTS;
			} else
				host = host + DEFAULT_NATIVE_SYSROOTS_SURFIX;
			Yocto_path = toolchain_location + DEFAULT_SYSROOTS_STR + host + DEFAULT_USER_BIN_STR; 
			String target_arg = "";
			StringTokenizer tok= new StringTokenizer(target, "-"); //$NON-NLS-1$
			if (tok.hasMoreTokens()) {
				target_arg= tok.nextToken() + DEFAULT_POKY_SURFIX;
			};
			
			// PKG_CONFIG_SYSROOT_DIR
			String Yocto_pkg_sys_root = toolchain_location  + DEFAULT_SYSROOTS_STR + target_arg;
			env.addVariable("PKG_CONFIG_SYSROOT_DIR", Yocto_pkg_sys_root, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

			// PKG_CONFIG_PATH
	        String Yocto_pkg_path = Yocto_pkg_sys_root + DEFAULT_PKGCONFIG_PATH_SURFIX;
	        //String Yocto_pkg_path2 = Yocto_pkg_sys_root + File.separator + "usr" + File.separator + "share" + File.separator + "pkgconfig";
	        //String Yocto_pkg_path =  Yocto_pkg_path1 + delimiter + Yocto_pkg_path2;
	        env.addVariable("PKG_CONFIG_PATH", Yocto_pkg_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		} else {
			//Use poky tree
			String Yocto_path_prefix = toolchain_location + DEFAULT_POKY_BUILD_PREFIX + DEFAULT_SYSROOTS_STR + host + DEFAULT_LINUX_STR;
			Yocto_path = Yocto_path_prefix + DEFAULT_USER_BIN_STR + delimiter + Yocto_path_prefix + DEFAULT_BIN_STR;
		}
		if (sys_path != null) {
			Yocto_path = Yocto_path + delimiter + sys_path;
		} 
		env.addVariable("PATH", Yocto_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		if (target_qemu.equals("true")) {
			env.addVariable(PreferenceConstants.QEMU_KERNEL, qemu_kernel, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			env.addVariable(PreferenceConstants.QEMU_ROOTFS, qemu_rootfs, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			env.addVariable(PreferenceConstants.IP_ADDR, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
			
			try {
				CoreModel.getDefault().setProjectDescription(project, cpdesc);
				ILaunchManager lManager = DebugPlugin.getDefault().getLaunchManager();
				ILaunchConfigurationType[] types = lManager.getLaunchConfigurationTypes();
				ILaunchMode[] modes = lManager.getLaunchModes();
				ILaunchConfigurationType configType = lManager.getLaunchConfigurationType("org.eclipse.ui.externaltools.ProgramLaunchConfigurationType");
				ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(project, "qemu"+"_"+project.getName());
				ArrayList listValue = new ArrayList();
				listValue.add(new String("org.eclipse.ui.externaltools.launchGroup"));
				w_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);
				
				w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE", "${projects:}");
				w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", "/usr/bin/xterm");
				String argument = "-e \"source " + env_script + ";poky-qemu " + qemu_kernel + " " + qemu_rootfs+";bash\"";
				//w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS", "-e \"source /usr/local/poky/eabi-glibc/environment-setup-i586-poky-linux;poky-qemu /home/jzhang/poky-purple-3.2.1/build/tmp/deploy/images/bzImage-qemux86.bin /home/jzhang/poky-purple-3.2.1/build/tmp/deploy/images/poky-image-sdk-qemux86.ext3;bash\"");
				w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS", argument);
				w_copy.doSave();
				createOProfileUI(project, configType, listValue);
			} catch (CoreException e) {
				// do nothing
			}	
		} else {
			env.addVariable(PreferenceConstants.QEMU_KERNEL, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
			env.addVariable(PreferenceConstants.QEMU_ROOTFS, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
			env.addVariable(PreferenceConstants.IP_ADDR, ip_addr, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		}
		
	}
	
	public static void configureAutotoolsOptions(IProject project, String target) {
		String target_arg = "";
		StringTokenizer tok= new StringTokenizer(target, "-"); //$NON-NLS-1$
		if (tok.hasMoreTokens()) {
			target_arg= tok.nextToken() + DEFAULT_POKY_SURFIX;
		};

		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		String id = icfg.getId();
		String name = icfg.getName();
		IAConfiguration cfg = AutotoolsConfigurationManager.getInstance().getConfiguration(project, id);
	    cfg.setOption(DEFAULT_HOST_STR, target_arg);
	    cfg.setOption(DEFAULT_TARGET_STR, target_arg);
	    AutotoolsConfigurationManager.getInstance().addConfiguration(project, cfg);
	    /*
		Collection<IConfigureOption> values = cfg.getOptions().values(); 
		for (Iterator<IConfigureOption> j = values.iterator(); j.hasNext();) {
			IConfigureOption opt = j.next();
			String optName = opt.getName();
			String value;
			if (optName.equals("host")){
				value = opt.getValue();
				opt.setValue(host);
			} else if (optName.equals("target")){
				value = opt.getValue();
				opt.setValue(target_arg);
			}
		}
		*/
		AutotoolsConfigurationManager.getInstance().saveConfigs(project);
		
	}
	
	public static void configureAutotools(IProject project) {
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		String sdkroot = store.getString(PreferenceConstants.SDK_ROOT);
		String sdk_location  = store.getString(PreferenceConstants.TOOLCHAIN_ROOT);
		String target  = store.getString(PreferenceConstants.TARGET);
		String target_qemu = store.getString(PreferenceConstants.TARGET_QEMU);
		String qemu_kernel = store.getString(PreferenceConstants.QEMU_KERNEL);
		String qemu_rootfs = store.getString(PreferenceConstants.QEMU_ROOTFS);
		String env_script = store.getString(PreferenceConstants.SETUP_ENV_SCRIPT);
		String ip_addr = store.getString(PreferenceConstants.IP_ADDR);

		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, sdk_location, target, target_qemu, qemu_kernel, qemu_rootfs, env_script, ip_addr);
		if (result == SDKCheckResults.SDK_PASS){
			setEnvironmentVariables(project, sdkroot, sdk_location, target, target_qemu, qemu_kernel, qemu_rootfs, env_script, ip_addr);
			configureAutotoolsOptions(project, target);
		}else {
			String title   =  YoctoSDKMessages.getString(WIZARD_WARNING_TITLE);		
			String message =  YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			MessageDialog.openWarning(YoctoSDKPlugin.getActiveWorkbenchShell(), title, message);
		}
	}
	
	protected static void createOProfileUI(IProject project, ILaunchConfigurationType configType, ArrayList listValue) {
		//Create one instance of OProfileUI launcher
		IWorkspaceRoot wp_root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = wp_root.getProjects();
		
		boolean found_oprofile_launcher = false;
		for (int i = 0; i < projects.length; i++) {
			IProject project_i = projects[i];
			String oprofile_str = "OProfileUI.launch";
			IFile oprofile_file = project_i.getFile(oprofile_str);
			if (oprofile_file.exists()) {
				found_oprofile_launcher = true;
				break;
			} 
		}
		if (!found_oprofile_launcher) {
			String oprofileUI_str = getOProfileUI();
			if ((oprofileUI_str.isEmpty()) || (oprofileUI_str == null))
				return;
			try {
				ILaunchConfigurationWorkingCopy oprofile_copy = configType.newInstance(project, "OProfileUI");
				
				oprofile_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);
				oprofile_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE", "${projects:}");
				oprofile_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", oprofileUI_str);
				oprofile_copy.doSave();
			} catch (CoreException e) {
			}
		}
	}
	
	protected static String getOProfileUI() {
		
        String which_command_str = DEFAULT_WHICH_COMMAND + " " + DEFAULT_WHICH_OPROFILEUI;
        try {
        	Process proc = ProcessFactory.getFactory().exec(which_command_str);
            if (proc != null) {
            	ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            	ByteArrayOutputStream errstream = new ByteArrayOutputStream();
            	ProcessClosure closure = new ProcessClosure(proc, outstream, errstream);
            	closure.runBlocking();
            	if (outstream.size() != 0)
            		return outstream.toString();
            }
        } catch (IOException e) {
        }
        return "";
	}
	
	protected static String getHostName() {
		String host_name_command = DEFAULT_HOST_NAME_COMMAND;
		try {
			Process proc = ProcessFactory.getFactory().exec(host_name_command);
			if (proc != null) {
				ByteArrayOutputStream outstream = new ByteArrayOutputStream();
				ByteArrayOutputStream errstream = new ByteArrayOutputStream();
	            ProcessClosure closure = new ProcessClosure(proc, outstream, errstream);
	            closure.runBlocking();
	            if (outstream.size() != 0)
	            	return outstream.toString();
			}
		} catch (IOException e) {
	    }
	    return "";
	}
}

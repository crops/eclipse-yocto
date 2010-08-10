package org.Yocto.sdk.ide;

import java.util.ArrayList;
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
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.cdt.autotools.core.AutotoolsNewProjectNature;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IAConfiguration;
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
	private static final String DEFAULT_GDB_SURFIX = "-gdb";

	private static final String DEFAULT_SYSROOTS_STR = "/sysroots/";
	private static final String DEFAULT_USER_BIN_STR = "/usr/bin";
	private static final String DEFAULT_BIN_STR = "/bin/";
	
	private static final String DEFAULT_HOST_STR = "host";
	private static final String DEFAULT_TARGET_STR = "target";
	private static final String DEFAULT_BUILD_STR = "build";
	private static final String DEFAULT_AUTOGEN_OPT_STR = "autogenOpts";
	private static final String DEFAULT_POKY_BUILD_PREFIX = "/build/tmp/";
	private static final String DEFAULT_LINUX_STR = "-linux";
	
	private static final String DEFAULT_OPTION_PREFIX_STR = "--";
	private static final String DEFAULT_CONFIGURE_STR = "configure";
	private static final String DEFAULT_AUTOGEN_STR = "autogen";
	
	private static IProgressMonitor myMonitor;
	private static String target_str = "";
	private static String host_str = "";
	private static String build_str = "";
	private static String CFLAGS_str = "";
	private static String CXXFLAGS_str = "";
	private static String CONFIGURE_FLAGS_str = "";
	private static String env_script = "";
	
	private IProject proj;
	private static String Yocto_native_path = "";

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
		myMonitor = monitor;
	}
	
	public static void setEnvironmentVariables(IProject project, 
			String sdkroot,
			String toolchain_location, 
			String target,
			String target_qemu,
			String qemu_kernel,
			//String qemu_rootfs,
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
		
		// PATH
		String sys_path    = System.getenv("PATH");
		String Yocto_path = "";
		String host = getHostName();
		
		if (sdkroot.equals("true")) {
			try {
				env_script = toolchain_location+"/"+"environment-setup-"+target+DEFAULT_POKY_SURFIX;
				File env_script_file = new File(env_script);
				if (env_script_file.exists()) {
					BufferedReader input = new BufferedReader(new FileReader(env_script_file));
					try {
						String line = null;
						
						while ((line = input.readLine()) != null) {
							if (line.contains("PATH")&& line.contains("PKG_CONFIG_PATH")) {
								String Yocto_pkg_path = line.substring(line.indexOf('=')+1);
						        env.addVariable("PKG_CONFIG_PATH", Yocto_pkg_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
								
							} else if (line.contains("PKG_CONFIG_SYSROOT_DIR")) {
								String Yocto_pkg_sys_root = line.substring(line.indexOf('=')+1);
								env.addVariable("PKG_CONFIG_SYSROOT_DIR", Yocto_pkg_sys_root, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
							} else if (line.contains("PATH") && !line.contains("LD_LIBRARY_PATH")) {
								Yocto_path = line.substring(line.indexOf('=')+1, line.indexOf('$'));
								Yocto_native_path = Yocto_path.substring(0, Yocto_path.length() - 1);
								if (sys_path != null) {
									Yocto_path = Yocto_path + sys_path;
								}
								env.addVariable("PATH", Yocto_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
							} else if (line.contains("CONFIG_SITE")) {
								String Yocto_pkg_config_site = line.substring(line.indexOf('=') + 1);
								
								env.addVariable("CONFIG_SITE", Yocto_pkg_config_site, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
							} else if (line.contains("CONFIGURE_FLAGS")) {
								int begin_index = 0;
								int end_index = 0;
								begin_index = line.indexOf('=') + 2;
								end_index = line.indexOf('"', begin_index);
								CONFIGURE_FLAGS_str = line.substring(begin_index, end_index);
								env.addVariable("CONFIGURE_FLAGS_str", CONFIGURE_FLAGS_str, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
								int index = line.indexOf("target");
								begin_index = line.indexOf('=', index);
								end_index = line.indexOf(' ', index);
								target_str = line.substring(begin_index+1, end_index);
								
								index = line.indexOf("host");
								begin_index = line.indexOf('=', index);
								end_index = line.indexOf(' ', index);
								host_str = line.substring(begin_index+1, end_index);
								
								index = line.indexOf("build");
								begin_index = line.indexOf('=', index);
								end_index = line.indexOf('"', index);
								build_str = line.substring(begin_index+1, end_index);
							} else if (line.contains("CFLAGS")) {
								CFLAGS_str = line.substring(line.indexOf('"') +1);
							} else if (line.contains("CXXFLAGS")){
								CXXFLAGS_str = line.substring(line.indexOf('"') + 1);
							}
									
						}
					} finally {
						input.close();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				
			}
			
		} else {
			//Use poky tree
			String Yocto_path_prefix = toolchain_location + DEFAULT_POKY_BUILD_PREFIX + DEFAULT_SYSROOTS_STR + host + DEFAULT_LINUX_STR;
			Yocto_path = Yocto_path_prefix + DEFAULT_USER_BIN_STR + delimiter + Yocto_path_prefix + DEFAULT_BIN_STR;
		}
		try {
			ILaunchManager lManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType configType = lManager.getLaunchConfigurationType("org.eclipse.ui.externaltools.ProgramLaunchConfigurationType");
			ILaunchConfigurationType debug_configType = lManager.getLaunchConfigurationType("org.eclipse.rse.remotecdt.RemoteApplicationLaunch");
			String cross_debugger = Yocto_native_path+"/"+target_str+DEFAULT_GDB_SURFIX;
			createRemoteDebugLauncher(project, debug_configType, cross_debugger);
			ArrayList<String> listValue = new ArrayList<String>();
			listValue.add(new String("org.eclipse.ui.externaltools.launchGroup"));
			if (target_qemu.equals(IPreferenceStore.TRUE)) {
				env.addVariable(PreferenceConstants.QEMU_KERNEL, qemu_kernel, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
				//env.addVariable(PreferenceConstants.QEMU_ROOTFS, qemu_rootfs, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
				env.addVariable(PreferenceConstants.IP_ADDR, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
			   
				//createOProfileUI(project, configType, listValue);
				createQemuLauncher(project, configType, listValue, qemu_kernel);
			
			} else {
				env.addVariable(PreferenceConstants.QEMU_KERNEL, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
				//env.addVariable(PreferenceConstants.QEMU_ROOTFS, "", IEnvironmentVariable.ENVVAR_REMOVE, delimiter, ccdesc);
				env.addVariable(PreferenceConstants.IP_ADDR, ip_addr, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
			}
			createOProfileUI(project, configType, listValue);
			CoreModel.getDefault().setProjectDescription(project,cpdesc);
		} catch (CoreException e) {
			e.printStackTrace();
		}	
	}
	
	public static void configureAutotoolsOptions(IProject project, String target) {
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		String id = icfg.getId();
		
		String command_prefix = "CFLAGS=\" -g -O0 " + CFLAGS_str + " CXXFLAGS=\" -g -O0 " + CXXFLAGS_str + "";
		String autogen_setting = command_prefix+" autogen.sh";
		String configure_setting = command_prefix + " configure";
		IAConfiguration cfg = AutotoolsConfigurationManager.getInstance().getConfiguration(project, id);
	    cfg.setOption(DEFAULT_CONFIGURE_STR, configure_setting);
		cfg.setOption(DEFAULT_BUILD_STR, build_str);
	    cfg.setOption(DEFAULT_HOST_STR, host_str);
	    cfg.setOption(DEFAULT_TARGET_STR, target_str);
	    cfg.setOption(DEFAULT_AUTOGEN_STR, autogen_setting);
	    cfg.setOption(DEFAULT_AUTOGEN_OPT_STR, CONFIGURE_FLAGS_str);
	    AutotoolsConfigurationManager.getInstance().addConfiguration(project, cfg);
		AutotoolsConfigurationManager.getInstance().saveConfigs(project);
		
	}
	
	public static void configureAutotools(IProject project) {
		IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
		String sdkroot = store.getString(PreferenceConstants.SDK_ROOT);
		String sdk_location  = store.getString(PreferenceConstants.TOOLCHAIN_ROOT);
		String target  = store.getString(PreferenceConstants.TARGET);
		String target_qemu = store.getString(PreferenceConstants.TARGET_QEMU);
		String qemu_kernel = store.getString(PreferenceConstants.QEMU_KERNEL);
		//String qemu_rootfs = store.getString(PreferenceConstants.QEMU_ROOTFS);
		String ip_addr = store.getString(PreferenceConstants.IP_ADDR);

		//SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, sdk_location, target, target_qemu, qemu_kernel, qemu_rootfs, ip_addr);
		SDKCheckResults result = YoctoSDKChecker.checkYoctoSDK(sdkroot, sdk_location, target, target_qemu, qemu_kernel, ip_addr);
		if (result == SDKCheckResults.SDK_PASS){
			setEnvironmentVariables(project, sdkroot, sdk_location, target, target_qemu, qemu_kernel, ip_addr);
			configureAutotoolsOptions(project, target);
		}else {
			String title   =  YoctoSDKMessages.getString(WIZARD_WARNING_TITLE);		
			String message =  YoctoSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			MessageDialog.openWarning(YoctoSDKPlugin.getActiveWorkbenchShell(), title, message);
		}
	}
	
	protected static void createRemoteDebugLauncher(IProject project, 
											ILaunchConfigurationType configType,  
											String cross_gdb) {
		try {
			String gdb_surfix = target_str;
			if (gdb_surfix.contains("86")) {
				gdb_surfix = "X86";
			}

			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(null, project.getName()+"_gdb_"+gdb_surfix);
			w_copy.setAttribute("org.eclipse.cdt.debug.mi.core.AUTO_SOLIB", true);
			w_copy.setAttribute("org.eclipse.cdt.debug.mi.core.DEBUG_NAME", cross_gdb);
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
											ArrayList<String> listValue, 
											String qemu_kernel) {
		try {
			String qemu_surfix = target_str;
			if (qemu_surfix.contains("86")) {
				qemu_surfix = "X86";
			}
		
			ILaunchConfigurationWorkingCopy w_copy = configType.newInstance(null, "qemu_"+qemu_surfix);
	
			w_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);
		
			//w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE", "${projects:}");
			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", "/usr/bin/xterm");
			//String argument = "-e \"source " + env_script + ";poky-qemu " + qemu_kernel + " " + qemu_rootfs+";bash\"";
			String argument = "-e \"source " + env_script + ";poky-qemu " + qemu_kernel + " ;bash\"";
		
			w_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS", argument);
			w_copy.doSave();
		} catch (CoreException e) {
		}
		
	}
	protected static void createOProfileUI(IProject project, ILaunchConfigurationType configType, ArrayList<String> listValue) {
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
				ILaunchConfigurationWorkingCopy oprofile_copy = configType.newInstance(null, "OProfileUI");
				
				oprofile_copy.setAttribute("org.eclipse.debug.ui.favoriteGroups", listValue);
				//oprofile_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE", "${projects:}");
				oprofile_copy.setAttribute("org.eclipse.ui.externaltools.ATTR_LOCATION", oprofileUI_str);
				oprofile_copy.doSave();
			} catch (CoreException e) {
			}
		}
	}
	
	protected static String getOProfileUI() {
		
		String file_str = Yocto_native_path + "/oprofile-viewer";
		File Yocto_oprofile = new File(file_str);
		if (Yocto_oprofile.exists())
			return file_str;
		
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

package org.Yocto.sdk.ide.actions;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.Yocto.sdk.ide.YoctoSDKMessages;
import org.Yocto.sdk.ide.YoctoSDKPlugin;
import org.Yocto.sdk.ide.YoctoSDKProjectNature;
import org.Yocto.sdk.ide.preferences.PreferenceConstants;

@SuppressWarnings("restrictionS")
public class ReconfigYoctoAction extends InvokeAction {
	private static final String SDK_LOCATION = "Preferences.SDK.Root.Name";
	private static final String SDK_TRIPLET  = "Preferences.Toolchain.Triplet.Name";
	private static final String DIALOG_TITLE  = "Menu.SDK.Dialog.Title";
	private static final String CONSOLE_MESSAGE  = "Menu.SDK.Console.Configure.Message";
	private static final String QEMU_KERNEL = "Preferences.QEMU.Kernel.Name";
	private static final String QEMU_ROOTFS = "Preferences.QEMU.ROOTFS.Name";
	private static final String SETUP_ENV_SCRIPT = "Preferences.SetupEnv.Script.Name";
	
	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;
		
		IProject project = container.getProject();
		String sdk_location = "";
		String toolchain_triplet = "";
		String qemu_kernel = "";
		String qemu_rootfs = "";
		String env_script = "";
		
		//Try to get the per project configuration first
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		IEnvironmentVariable sdk_loc_env = env.getVariable("sdk_location", ccdesc);
		IEnvironmentVariable host_alias_env = env.getVariable("host_alias", ccdesc);
		IEnvironmentVariable qemu_kernel_env = env.getVariable("qemu_kernel", ccdesc);
		IEnvironmentVariable qemu_rootfs_env = env.getVariable("qemu_rootfs", ccdesc);
		IEnvironmentVariable env_script_env = env.getVariable("env_setup_script", ccdesc);
		
		if ((sdk_loc_env == null) || (host_alias_env == null) || (qemu_kernel_env == null) ||
				(qemu_rootfs_env == null) || (env_script_env == null)){
			// No project environment has been set yet, use the Preference values
			IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
			sdk_location  = store.getString(PreferenceConstants.SDK_LOCATION);
			toolchain_triplet  = store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);
			qemu_kernel = store.getString(PreferenceConstants.QEMU_KERNEL);
			qemu_rootfs = store.getString(PreferenceConstants.QEMU_ROOTFS);
			env_script = store.getString(PreferenceConstants.SETUP_ENV_SCRIPT);
		} else {
			// Use the per project settings
			sdk_location = sdk_loc_env.getValue();
			toolchain_triplet = host_alias_env.getValue();
			qemu_kernel = qemu_kernel_env.getValue();
			qemu_rootfs = qemu_rootfs_env.getValue();
			env_script = env_script_env.getValue();
		}
		
		SDKLocationDialog optionDialog = new SDKLocationDialog(
				new Shell(),
				YoctoSDKMessages.getString(DIALOG_TITLE),
				YoctoSDKMessages.getString(SDK_LOCATION),
				sdk_location,
				YoctoSDKMessages.getString(SDK_TRIPLET),
				toolchain_triplet,
				YoctoSDKMessages.getString(QEMU_KERNEL),
				qemu_kernel,
				YoctoSDKMessages.getString(QEMU_ROOTFS),
				qemu_rootfs,
				YoctoSDKMessages.getString(SETUP_ENV_SCRIPT),
				env_script,
				null);

		optionDialog.open();
		
		String location = optionDialog.getSDKLocation();
		String triplet  = optionDialog.getToolchainTriplet(); 
		String kernel = optionDialog.getQEMUKernel();
		String rootfs = optionDialog.getQEMURootfs();
		String script = optionDialog.getEnvScript();
		if (location != null) {
			
			YoctoSDKProjectNature.setEnvironmentVariables(project, location, triplet, kernel, rootfs, script);
			//YoctoSDKProjectNature.configureAutotoolsOptions(project, location, triplet);
			try {
				IConsole console = CCorePlugin.getDefault().getConsole("org.Yocto.sdk.ide.YoctoConsole");
				console.start(project);
				ConsoleOutputStream consoleOutStream;
			
				consoleOutStream = console.getOutputStream();
				String messages = YoctoSDKMessages.getString(CONSOLE_MESSAGE);
				consoleOutStream.write(messages.getBytes());
				consoleOutStream.flush();
				consoleOutStream.close();
			} catch (CoreException e1) {
			} catch (IOException e2) {
			}
		}
	}

	public void dispose() {

	}
}

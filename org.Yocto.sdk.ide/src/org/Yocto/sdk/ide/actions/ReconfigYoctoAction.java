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
	private static final String TOOLCHAIN_LOCATION = "Preferences.Toolchain.Root.Name";
	private static final String TARGET  = "Preferences.Target.Name";
	private static final String DIALOG_TITLE  = "Menu.SDK.Dialog.Title";
	private static final String CONSOLE_MESSAGE  = "Menu.SDK.Console.Configure.Message";
	private static final String QEMU_KERNEL = "Preferences.QEMU.Kernel.Name";
	private static final String QEMU_ROOTFS = "Preferences.QEMU.ROOTFS.Name";
	private static final String SETUP_ENV_SCRIPT = "Preferences.SetupEnv.Script.Name";
	private static final String IP_ADDR = "Preferences.IP.Addr.Name";
	private static final String SDK_ROOT = "Preferences.SDK.Root.Name";
	private static final String TARGET_QEMU = "Preferences.Target.Qemu.Name";
	
	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;
		
		IProject project = container.getProject();
		String toolchain_root = "";
		String target = "";
		String qemu_kernel = "";
		String qemu_rootfs = "";
		String env_script = "";
		String ip_addr = "";
		String sdk_root = "";
		String target_qemu = "";
		
		//Try to get the per project configuration first
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		IEnvironmentVariable toolchain_env = env.getVariable(PreferenceConstants.TOOLCHAIN_ROOT, ccdesc);
		IEnvironmentVariable target_env = env.getVariable(PreferenceConstants.TARGET, ccdesc);
		IEnvironmentVariable qemu_kernel_env = env.getVariable(PreferenceConstants.QEMU_KERNEL, ccdesc);
		IEnvironmentVariable qemu_rootfs_env = env.getVariable(PreferenceConstants.QEMU_ROOTFS, ccdesc);
		IEnvironmentVariable env_script_env = env.getVariable(PreferenceConstants.SETUP_ENV_SCRIPT, ccdesc);
		IEnvironmentVariable ip_addr_env = env.getVariable(PreferenceConstants.IP_ADDR, ccdesc);
		IEnvironmentVariable sdkroot_env = env.getVariable(PreferenceConstants.SDK_ROOT, ccdesc);
		IEnvironmentVariable targetqemu_env = env.getVariable(PreferenceConstants.TARGET_QEMU, ccdesc);
		
		if ((toolchain_env == null) || (target_env == null)){
			// No project environment has been set yet, use the Preference values
			IPreferenceStore store = YoctoSDKPlugin.getDefault().getPreferenceStore();
			toolchain_root  = store.getString(PreferenceConstants.TOOLCHAIN_ROOT);
			target  = store.getString(PreferenceConstants.TARGET);
			qemu_kernel = store.getString(PreferenceConstants.QEMU_KERNEL);
			qemu_rootfs = store.getString(PreferenceConstants.QEMU_ROOTFS);
			env_script = store.getString(PreferenceConstants.SETUP_ENV_SCRIPT);
			ip_addr = store.getString(PreferenceConstants.IP_ADDR);
			sdk_root = store.getString(PreferenceConstants.SDK_ROOT);
			target_qemu = store.getString(PreferenceConstants.TARGET_QEMU);
		} else {
			// Use the per project settings
			toolchain_root = toolchain_env.getValue();
			target  = target_env.getValue();
			if (qemu_kernel_env != null)
				qemu_kernel = qemu_kernel_env.getValue();
			if (qemu_rootfs_env != null)
				qemu_rootfs = qemu_rootfs_env.getValue();
			if (env_script_env != null)
				env_script = env_script_env.getValue();
			if (ip_addr_env != null)
				ip_addr = ip_addr_env.getValue();
			if (sdkroot_env != null)
				sdk_root = sdkroot_env.getValue();
			if (targetqemu_env != null)
				target_qemu = targetqemu_env.getValue();
		}
		
		SDKLocationDialog optionDialog = new SDKLocationDialog(
				new Shell(),
				YoctoSDKMessages.getString(DIALOG_TITLE),
				YoctoSDKMessages.getString(SDK_ROOT),
				sdk_root,
				YoctoSDKMessages.getString(TOOLCHAIN_LOCATION),
				toolchain_root,
				YoctoSDKMessages.getString(TARGET),
				target,
				YoctoSDKMessages.getString(TARGET_QEMU),
				target_qemu,
				YoctoSDKMessages.getString(QEMU_KERNEL),
				qemu_kernel,
				YoctoSDKMessages.getString(QEMU_ROOTFS),
				qemu_rootfs,
				YoctoSDKMessages.getString(SETUP_ENV_SCRIPT),
				env_script,
				YoctoSDKMessages.getString(IP_ADDR),
				ip_addr,
				null);

		optionDialog.open();
		
		String SDK = optionDialog.getSDKRoot();
		String location = optionDialog.getToolchainLocation();
		String targetRet = optionDialog.getTarget(); 
		String qemu = optionDialog.getTargetQemu();
		String kernel = optionDialog.getQEMUKernel();
		String rootfs = optionDialog.getQEMURootfs();
		String script = optionDialog.getEnvScript();
		String ipaddr = optionDialog.getIPAddr();
		if (location != null) {			
			YoctoSDKProjectNature.setEnvironmentVariables(project, SDK, location, targetRet, qemu, kernel, rootfs, script, ipaddr);
			YoctoSDKProjectNature.configureAutotoolsOptions(project, targetRet);
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

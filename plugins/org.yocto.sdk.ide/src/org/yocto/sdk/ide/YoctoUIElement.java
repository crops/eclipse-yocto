package org.yocto.sdk.ide;



public class YoctoUIElement {
	public enum PokyMode
	{
		POKY_SDK_MODE,
		POKY_TREE_MODE
	};
	public enum DeviceMode
	{
		QEMU_MODE,
		DEVICE_MODE
	};

	private String strTarget;
	private String[] strTargetsArray;
	private DeviceMode enumDeviceMode;
	private String strQemuKernelLoc;
	private String strQemuRootFSLoc;
	private String strDeviceIP;
	private PokyMode enumPokyMode;
	private String strToolChainRoot;
	private int intTargetIndex;	

	public YoctoUIElement()
	{
		this.enumDeviceMode = DeviceMode.QEMU_MODE;
		this.enumPokyMode = PokyMode.POKY_SDK_MODE;
		this.strDeviceIP = "";
		this.strToolChainRoot = "";
		this.strQemuKernelLoc = "";
		this.strQemuRootFSLoc = "";
		this.intTargetIndex = -1;
		this.strTarget = "";
	}

	public PokyMode getEnumPokyMode() {
		return enumPokyMode;
	}
	public void setEnumPokyMode(PokyMode enumPokyMode) {
		this.enumPokyMode = enumPokyMode;
	}
	public String getStrToolChainRoot() {
		return strToolChainRoot;
	}
	public void setStrToolChainRoot(String strToolChainRoot) {
		this.strToolChainRoot = strToolChainRoot;
	}
	public int getIntTargetIndex() {
		return intTargetIndex;
	}
	public void setIntTargetIndex(int intTargetIndex) {
		this.intTargetIndex = intTargetIndex;
	}
	public String getStrTarget() {
		return strTarget;
	}
	public void setStrTarget(String strTarget) {
		this.strTarget = strTarget;
	}
	public String[] getStrTargetsArray() {
		return strTargetsArray;
	}
	public void setStrTargetsArray(String[] strTargetsArray) {
		this.strTargetsArray = strTargetsArray;
	}
	public DeviceMode getEnumDeviceMode() {
		return enumDeviceMode;
	}
	public void setEnumDeviceMode(DeviceMode enumDeviceMode) {
		this.enumDeviceMode = enumDeviceMode;
	}
	public String getStrQemuKernelLoc() {
		return strQemuKernelLoc;
	}
	public void setStrQemuKernelLoc(String strQemuKernelLoc) {
		this.strQemuKernelLoc = strQemuKernelLoc;
	}
	public String getStrQemuRootFSLoc() {
		return strQemuRootFSLoc;
	}
	public void setStrQemuRootFSLoc(String strQemuRootFSLoc) {
		this.strQemuRootFSLoc = strQemuRootFSLoc;
	}
	public String getStrDeviceIP() {
		return strDeviceIP;
	}
	public void setStrDeviceIP(String strDeviceIP) {
		this.strDeviceIP = strDeviceIP;
	}


}

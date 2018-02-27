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
package org.yocto.sdk.ide;

public class YoctoUIElement {
	public enum YoctoMode
	{
		YOCTO_SDK_MODE,
		YOCTO_TREE_MODE
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
	private String strQemuOption;
	private String strSysrootLoc;
	private YoctoMode enumYoctoMode;
	private String strToolChainRoot;
	private int intTargetIndex;	

	public YoctoUIElement()
	{
		this.enumDeviceMode = DeviceMode.QEMU_MODE;
		this.enumYoctoMode = YoctoMode.YOCTO_SDK_MODE;
		this.strToolChainRoot = "";
		this.strQemuKernelLoc = "";
		this.strQemuOption = "";
		this.strSysrootLoc = "";
		this.intTargetIndex = -1;
		this.strTarget = "";
	}

	public YoctoMode getEnumYoctoMode() {
		return enumYoctoMode;
	}
	public void setEnumYoctoMode(YoctoMode enumYoctoMode) {
		this.enumYoctoMode = enumYoctoMode;
	}
	public String getStrToolChainRoot() {
		return strToolChainRoot;
	}
	public void setStrToolChainRoot(String strToolChainRoot) {
		this.strToolChainRoot = strToolChainRoot;
	}
	public int getIntTargetIndex() {
		if ((this.strTargetsArray != null) && (this.strTargetsArray.length == 1))
			return 0;
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
	public String getStrQemuOption() {
		return strQemuOption;
	}
	public void setStrQemuOption(String strQemuOption) {
		this.strQemuOption = strQemuOption;
	}
	public String getStrSysrootLoc() {
		return strSysrootLoc;
	}
	public void setStrSysrootLoc(String strSysrootLoc) {
		this.strSysrootLoc = strSysrootLoc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((enumDeviceMode == null) ? 0 : enumDeviceMode.hashCode());
		result = prime * result + ((enumYoctoMode == null) ? 0 : enumYoctoMode.hashCode());
		result = prime * result + intTargetIndex;
		result = prime * result + ((strQemuKernelLoc == null) ? 0 : strQemuKernelLoc.hashCode());
		result = prime * result + ((strQemuOption == null) ? 0 : strQemuOption.hashCode());
		result = prime * result + ((strSysrootLoc == null) ? 0 : strSysrootLoc.hashCode());
		result = prime * result + ((strTarget == null) ? 0 : strTarget.hashCode());
		result = prime * result + ((strToolChainRoot == null) ? 0 : strToolChainRoot.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		YoctoUIElement other = (YoctoUIElement) obj;
		if (enumDeviceMode != other.enumDeviceMode)
			return false;
		if (enumYoctoMode != other.enumYoctoMode)
			return false;
		if (intTargetIndex != other.intTargetIndex)
			return false;
		if (strQemuKernelLoc == null) {
			if (other.strQemuKernelLoc != null)
				return false;
		} else if (!strQemuKernelLoc.equals(other.strQemuKernelLoc))
			return false;
		if (strQemuOption == null) {
			if (other.strQemuOption != null)
				return false;
		} else if (!strQemuOption.equals(other.strQemuOption))
			return false;
		if (strSysrootLoc == null) {
			if (other.strSysrootLoc != null)
				return false;
		} else if (!strSysrootLoc.equals(other.strSysrootLoc))
			return false;
		if (strTarget == null) {
			if (other.strTarget != null)
				return false;
		} else if (!strTarget.equals(other.strTarget))
			return false;
		if (strToolChainRoot == null) {
			if (other.strToolChainRoot != null)
				return false;
		} else if (!strToolChainRoot.equals(other.strToolChainRoot))
			return false;
		return true;
	}
}

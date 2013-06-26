package org.yocto.bc.ui.filesystem;

import java.io.File;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.yocto.remote.utils.RemoteHelper;

public class CustomLocalFile extends LocalFile{
	
	public CustomLocalFile(String projName, File file) {
		super(new File(RemoteHelper.retrieveProjRootFromMetaArea(projName)));
	}
}

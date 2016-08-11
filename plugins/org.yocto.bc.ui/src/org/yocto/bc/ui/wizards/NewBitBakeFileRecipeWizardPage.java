/*****************************************************************************
 * Copyright (c) 2009 Ken Gilmer
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ken Gilmer - initial API and implementation
 *     Jessica Zhang (Intel) - Extend to support auto-fill base on src_uri value
 *******************************************************************************/
package org.yocto.bc.ui.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.math.BigInteger;

public class NewBitBakeFileRecipeWizardPage extends WizardPage {
	private Text containerText;
	private Text fileText;
	
	private Text descriptionText;
	private Text licenseText;
	private Text checksumText;
	private Text homepageText;
	private Text authorText;
	private Text sectionText;
	private Text srcuriText;
	private Text md5sumText;
	private Text sha256sumText;
	private BitbakeRecipeUIElement element;
	
	private ISelection selection;
	private String metaDirLoc;
	private ArrayList<String> inheritance;

	public NewBitBakeFileRecipeWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("BitBake Recipe");
		setDescription("Create a new BitBake recipe.");
		this.selection = selection;
		element = new BitbakeRecipeUIElement();
		inheritance = new ArrayList<String>();
	}

	public void createControl(Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText("Recipe &Directory:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button buttonBrowse = new Button(container, SWT.PUSH);
		buttonBrowse.setText("Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse(container, containerText);
			}
		});
		
		label = new Label(container, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		label = new Label(container, SWT.NULL);
		label.setText("SRC_&URI:");

		srcuriText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		srcuriText.setLayoutData(gd);
		srcuriText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button buttonP = new Button(container, SWT.PUSH);
		buttonP.setText("Populate...");
		buttonP.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handlePopulate();
			}
		});

		createField(container, "&Recipe Name:", (fileText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "SRC_URI[&md5sum]:", (md5sumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "SRC_URI[&sha256sum]:", (sha256sumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "License File &Checksum:", (checksumText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&Package Description:", (descriptionText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&License:", (licenseText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		
		createField(container, "&Homepage:", (homepageText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "Package &Author:", (authorText = new Text(container, SWT.BORDER | SWT.SINGLE)));
		createField(container, "&Section:", (sectionText = new Text(container, SWT.BORDER | SWT.SINGLE)));

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void createField(Composite container, String title, Text control) {
		Label label = new Label(container, SWT.NONE);
		label.setText(title);
		label.moveAbove(control);
	
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		control.setLayoutData(gd);
		control.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}

		});
	}

	private void dialogChanged() {
		String containerName = containerText.getText();
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(containerName));
		String fileName = fileText.getText();

		if (containerName.length() == 0) {
			updateStatus("Directory must be specified");
			return;
		}
	
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		
		IProject project = container.getProject();
		metaDirLoc = project.getLocation().toString() + "/meta";
	
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.contains(" ")) {
			updateStatus("File name must be valid with no space in it");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}

		if (descriptionText.getText().length() == 0) {
			updateStatus("Recipe must have a description");
			return;
		}
		
		if (licenseText.getText().length() == 0) {
			updateStatus("Recipe must have a license");
			return;
		}

		if (srcuriText.getText().length() == 0) {
			updateStatus("SRC_URI can't be empty");
			return ;
		} else if (!element.is_src_uri_valid(srcuriText.getText())) {
			updateStatus("Invalid SRC_URI");
			return ;
		}

		updateStatus(null);
	}

	public BitbakeRecipeUIElement getUIElement() {
		element.setAuthor(authorText.getText());
		element.setChecksum(checksumText.getText());
		element.setContainer(containerText.getText());
		element.setDescription(descriptionText.getText());
		element.setFile(fileText.getText());
		element.setHomePage(homepageText.getText());
		element.setLicense(licenseText.getText());
		element.setMd5sum(md5sumText.getText());
		element.setSection(sectionText.getText());
		element.setSha256sum(sha256sumText.getText());
		element.setSrcuri(srcuriText.getText());
		element.setInheritance(inheritance);
		element.setMetaDir(metaDirLoc);
		
		return element;
	}
	
	private void handleBrowse(final Composite parent, final Text text) {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select project directory");
		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				text.setText(((Path) result[0]).toString());
			}
		}
	}

	private void handlePopulate() {
		String src_uri = srcuriText.getText();
		if ((src_uri.startsWith("http://") || src_uri.startsWith("ftp://"))
			&& (src_uri.endsWith("tar.gz") || src_uri.endsWith("tar.bz2"))) {

			HashMap<String, String> mirror_map = createMirrorLookupTable();

			populateRecipeName(src_uri);
			populateSrcuriChecksum(src_uri);
			String extractDir = extractPackage(src_uri);
			populateLicensefileChecksum(extractDir);
			updateSrcuri(mirror_map, src_uri);
			populateInheritance(extractDir);
		} else if (src_uri.startsWith("file://")) {
			String path_str = src_uri.substring(7);
			File package_dir = new File(path_str);
			if (package_dir.isDirectory()) {
				String package_name = path_str.substring(path_str.lastIndexOf("/")+1);
				fileText.setText(package_name+".bb");
				populateLicensefileChecksum(path_str);
				populateInheritance(path_str);
			}
		}
	}

	private String extractPackage(String src_uri) {
		try {
			File working_dir = new File(metaDirLoc+"/temp");
			int idx = src_uri.lastIndexOf("/");
			String tar_file = src_uri.substring(idx+1);
			int tar_file_surfix_idx = tar_file.lastIndexOf(".tar");
			String tar_file_surfix = tar_file.substring(tar_file_surfix_idx);
			String tar_file_path = metaDirLoc+"/temp/"+tar_file;
			String tar_cmd = "";
			int tar_idx = 0;
			if (tar_file_surfix.matches(".tar.gz")) {
				tar_cmd = "tar -zxvf "+ tar_file_path;
				tar_idx = tar_file_path.lastIndexOf(".tar.gz");
			} else if (tar_file_surfix.matches(".tar.bz2")) {
				tar_idx = tar_file_path.lastIndexOf(".tar.bz2");
				tar_cmd = "tar -xvf " + tar_file_path;
			}
			final Process process = Runtime.getRuntime().exec(tar_cmd, null, working_dir);
			int returnCode = process.waitFor();
			if (returnCode == 0) {
				return tar_file_path.substring(0, tar_idx);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void populateInheritance(String extractDir) {
		File extract_dir = new File(extractDir);
		
		File[] files = extract_dir.listFiles();
		for (File file : files) {
			if (file.isDirectory())
				continue;
			else {
				if (file.getName().equalsIgnoreCase("cmakelists.txt"))
					inheritance.add("cmake");
				else if (file.getName().equalsIgnoreCase("setup.py"))
					inheritance.add("disutils");
				else {
					if (file.getName().equalsIgnoreCase("configure.ac") || file.getName().equalsIgnoreCase("configure.in"))
						inheritance.add("autotools");
					else
						continue;
				}
			}
		}
	}
	
	private void populateLicensefileChecksum(String extractDir) {
		String licenseFileChecksum_str = null;
		String licenseFilePath = null;

		try {
			File extract_dir = new File(extractDir);
			FilenameFilter copyFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.startsWith("COPYING")) {
						return true;
					} else {
						return false;
					}
				}
			};

			File copyFile = null;
			File[] files = extract_dir.listFiles(copyFilter);
			for (File file : files) {
				if (file.isDirectory())
					continue;
				else {
					copyFile = file;
					licenseFilePath = file.getCanonicalPath();
					break;
				}
			}

			MessageDigest digest_md5 = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(copyFile);
			byte[] buffer = new byte[8192];
			int read = 0;

			while( (read = is.read(buffer)) > 0) {
				digest_md5.update(buffer, 0, read);
			}

			byte[] md5sum = digest_md5.digest();
			BigInteger bigInt_md5 = new BigInteger(1, md5sum);
			licenseFileChecksum_str = bigInt_md5.toString(16);
			is.close();
		} catch (Exception e) {
			throw new RuntimeException("Unable to process file for MD5 calculation", e);
		}

		if (licenseFileChecksum_str != null) {
			int idx = licenseFilePath.lastIndexOf("/");
			String license_file_name = licenseFilePath.substring(idx+1);
			checksumText.setText("file://"+license_file_name+";md5="+licenseFileChecksum_str);
		}
	}

	private void populateSrcuriChecksum(String src_uri) {
		String md5sum_str = null;
		String sha256sum_str = null;

		try {
			File working_dir = new File(metaDirLoc+"/temp");
			working_dir.mkdir();
			String download_cmd = "wget " + src_uri;
			final Process process = Runtime.getRuntime().exec(download_cmd, null, working_dir);
			int returnCode = process.waitFor();
			if (returnCode == 0) {
				int idx = src_uri.lastIndexOf("/");
				String tar_file = src_uri.substring(idx+1);
				String tar_file_path = metaDirLoc+"/temp/"+tar_file;
				MessageDigest digest_md5 = MessageDigest.getInstance("MD5");
				MessageDigest digest_sha256 = MessageDigest.getInstance("SHA-256");
				File f = new File(tar_file_path);
				InputStream is = new FileInputStream(f);
				byte[] buffer = new byte[8192];
				int read = 0;
				try {
					while( (read = is.read(buffer)) > 0) {
						digest_md5.update(buffer, 0, read);
						digest_sha256.update(buffer, 0, read);
					}
					byte[] md5sum = digest_md5.digest();
					byte[] sha256sum = digest_sha256.digest();
					BigInteger bigInt_md5 = new BigInteger(1, md5sum);
					BigInteger bigInt_sha256 = new BigInteger(1, sha256sum);
					md5sum_str = bigInt_md5.toString(16);
					sha256sum_str = bigInt_sha256.toString(16);
				}
				catch(IOException e) {
					throw new RuntimeException("Unable to process file for MD5", e);
				}
				finally {
					try {
						is.close();
					}
					catch(IOException e) {
						throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
					}
				}
				if (md5sum_str != null)
					md5sumText.setText(md5sum_str);
				if (sha256sum_str != null)
					sha256sumText.setText(sha256sum_str);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HashMap<String, String> createMirrorLookupTable() {
		HashMap<String, String> mirror_map = new HashMap<String, String>();
		File mirror_file = new File(metaDirLoc+"/classes/mirrors.bbclass");

		try {
			if (mirror_file.exists()) {
				BufferedReader input = new BufferedReader(new FileReader(mirror_file));

				try {
					String line = null;
					String delims = "[\\t]+";

					while ((line = input.readLine()) != null)
					{
						String[] tokens = line.split(delims);
						if (tokens.length < 2)
							continue;
						String ending_str = " \\n \\";
						int idx = tokens[1].lastIndexOf(ending_str);
						String key = tokens[1].substring(0, idx);
						mirror_map.put(key, tokens[0]);
					}
				}
				finally {
					input.close();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();

		}
		return mirror_map;
	}
	
	private void populateRecipeName(String src_uri) {
		String file_name = fileText.getText();
		if (!file_name.isEmpty())
			return;
		String delims = "[/]+";
		String recipe_file = null;

		String[] tokens = src_uri.split(delims);
		if (tokens.length > 0) {
			String tar_file = tokens[tokens.length - 1];
			int surfix_idx = 0;
			if (tar_file.endsWith(".tar.gz"))
				surfix_idx = tar_file.lastIndexOf(".tar.gz");
			else
				surfix_idx = tar_file.lastIndexOf(".tar.bz2");
			int sept_idx = tar_file.lastIndexOf("-");
			recipe_file = tar_file.substring(0, sept_idx)+"_"+tar_file.substring(sept_idx+1, surfix_idx)+".bb";
		}
		if (recipe_file != null)
			fileText.setText(recipe_file);
	}
	
	private void updateSrcuri(HashMap<String, String> mirrorsMap, String src_uri) {
		Set<String> mirrors = mirrorsMap.keySet();
		Iterator<String> iter = mirrors.iterator();
		String mirror_key = null;

	    while (iter.hasNext()) {
            String value = (String)iter.next();
            if (src_uri.startsWith(value)) {
                mirror_key = value;
                break;
            }
	    }

	    if (mirror_key != null) {
            String replace_string = (String)mirrorsMap.get(mirror_key);
            if (replace_string != null)
                src_uri = replace_string+src_uri.substring(mirror_key.length());
	    }
	    int idx = src_uri.lastIndexOf("-");
	    String new_src_uri = src_uri.substring(0, idx)+"-${PV}.tar.gz";
	    srcuriText.setText(new_src_uri);
	}
	
	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}
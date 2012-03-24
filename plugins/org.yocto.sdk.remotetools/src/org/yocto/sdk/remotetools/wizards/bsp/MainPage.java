package org.yocto.sdk.remotetools.wizards.bsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


import org.yocto.sdk.remotetools.YoctoBspElement;

/**
 * 
 * Setting up the parameters for creating the new Yocto BSP
 * 
 * @author jzhang
 */
public class MainPage extends WizardPage {
	public static final String PAGE_NAME = "Main";
	private static final String META_DATA_LOC = "MetadataLoc";
	private static final String BSP_OUT_LOC = "BspOutLoc";
	private static final String KARCH_CMD = "yocto-bsp list karch";
	private static final String QARCH_CMD = "yocto-bsp list qemu property qemuarch";
	private static final String BSP_SCRIPT = "yocto-bsp";
	private static final String PROPERTIES_CMD_PREFIX = "yocto-bsp list ";
	private static final String PROPERTIES_CMD_SURFIX = " properties -o ";
	private static final String PROPERTIES_FILE = "/tmp/properties.json";

	private Button btnMetadataLoc;
	private Button btnBspOutLoc;
	private Text textMetadataLoc;
	private Text textBspName;
	private Text textBspOutLoc;
	private Combo karchCombo;
	private Combo qarchCombo;
	private Label metadata_label; 
	private Label bspname_label;
	private Label bspout_label;
	private Label karch_label;
	private Label qarch_label;
	
	private YoctoBspElement bspElem;
	
	public MainPage(YoctoBspElement element) {
		super(PAGE_NAME, "yocto-bsp main page", null);

		//setTitle("Yocto-bsp main page");
		setMessage("Enter the required fields(with *) to create new Yocto Project BSP!");
		this.bspElem = element;
	}

	@Override
	public void createControl(Composite parent) {
		setErrorMessage(null);
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);	
		
		metadata_label = new Label(composite, SWT.NONE);
		metadata_label.setText("Meta_data location*: ");
		Composite textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textMetadataLoc = (Text)addTextControl(textContainer,META_DATA_LOC, "");
		textMetadataLoc.setEnabled(false);
		textMetadataLoc.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		btnMetadataLoc = addFileSelectButton(textContainer, textMetadataLoc);
		
		bspname_label = new Label(composite, SWT.NONE);
	
		bspname_label.setText("BSP Name*: ");
		textBspName = new Text(composite, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		textBspName.setLayoutData(gd);
		textBspName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		
		bspout_label = new Label(composite, SWT.NONE);
		bspout_label.setText("Bsp output location: ");
		textContainer = new Composite(composite, SWT.NONE);
		textContainer.setLayout(new GridLayout(2, false));
		textContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textBspOutLoc = (Text)addTextControl(textContainer, BSP_OUT_LOC, "");
		btnBspOutLoc = addFileSelectButton(textContainer, textBspOutLoc);
		karch_label= new Label(composite, SWT.NONE);
		karch_label.setText("kernel Architecture*: ");
		karchCombo= new Combo(composite, SWT.READ_ONLY);
		karchCombo.setLayout(new GridLayout(2, false));
		karchCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		karchCombo.setEnabled(false);
		karchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		
		qarch_label = new Label(composite, SWT.NONE);
		qarch_label.setText("Qemu Architecture(* for karch as qemu): ");
		qarch_label.setEnabled(false);
		qarchCombo = new Combo(composite, SWT.READ_ONLY);
		qarchCombo.setLayout(new GridLayout(2, false));
		qarchCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		qarchCombo.setEnabled(false);
		qarchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlChanged(e.widget);
			}
		});
		
		setControl(composite);
		validatePage();
	}

	private Control addTextControl(final Composite parent, String key, String value) {
		final Text text;

		text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(value);
		text.setSize(10, 150);

		return (Control)text;
	}
	
	private Button addFileSelectButton(final Composite parent, final Text text) {
		Button button = new Button(parent, SWT.PUSH | SWT.LEAD);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new DirectoryDialog(parent.getShell()).open();
				if (dirName != null) {
					text.setText(dirName);
				}
			}
		});
		return button;
	}	
	
	private void controlChanged(Widget widget) {
		 Status status = new Status(IStatus.OK, "not_used", 0, "", null);
		 setErrorMessage(null);
		 
		 if (widget == textMetadataLoc) {
			 resetKarchCombo();
			 String metadata_loc = textMetadataLoc.getText();
				
			 if (metadata_loc.length() == 0) {
				 status = new Status(IStatus.ERROR, "not_used", 0, "Meta data location can't be empty!", null);
			 } else {
				 File meta_data = new File(metadata_loc);
				 if (!meta_data.exists() || !meta_data.isDirectory()) {
					 status = new Status(IStatus.ERROR, "not_used", 0, 
							 "Invalid meta data location: Make sure it exists and is a directory!", null);
				 } else {
					 File bsp_script = new File(metadata_loc + "/scripts/" + BSP_SCRIPT);
					 if (!bsp_script.exists() || !bsp_script.canExecute())
						 status = new Status(IStatus.ERROR, "not_used", 0,
								 "Make sure yocto-bsp exists under \"" + metadata_loc + "/scripts\" and is executable!", null);
					 else {						
						 kernelArchesHandler();
						 canFlipToNextPage();
					 }
				 }
			 }
		 }
		 if (widget == karchCombo) {
			 String selection = karchCombo.getText();
			 if (!bspElem.getKarch().contentEquals(selection))
				 bspElem = new YoctoBspElement();
			 if (selection.matches("qemu")) {
				 qarch_label.setEnabled(true);
				 qarchCombo.setEnabled(true);
			 } else {
				 qarch_label.setEnabled(false);
				 qarchCombo.setEnabled(false);
			 }		
			 canFlipToNextPage();
		 }
		 if (widget == qarchCombo) {
			 canFlipToNextPage();
		 }
		 if (status.getSeverity() == IStatus.ERROR)
			 setErrorMessage(status.getMessage());
		 
		 getWizard().getContainer().updateButtons();
	}
	
	public YoctoBspElement bspElement() {
		return this.bspElem;
	}
	
	public void handleEvent(Event event) {
		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
	}
	
	private void resetKarchCombo() {
		karchCombo.deselectAll();
		qarchCombo.deselectAll();
		karchCombo.setEnabled(false);
		qarch_label.setEnabled(false);
		qarchCombo.setEnabled(false);
	}
	
	private void kernelArchesHandler() {
		ArrayList<String> karches = getKArches();
		if (!karches.isEmpty()) {
			String[] kitems = new String[karches.size()];
			kitems = karches.toArray(kitems);
			karchCombo.setItems(kitems);
			karchCombo.setEnabled(true);
		}
		ArrayList<String> qarches = getQArches();
		if (!qarches.isEmpty()) {
			String[] qitems = new String[qarches.size()];
			qitems = qarches.toArray(qitems);
			qarchCombo.setItems(qitems);
		}
	}
	
	public boolean canFlipToNextPage(){
		String err = getErrorMessage();
		if (err != null) 
			return false;
		else if (!validatePage())
			return false;
		return true;
	}
	
	
	private boolean validatePage() {
		String metadata_loc = textMetadataLoc.getText();
		String bspname = textBspName.getText();
		String karch = karchCombo.getText();
		String qarch = qarchCombo.getText();
		if (metadata_loc.isEmpty() ||
				bspname.isEmpty() ||
				karch.isEmpty()) {
			
			return false;
		} else {
			if (karch.matches("qemu"))
				if (qarch.isEmpty())
					return false;
		}
		
		bspElem.setBspName(bspname);
		if (!textBspOutLoc.getText().isEmpty())
			bspElem.setBspOutLoc(textBspOutLoc.getText());
		bspElem.setMetadataLoc(metadata_loc);
		bspElem.setKarch(karch);
		bspElem.setQarch(qarch);
		bspElem.setValidPropertiesFile(createPropertiesFile());
		
		return true;
	}
	
	private boolean createPropertiesFile() {
		String create_properties_cmd = bspElem.getMetadataLoc() + "/scripts/" + 
										PROPERTIES_CMD_PREFIX + bspElem.getKarch() + 
										PROPERTIES_CMD_SURFIX + PROPERTIES_FILE;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(create_properties_cmd);
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			
			while ( (line = br.readLine()) != null) {
			}
			
			int exitVal = proc.waitFor();
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
		return true;
	}
	
	private ArrayList<String> getKArches() {
		ArrayList<String> karches = new ArrayList<String>();
		
		String get_karch_cmd = textMetadataLoc.getText() + "/scripts/" + KARCH_CMD;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(get_karch_cmd);
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			
			while ( (line = br.readLine()) != null) {
				if (line.contains(":"))
					continue;
				line = line.replaceAll("^\\s+", "");
				line = line.replaceAll("\\s+$", "");
				karches.add(line);
			}
			
			int exitVal = proc.waitFor();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		return karches;
	}

	private ArrayList<String> getQArches() {
		ArrayList<String> qarches = new ArrayList<String>();
		
		String get_qarch_cmd = textMetadataLoc.getText() + "/scripts/" + QARCH_CMD;
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(get_qarch_cmd);
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			
			while ( (line = br.readLine()) != null) {
				if (!line.startsWith("["))
					continue;
				String[] values = line.split(",");
				
				String value = values[0];
				value = value.replace("[\"", "");
				value = value.replaceAll("\"$", "");
				qarches.add(value);
			}
			int exitVal = proc.waitFor();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		return qarches;
	}
}

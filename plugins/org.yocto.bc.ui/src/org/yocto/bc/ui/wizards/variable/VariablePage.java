package org.yocto.bc.ui.wizards.variable;

import java.util.Comparator;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import org.yocto.bc.ui.wizards.FiniteStateWizardPage;

/**
 * The wizard page for the Variable Wizard.
 * @author kgilmer
 *
 */
public class VariablePage extends FiniteStateWizardPage {

	private Text txtName;
	private Text txtValue;
	private TableViewer viewer;
	private TableColumn c1;
	private TableColumn c2;

	protected VariablePage(Map model) {
		super("Yocto Project BitBake Commander", model);
		setTitle("Yocto Project BitBake Variable Viewer");
		setDescription("Sort and fitler global BitBake variables by name or value.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.None);
		top.setLayout(new GridLayout(2, true));
		top.setLayoutData(new GridData(GridData.FILL_BOTH));

		ValidationListener listener = new ValidationListener();

		txtName = new Text(top, SWT.BORDER);
		txtName.addModifyListener(listener);
		txtName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		txtValue = new Text(top, SWT.BORDER);
		txtValue.addModifyListener(listener);
		txtValue.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		viewer = new TableViewer(top);

		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		data.horizontalSpan = 2;
		table.setLayoutData(data);
		c1 = new TableColumn(table, SWT.NONE);
		c1.setText("Name");
		c1.setWidth(200);
		c1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((VariableViewerSorter) viewer.getSorter()).doSort(0);
				viewer.refresh();
			}
		});

		c2 = new TableColumn(table, SWT.NONE);
		c2.setText("Value");
		c2.setWidth(200);
		c2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				((VariableViewerSorter) viewer.getSorter()).doSort(1);
				viewer.refresh();
			}
		});

		viewer.setContentProvider(new VariableContentProvider());
		viewer.setLabelProvider(new VariableLabelProvider());
		viewer.setSorter(new VariableViewerSorter());

		viewer.setFilters(new ViewerFilter[] {new MapViewerFilter()});
		setControl(top);
	}

	@Override
	public void pageCleanup() {

	}

	@Override
	public void pageDisplay() {
		viewer.setInput(model);
	}

	@Override
	protected void updateModel() {
		viewer.refresh();
	}

	@Override
	protected boolean validatePage() {
		return true;
	}

	/**
	 * A content provider for the variable wizard dialog.
	 * @author kgilmer
	 *
	 */
	private class VariableContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return model.keySet().toArray();
		}

		public void dispose() {

		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}
	}

	/**
	 * A label provider for variable wizard dialog.
	 * @author kgilmer
	 *
	 */
	private class VariableLabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String val;

			switch (columnIndex) {
			case 0:
				val = element.toString();
				break;
			case 1:
				val = (String) model.get(element);
				break;
			default:
				val = "";
				break;
			}

			return val;
		}

		public void addListener(ILabelProviderListener listener) {

		}

		public void dispose() {

		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {

		}

	}

	/**
	 * 
	 * A tableviewer sorter found on the internet.
	 *
	 */
	class VariableViewerSorter extends ViewerSorter {
		private static final int ASCENDING = 0;

		private static final int DESCENDING = 1;

		private int column;

		private int direction;

		public void doSort(int column) {
			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an ascending sort
				this.column = column;
				direction = ASCENDING;
			}
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			int rc = 0;
			Comparator c = this.getComparator();
			// Determine which column and do the appropriate sort
			switch (column) {
			case 0:
				rc = c.compare(e1, e2);
				break;
			case 1:
				rc = c.compare(model.get(e1), model.get(e2));
				break;
			}

			// If descending order, flip the direction
			if (direction == DESCENDING)
				rc = -rc;

			return rc;
		}
	}
	
	/**
	 * A filter for the name/value model.
	 * @author kgilmer
	 *
	 */
	private class MapViewerFilter extends ViewerFilter {

		public MapViewerFilter() {
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String keyFilter = txtName.getText();
			String valFilter = txtValue.getText();
			
			String elem = (String) element;
			String val = (String) model.get(element);
			
			if (keyFilter.length() > 0 && elem.indexOf(keyFilter) == -1 ) {
				return false;
			}
			
			if (valFilter.length() > 0 && val.indexOf(valFilter) == -1 ) {
				return false;
			}
			
			return true;
		}
		
	}

}

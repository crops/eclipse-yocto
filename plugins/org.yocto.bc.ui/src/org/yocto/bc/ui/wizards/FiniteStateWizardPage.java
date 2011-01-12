package org.yocto.bc.ui.wizards;
import java.util.Map;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class FiniteStateWizardPage extends WizardPage {
    protected Map model = null;
    protected FiniteStateWizard wizard = null;
    private static boolean previousState = false;
    /**
     * @param pageName
     */
    protected FiniteStateWizardPage(String name, Map model) {
        super(name);
        this.model = model;
        this.setPageComplete(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public abstract void createControl(Composite parent);

    protected void setModelWizard() {
        if (wizard == null) {
            wizard = (FiniteStateWizard)FiniteStateWizardPage.this.getWizard();
        }
    }
    
    /**
     * Add page validation logic here. Returning <code>true</code> means that
     * the page is complete and the user can go to the next page.
     * 
     * @return
     */
    protected abstract boolean validatePage();

    /**
     * This method should be implemented by ModelWizardPage classes. This method
     * is called after the <code>validatePage()</code> returns successfully.
     * Update the model with the contents of the controls on the page.
     */
    protected abstract void updateModel();

    /**
     * Helper method to see if a field has some sort of text in it.
     * @param value
     * @return
     */
    protected boolean hasContents(String value) {
        if (value == null || value.length() == 0) {
            return false;
        } 
        
        return true;
    }
    
    /**
     * This method is called right before a page is displayed.
     * This occurs on user action (Next/Back buttons).
     */
    public abstract void pageDisplay();
    
	/**
	 * This method is called on the concrete WizardPage after the user has
	 * gone to the page after.
	 */
	public abstract void pageCleanup();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean arg0) {
	    
		if (!arg0 && previousState) {
			pageCleanup();
		} else if (arg0 && !previousState) {
			pageDisplay();
		} else if (arg0 && previousState) {
			pageDisplay();
		}
		
		previousState = arg0;
		
		super.setVisible(arg0);
	}
	
    public class ValidationListener implements SelectionListener, ModifyListener, Listener, ISelectionChangedListener {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            validate();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
         */
        public void modifyText(ModifyEvent e) {
            validate();
        }

        public void validate() {                       
            if (validatePage()) {
                updateModel();
                setPageComplete(true);
                return;
            }

            setPageComplete(false);
        }

        /* (non-Javadoc)
         * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
         */
        public void handleEvent(Event event) {
            
            validate();
        }

        public void selectionChanged(SelectionChangedEvent event) {
            validate();
        }
    }
}

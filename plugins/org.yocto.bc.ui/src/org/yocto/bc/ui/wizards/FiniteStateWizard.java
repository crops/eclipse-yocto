package org.yocto.bc.ui.wizards;
import java.util.Map;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;



public abstract class FiniteStateWizard extends Wizard {
    private boolean finishable = false;
    private boolean canFinish = false;

    public FiniteStateWizard() {                
    }
    
    public abstract boolean performFinish();

    /**
     * @return Returns if the wizard is finishable in its current state.
     */
    public boolean isFinishable() {
        return finishable;
    }
    /**
     * @param finishable Change the finish state of the wizard.
     */
    public void setFinishable(boolean finishable) {
        this.finishable = finishable;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.IWizard#createPageControls(org.eclipse.swt.widgets.Composite)
     */
    public void createPageControls(Composite pageContainer) {
        super.createPageControls(pageContainer);        
    }

    /*
     * (non-Javadoc) Method declared on IWizard.
     */
    public boolean canFinish() {
      if (canFinish)
        return true;
      return super.canFinish();
    }
    
    public void setCanFinish(boolean canFinish) {
      this.canFinish = canFinish;
    }
    
    /**
     * Retrive the model object from the wizard.
     * @return
     */
    public abstract Map getModel();
}

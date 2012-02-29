package org.yocto.bc.ui;

import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.*;
import org.yocto.bc.ui.builder.BitbakeCommanderNature;

public class BCResourceChangeListener implements IResourceChangeListener {

	public void resourceChanged(IResourceChangeEvent event) {
		final HashSet<IResource> removed = new HashSet<IResource>();
		final HashSet<IResource> changed = new HashSet<IResource>();
        switch (event.getType()) {
           case IResourceChangeEvent.POST_CHANGE:
              try {
            	  event.getDelta().accept(new IResourceDeltaVisitor() {
            		  public boolean visit(IResourceDelta delta) throws CoreException {
            				IResource res = delta.getResource();
            				Boolean visit= true;
            				if (res instanceof IProject) {
            					visit = false;
            					try {
            						if(((IProject) res).isOpen() && 
            							((IProject) res).hasNature(BitbakeCommanderNature.NATURE_ID)){
            							visit = true;
            						}
            					}catch (CoreException e) {
            					}
            				}
            				if (visit && (res instanceof IFile))
            				{
            			        switch (delta.getKind()) {
            			           case IResourceDelta.REMOVED:
            			        	  removed.add(res);
            			              break;
            			           case IResourceDelta.CHANGED:
            			        	  changed.add(res);
            			              break;
            			        }
            				}
            		        return visit; // visit the children
            		  }
            	  });
            	  //notify all the sessions
            	  Activator.notifyAllBBSession(null, 
            			  removed.toArray(new IResource[removed.size()]), 
            			  changed.toArray(new IResource[changed.size()]));
            	  
              }catch (CoreException e) {
            	  e.printStackTrace();
              }
              break;
          default:
        	  break;
        }
	}
}

/**
 * Maps BB Variables in the editor to BBSession
 * @author kgilmer
 *
 */

package org.yocto.bc.ui.editors.bitbake;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import org.yocto.bc.bitbake.BBRecipe;
import org.yocto.bc.bitbake.BBSession;
import org.yocto.bc.ui.Activator;

class BBVariableTextHover implements ITextHover {
	private final BBSession session;
	private volatile Map<String, Object> envMap;

	public BBVariableTextHover(BBSession session, String file) {
		this.session = session;
		envMap = session;
		LoadRecipeJob loadRecipeJob = new LoadRecipeJob(getFilename(file), file);
		loadRecipeJob.schedule();
	}

	private String getFilename(String file) {
		String [] elems = file.split(File.separator);

		return elems[elems.length - 1];
	}

	public IRegion getHoverRegion(ITextViewer tv, int off) {
		return new Region(off, 0);
	}

	public String getHoverInfo(ITextViewer tv, IRegion r) {
		try {
			IRegion lineRegion = tv.getDocument().getLineInformationOfOffset(r.getOffset());

			return getBBVariable(tv.getDocument().get(lineRegion.getOffset(), lineRegion.getLength()).toCharArray(), r.getOffset() - lineRegion.getOffset());
		} catch (Exception e) {
			return "";
		}
	}

	private String getBBVariable(char[] line, int offset) {
		// Find start of word.
		int i = offset;
		
		while (line[i] != ' ' && line[i] != '$' && i > 0) {
			i--;
		}
		
		if (i < 0 || line[i] != '$') {
			return "";  //this is not a BB variable.
		}
		
		// find end of word
		int start = i;
		i = offset;
		
		while (line[i] != ' ' && line[i] != '}' && i <= line.length) {
			i++;
		}
		
		if (line[i] != '}') {
			return "";  //this bb variable didn't terminate as expected
		}
		
		String key = new String(line, start + 2, i - start - 2);
		String val = (String) envMap.get(key);
		
		if (val == null) {
			val = "";
		}
		
		if (val.length() > 64) {
			val = val.substring(0, 64) + '\n' + val.substring(65);
		}
		
		return val;
	}
	
	private class LoadRecipeJob extends Job {
		private final String filePath;

		public LoadRecipeJob(String name, String filePath) {
			super("Extracting BitBake environment for " + name);
			this.filePath = filePath;
		}

		@Override
		protected IStatus run(IProgressMonitor mon) {
			try {
				BBRecipe recipe = Activator.getBBRecipe(session, filePath);
				recipe.initialize();
				envMap = recipe;
			} catch (Exception e) {
				return new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Unable to load session for " + filePath, e);
			} 
			
			return Status.OK_STATUS;
		}
	}
}
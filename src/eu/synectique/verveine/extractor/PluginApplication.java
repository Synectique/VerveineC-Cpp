package eu.synectique.verveine.extractor;

import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import eu.synectique.verveine.extractor.c.VerveineCParser;

public class PluginApplication implements IApplication {
	protected VerveineCParser parser;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String,Object> ctxtArg = context.getArguments();
		String[] appArg = (String[]) ctxtArg.get(IApplicationContext.APPLICATION_ARGS);
//		try {
			/*ICProject project= CoreModel.getDefault().getCModel().getCProject("CPAchecker");
			CCorePlugin.getIndexManager().getIndex(project);
			IIndex index= CCorePlugin.getIndexManager().getIndex(project);*/


			VerveineCParser parser = new VerveineCParser();
			parser.setOptions(appArg);
			parser.parse();
			parser.emitMSE();
/*		} catch (CoreException e) {
			e.printStackTrace();
		}*/
		return null;
	}

	@Override
	public void stop() {
	}

}

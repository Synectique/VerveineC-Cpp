package eu.synectique.verveine.extractor.plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class PluginApplication implements IApplication {

	@Override
	public Object start(IApplicationContext ctxt) throws Exception {
		Activator.log(IStatus.INFO, "Starting VerveineC");

		String[] appArgs = (String[])ctxt.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		VerveineCParser verveine = new VerveineCParser();

		verveine.setOptions(appArgs);
		if (verveine.parse()) {
			verveine.emitMSE();
		}
		else {
			Activator.log(IStatus.ERROR, "Error in model creation, aborting");
		}
		return null;
	}

	@Override
	public void stop() {
		// nothing
	}

}

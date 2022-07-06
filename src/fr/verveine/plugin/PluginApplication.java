package fr.verveine.plugin;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class PluginApplication implements IApplication {

	@Override
	public Object start(IApplicationContext ctxt) throws Exception {
		System.out.println("Starting VerveineC");
		
		String[] appArgs = (String[])ctxt.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		VerveineCParser verveine = new VerveineCParser();

		verveine.setOptions(appArgs);
		if (verveine.parse()) {
			verveine.emitMSE();
			System.out.println("done exporting model (" + verveine.nbEntities() + " entitites)");
		}
		else {
			System.err.println("Error in model creation, aborting");
		}
		return null;
	}

	@Override
	public void stop() {
		// nothing
	}

}

package eu.synectique.verveine.extractor.plugin;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import eu.synectique.verveine.core.gen.famix.Entity;
import eu.synectique.verveine.core.gen.famix.NamedEntity;

public class PluginApplication implements IApplication {

	@Override
	public Object start(IApplicationContext ctxt) throws Exception {
		System.out.println("Starting VerveineC");
		
		String[] appArgs = (String[])ctxt.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		VerveineCParser verveine = new VerveineCParser();

		verveine.setOptions(appArgs);
		verveine.parse();
		verveine.emitMSE();

		return null;
	}

	@Override
	public void stop() {
		// nothing
	}

}

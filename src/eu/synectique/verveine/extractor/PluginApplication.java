package eu.synectique.verveine.extractor;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import eu.synectique.verveine.extractor.cpp.VerveineCParser;

public class PluginApplication implements IApplication {

	@Override
	public Object start(IApplicationContext ctxt) throws Exception {
		System.out.println("Verveine running in: " + System.getenv("PWD"));
		
		String[] appArg = (String[])ctxt.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		new VerveineCParser().parse(appArg[0]);
		return null;
	}

	@Override
	public void stop() {
		// nothing
	}

}

package eu.synectique.verveine.extractor;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import eu.synectique.verveine.extractor.cpp.VerveineCParser;

public class PluginApplication implements IApplication {

	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		System.out.println("Verveine running in: " + System.getenv("PWD"));
		new VerveineCParser().parse();
		return null;
	}

	@Override
	public void stop() {
		// nothing
	}

}

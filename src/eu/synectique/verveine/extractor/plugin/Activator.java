package eu.synectique.verveine.extractor.plugin;


import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	private static BundleContext context;
	
	/**
	 * The eclipse logger
	 */
	static private ILog logger = null; 

	/**
	 * plugin Id = Symbolic name
	 */
	static private String pluginId = null;

	/**
	 * There should be only one activator of this plugin at a time.
	 * Stored here to allow calling stop()
	 */
	static private Activator lastActivator = null;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		pluginId = getBundle().getSymbolicName();
		logger = getLog();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		Activator.log(IStatus.INFO, "VerveineC done");
	}

	static public void stop() {
		try {
			lastActivator.stop(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public ILog getLogger() {
		return logger;
	}

	static public String getPluginId() {
		return pluginId;
	}
	static public void log(int severity, String message) {
		logger.log( logMsg(severity, message));
	}

	static public IStatus logMsg(int severity, String message) {
		return new Status(severity, pluginId, message);
	}

}

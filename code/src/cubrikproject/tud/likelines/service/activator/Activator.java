package cubrikproject.tud.likelines.service.activator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator for LikeLines indexing OSGI bundle
 * 
 * @author R. Vliegendhart
 */
public class Activator implements BundleActivator {
	public static final String BUNDLE_NAME = "cubrikproject.tud.likelines.service";

	private static BundleContext context;

	private final static Log _log = LogFactory.getLog(Activator.class);

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		_log.info("Indexing service started!");
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}


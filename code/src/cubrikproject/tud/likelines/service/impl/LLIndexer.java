package cubrikproject.tud.likelines.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import cubrikproject.tud.likelines.service.activator.Activator;
import cubrikproject.tud.likelines.webservice.LikeLinesWebService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.smila.utils.config.ConfigUtils;

/**
 * The service responsible for launching indexing processes in the background.
 * 
 * @author R. Vliegendhart
 */
public class LLIndexer implements cubrikproject.tud.likelines.service.interfaces.LLIndexer {
	
	private final static Log _log = LogFactory.getLog(LLIndexer.class);
	private final String propertiesFile = getClass().getSimpleName() + ".properties";
	private final Properties props;
	
	private final String ffmpegPath;
	private final String motionActivityPath;
	
	private final String DEFAULT_FFMPEG = "ffmpeg";
	private final String DEFAULT_MOTIONACTIVITY = "motionActivity";
	private final Map<String, String> secretKeys;
	
	private Transcoder transcoder;
	
	public LLIndexer() {
		System.out.println(">>> LLIndexer: Reading (configuration/)" + Activator.BUNDLE_NAME + "/" + propertiesFile);
		
		props = loadProperties();
		
		String ffmpegPathFromProps = nullIfMissing( getProperty("ffmpegPath") );
		String motionActivityPathFromProps = nullIfMissing( getProperty("motionActivityPath") );
		
		ffmpegPath = (ffmpegPathFromProps == null) ?
				findOnPath(DEFAULT_FFMPEG) : ffmpegPathFromProps;
		
		motionActivityPath = (motionActivityPathFromProps == null) ?
				findOnPath(DEFAULT_MOTIONACTIVITY) : motionActivityPathFromProps;	
		
		secretKeys = getSecretKeys();
		testSecretKeys(secretKeys);
		
		transcoder = (ffmpegPath == null) ? null : new Transcoder(ffmpegPath);
		
		if (_log.isInfoEnabled()) {
			_log.info("LLIndexer created using following setting");
			_log.info(" -ffmpegPath=" + ffmpegPath);
			_log.info(" -motionActivityPath=" + motionActivityPath);
		}

	}

	/**
	 * Loads the property file
	 * 
	 * @return The Properties or null if it does not exist
	 */
	private Properties loadProperties() {
		Properties res = null;
		try {
			res = ConfigUtils.getConfigProperties(Activator.BUNDLE_NAME, propertiesFile);
		} catch (Exception e) {
			_log.warn(e);
		}
		return res;
	}
	
	/**
	 * Gets a property or null if the properties file or the property don't exist
	 * 
	 * @return The Properties or null if it does not exist
	 */
	private String getProperty(String propertyName) {
		return props == null ? null : props.getProperty(propertyName);
	}
	
	/**
	 * Find an executable on the PATH
	 * 
	 * @param executable Name of the executable to search for
	 * @return A path if found, otherwise null
	 */
	private String findOnPath(String executable) {
		final boolean w32 = System.getProperty("os.name").startsWith("Windows");
		final String exeSuffix = w32 ? ".exe" : "";
		executable += exeSuffix;
		
		for (String path: System.getenv("PATH").split(File.pathSeparator)) {
			File f = new File(path, executable);
			if (f.exists())
				return f.getAbsolutePath();
		}
		return null;
	}
	
	/**
	 * Maps a path to null if it does not exist.
	 * 
	 * @param path The path to check as a String
	 * @return The path if it exists, otherwise null
	 */
	private String nullIfMissing(String path) {
		return (path != null && new File(path).exists()) ? path : null;
	}
	
	/**
	 * Loads the secret keys from the properties file and stores it in a map
	 * 
	 * @return A mapping from LikeLines server URLs to their secret keys. 
	 */
	private Map<String,String> getSecretKeys() {
		final HashMap<String, String> res = new HashMap<String,String>();
		final String secretKeysProperty = getProperty("secretKeys");
		
		final String[] secretKeysAndServers = (secretKeysProperty==null) ? new String[0] : secretKeysProperty.split("\\s+");
		
		if (secretKeysAndServers.length % 2 != 0) {
			_log.warn("LLIndexer: secretKeys property should contain an even number of items after splitting!");
			return res;
		}
		
		for (int i = 0; i < secretKeysAndServers.length; i += 2) {
			final String secretKey = secretKeysAndServers[i];
			final String server = LikeLinesWebService.ensureTrailingSlash(secretKeysAndServers[i + 1]);
			res.put(server, secretKey);
		}

		return res;
	}
	
	/**
	 * Tests and removes secret keys that are invalid.
	 * 
	 * @param secretKeys
	 */
	private static void testSecretKeys(Map<String,String> secretKeys) {
		final List<String> failedServers = new ArrayList<String>();
		
		for (final Entry<String, String> kv : secretKeys.entrySet()) {
			final String server = kv.getKey();
			final String key = kv.getValue();
			
			LikeLinesWebService llserver = null;
			try {
				llserver = new LikeLinesWebService(server);
			} catch (MalformedURLException e) {
				_log.warn("LLIndexer: testSecretKeys: malformed URL in server: " + server);
				failedServers.add(server);
				e.printStackTrace();
				continue;
			}
			
			try {
				if (!llserver.testKey(key)) {
					_log.warn("LLIndexer: testSecretKeys: incorrect key for server: " + server);
					failedServers.add(server);
					continue;
				}
					
			} catch (IOException e) {
				_log.warn("LLIndexer: testSecretKeys: I/O error, server down? " + server);
				failedServers.add(server);
				e.printStackTrace();
				continue;
			}
		}
		
		for (final String server : failedServers)
			secretKeys.remove(server);
	}
}

package cubrikproject.tud.likelines.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Simple utilities class for AJAX related work
 * 
 * @author Raynor Vliegendhart
 */
public class Ajax {

	/**
	 * Retrieves a JSON value from the given URL.
	 * 
	 * @param url
	 *            URL to JSON resource
	 * @return JSON element
	 * @throws MalformedURLException
	 *             When the given URL is malformed
	 * @throws IOException
	 *             When the resource cannot be retrieved
	 * @see #getJSON(URL)
	 */
	public static JsonElement getJSON(String url) throws MalformedURLException,
			IOException {
		return getJSON(new URL(url));
	}

	/**
	 * Retrieves a JSON value from the given URL.
	 * 
	 * @param url
	 *            URL to JSON resource
	 * @return JSON element
	 * @throws IOException
	 *             When the resource cannot be retrieved
	 */
	public static JsonElement getJSON(URL url) throws IOException {
		JsonParser jp = new JsonParser();
		
		URLConnection conn = url.openConnection();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		
		return jp.parse(reader);
	}
}

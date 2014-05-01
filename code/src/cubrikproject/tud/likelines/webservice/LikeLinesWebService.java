package cubrikproject.tud.likelines.webservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonObject;

import cubrikproject.tud.likelines.util.Ajax;
import cubrikproject.tud.likelines.util.Peaks;
import cubrikproject.tud.likelines.util.Peaks.Point;

/**
 * The proxy class to talk with a LikeLines server.
 * 
 * @author R. Vliegendhart
 */
public class LikeLinesWebService {

	/** The LikeLines server URL with a trailing slash */
	public final String serverUrl;
	
	/** The webservice call for aggregating interaction sessions */
	private static final String METHOD_AGGREGATE = "aggregate";
	
	/** The webservice call for testing the secret key */
	private static final String METHOD_TESTKEY = "testKey";
	
	/** The webservice call for posting MCA */
	private static final String METHOD_POSTMCA = "postMCA?s=";
	
	/** Default peak detection delta */
	public final double DEFAULT_PEAK_DELTA = 0.1;
	
	/**
	 * Constructs a proxy for a LikeLines server.
	 * 
	 * @param url
	 *            The address pointing to the LikeLines server
	 * @throws MalformedURLException When the server URL is not well-formed.
	 */
	public LikeLinesWebService(String url) throws MalformedURLException {
		serverUrl = ensureTrailingSlash(url);
	}

	/**
	 * Makes sure the input string ends with a trailing slash. If not, it is
	 * added to the string.
	 * 
	 * @param input
	 *            Input string
	 * @return String with a trailing slash added if it did not end with a
	 *         slash.
	 */
	static public String ensureTrailingSlash(String input) {
		return input.charAt(input.length() - 1) != '/' ? input + '/' : input;
	}

	/**
	 * Constructs the URL for a method and its parameters.
	 * 
	 * @param method
	 *            The method to be invoked on the LikeLines server
	 * @param paramsAndValues
	 *            A list of named parameters and corresponding values
	 * @return The URL for the given method set with the given parameters
	 */
	String constructUrl(String method, String... paramsAndValues) {
		assert paramsAndValues.length % 2 == 0 : "paramsAndValues should contain even number of values";

		final StringBuilder url = new StringBuilder(serverUrl);
		url.append(method);

		char delim = '?';
		try {
			for (int i = 0; i < paramsAndValues.length; i += 2) {
				final String param = paramsAndValues[i];
				final String value = paramsAndValues[i + 1];
				url.append(delim);
				url.append(URLEncoder.encode(param, "UTF-8"));
				url.append('=');
				url.append(URLEncoder.encode(value, "UTF-8"));
				delim = '&';
			}

		} catch (UnsupportedEncodingException e) {
			assert false : "UTF-8 should be supported";
		}
		return url.toString();
	}

	/**
	 * Aggregate user interaction sessions for a given video.
	 * 
	 * @param videoId Video ID for which interaction sessions need to be aggregated. Format is "YouTube:<i>videoId</i>" for YouTube videos.
	 * @return An aggregation of user interaction sessions for the given video.
	 * @throws IOException
	 */
	public Aggregate aggregate(String videoId) throws IOException {
		String url = constructUrl(METHOD_AGGREGATE, "videoId", videoId);
		
		System.out.println(url);
		try {
			JsonObject aggregation = Ajax.getJSON(url).getAsJsonObject();
			Aggregate agg = new Aggregate(aggregation);
			return agg;
		}
		catch (MalformedURLException e) {
			assert false : "Constructed URL should not be malformed since base server URL is valid";
		}
		return null; /* should not reach */
	}

	
	/**
	 * Computes the top N key frames for a queried video and returns the
	 * time-codes of these key frames.
	 * 
	 * @param N The (maximum) number of time-codes to be returned 
	 * @param videoId The video ID. For YouTube videos: YouTube:video_id.
	 * @return At most N time-codes.
	 * @throws IOException
	 */
	public double[] getNKeyFrames(int N, String videoId) throws IOException {
		Aggregate agg = aggregate(videoId);
		return getNKeyFrames(N, agg);
	}
	
	/**
	 * Computes the top N key frames for a queried video and returns the
	 * time-codes of these key frames.
	 * 
	 * @param N The (maximum) number of time-codes to be returned 
	 * @param aggregate A previously retrieved Aggregate object for a video.
	 * @return At most N time-codes.
	 * @throws IOException
	 */
	public double[] getNKeyFrames(int N, Aggregate aggregate) {
		double[] heatmap = aggregate.heatmap(aggregate.durationEstimate);
		Peaks peaks = Peaks.extract(heatmap, null, DEFAULT_PEAK_DELTA);
		
		double[] timecodes = new double[Math.min(N, peaks.peaks.size())];
		Collections.sort(peaks.peaks, new Comparator<Point>() {
			@Override public int compare(Point p1, Point p2) {
				return -Double.compare(p1.y, p2.y);
			}
		});
		for (int i = 0; i < timecodes.length; i++) {
			timecodes[i] = peaks.peaks.get(i).x;
		}
		return timecodes;
	}
	
	/**
	 * Method to test whether the same secret key is used on the LikeLines server
	 * by comparing signatures.
	 * 
	 * @param secretKey The secret key
	 * @param payload The payload message that will be signed
	 * @return True iff the same key is being used
	 * @throws IOException
	 */
	public boolean testKey(String secretKey, String payload) throws IOException {
		final String url = constructUrl(METHOD_TESTKEY);
		String sig;
		try {
			sig = computeSignature(secretKey, payload);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return false;
		}
		JsonObject res = Ajax.postJSON(url, new TestKeyRequest(payload, sig)).getAsJsonObject();
		
		return res.has("ok") && res.get("ok").getAsString().equals("ok");
	}
	
	/**
	 * Method to test whether the same secret key is used on the LikeLines server
	 * by comparing signatures.
	 * 
	 * This method will generate a random string and delegate the call to {@link #testKey(String, String)}.
	 * 
	 * @param secretKey The secret key
	 * @return True iff the same key is being used
	 * @throws IOException 
	 */
	public boolean testKey(String secretKey) throws IOException {
		final String payload = Long.toHexString(Double.doubleToLongBits(Math.random()));
		return testKey(secretKey, payload);
	}
	
	/**
	 * Computes the signature for a given message
	 * 
	 * @param secretKey The secret key to be used in computing the signature
	 * @param message The message to be signed
	 * @return The signature for the given message
	 * @throws InvalidKeyException
	 */
	public String computeSignature(String secretKey, String message) throws InvalidKeyException {
		return computeSignature(secretKey, message.getBytes());
	}
	
	/**
	 * Computes the signature for a given message
	 * 
	 * @param secretKey The secret key to be used in computing the signature
	 * @param message The message to be signed
	 * @return The signature for the given message
	 * @throws InvalidKeyException
	 */
	public String computeSignature(String secretKey, byte[] message) throws InvalidKeyException {
		SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
		Mac mac = null;
		try {
			mac = Mac.getInstance("HmacSHA1");
		} catch (NoSuchAlgorithmException e) {
			assert false : "HmacSHA1 should exist";
			e.printStackTrace();
		}
		mac.init(keySpec);
		byte[] result = mac.doFinal(message);
		return new String(Base64.encodeBase64(result));
	}
	
	/**
	 * The schema of the JSON payload for the testKey API call.
	 */
	static class TestKeyRequest {
		final public String msg;
		final public String sig;
		private TestKeyRequest(String msg, String sig) {
			this.msg = msg;
			this.sig = sig;
		}
	}
	
	/** MCA type: points */
	public final String MCA_TYPE_POINT = "point";
	
	/** MCA type: curve */
	public final String MCA_TYPE_CURVE = "curve";
	
	/**
	 * Posts MCA analysis results to the server for a given video.
	 * 
	 * @param videoId The ID of the video
	 * @param mcaName The MCA algorithm
	 * @param mcaType The type of the MCA output (continuous "curve" or individual "points")
	 * @param mcaData The MCA output
	 * @param secretKey The secret key of the server
	 * @param weight Weight of this MCA analysis in the heat-map aggregate
	 * @return True on success
	 * @throws IOException
	 */
	public boolean postMCA(String videoId, String mcaName, String mcaType, double[] mcaData, String secretKey, double weight) throws IOException {
		final String baseUrl = constructUrl(METHOD_POSTMCA);
		final PostMCARequest request = new PostMCARequest(videoId, mcaName, mcaType, mcaData, weight);
		final byte[] payload = Ajax.jsonSerialize(request);
		
		String sig;
		try {
			sig = computeSignature(secretKey, payload);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return false;
		}
		
		final String url = baseUrl + URLEncoder.encode(sig, "UTF-8");
		JsonObject res = Ajax.postSerializedJSON(url, payload).getAsJsonObject();
		
		return res.has("ok") && res.get("ok").getAsString().equals("ok");
	}
	
	/**
	 * Posts MCA analysis results to the server for a given video with the default weight 1.0.
	 * 
	 * @param videoId The ID of the video
	 * @param mcaName The MCA algorithm
	 * @param mcaType The type of the MCA output (continuous "curve" or individual "points")
	 * @param mcaData The MCA output
	 * @param secretKey The secret key of the server
	 * @return True on success
	 * @throws IOException
	 */
	public boolean postMCA(String videoId, String mcaName, String mcaType, double[] mcaData, String secretKey) throws IOException {
		return postMCA(videoId, mcaName, mcaType, mcaData, secretKey, 1.0);
	}
	
	/**
	 * The schema of the JSON payload for the postMCA API call.
	 */
	static class PostMCARequest {
		final public String videoId;
		final public String mcaName;
		final public String mcaType;
		final public double[] mcaData;
		final public double mcaWeight;
		
		private PostMCARequest(String videoId, String mcaName, String mcaType, double[] mcaData, double mcaWeight) {
			this.videoId = videoId;
			this.mcaName = mcaName;
			this.mcaType = mcaType;
			this.mcaData = mcaData;
			this.mcaWeight = mcaWeight;
		}
		
	}
}


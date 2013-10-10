package cubrikproject.tud.likelines.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * YouTube download utility class.
 * 
 * @author R. Vliegendhart
 */
public class YouTubeDL {
	
	/** Regular expression that matches YouTube IDs */
	public final static Pattern RE_YOUTUBE_ID = Pattern.compile("[a-zA-Z0-9_-]{11}");
	
	/** Usage documentation when used as a CLI program */ 
	private static void printUsage() {
		System.err.println("java " + YouTubeDL.class.getCanonicalName() + " YOUTUBE_ID [TARGET_PATH]");
	}
	
	/**
	 * Application entry-point. Downloads a given video.
	 * @param args Arguments passed through the command line.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0 || args.length > 2) {
			printUsage();
			System.exit(-1);
		}
		
		String youtubeId = args[0];
		if (!validateYouTubeID(youtubeId)) {
			System.err.println("ERR: Invalid YouTube ID");
			System.exit(-2);
		}
		
		final Map<String, String> videoInfo = getVideoInfo(youtubeId);
		final boolean ageGate = isAgeRestrictedVideo(youtubeId);
		
		YouTubeStream firstStream = null;
		for (YouTubeStream stream : getDownloadStreams(videoInfo, ageGate)) {
			firstStream = stream;
			break;
		}
		
		if (firstStream == null) {
			System.err.println("ERR: No stream found!");
			System.exit(-3);
		}
		
		final String targetPath = (args.length == 2) ? args[1] : youtubeId + firstStream.getExtension();
		firstStream.downloadTo(targetPath);
	}
		
	
	/**
	 * Validates a YouTube video ID.
	 * 
	 * @param videoId The YouTube video ID to be validated.
	 * @return True iff the input is a valid YouTube ID.
	 */
	public static boolean validateYouTubeID(String videoId) {
		return RE_YOUTUBE_ID.matcher(videoId).matches();
	}
	
	/**
	 * Get the VideoInfo for a YouTube video. 
	 * 
	 * @param videoId YouTube video id
	 * @return Video Info map
	 * @throws IOException In case of HTTP (e.g., 404) or network errors
	 */
	public static Map<String, String> getVideoInfo(String videoId) throws IOException {
		final String url = "http://www.youtube.com/get_video_info?&video_id=" + videoId + "&el=detailpage&ps=default&eurl=&gl=US&hl=en";

		try {
			String queryString = downloadAsString(url);
			Map<String, String> videoInfo = parseQueryString(queryString);
			
			return videoInfo;
		}
		catch (MalformedURLException e) {
			assert false : "URL should not be malformed";
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Extracts the stream URLs for a YouTube video 
	 * with the correct signature parameter appended to it.
	 * 
	 * @param videoInfo The VideoInfo for a YouTube video
	 * @param ageGate Whether this video is age-restricted (influences the signature computation)
	 * @return A list of YouTubeStreams
	 */
	public static List<YouTubeStream> getDownloadStreams(Map<String, String> videoInfo, boolean ageGate) {
		final String encodedMaps = videoInfo.get("url_encoded_fmt_stream_map");
		
		List<YouTubeStream> res = new ArrayList<YouTubeStream>(); 
		for (String encodedMap : encodedMaps.split(",")) {
			final Map<String, String> streamMap = parseQueryString(encodedMap);
			final String quality = streamMap.get("quality");
			final String mime = streamMap.get("type").split(";")[0];
			
			String signature;
			try {
				signature = signatureForStream(streamMap, ageGate);
			} catch (Exception e) {
				System.err.println("Skipping signature for this stream...");
				e.printStackTrace();
				continue;
			}
			
			try {
				String streamURL = streamMap.get("url")
						+ "&fallback_host=" + streamMap.get("fallback_host")
						+ "&signature=" + signature;
				
				YouTubeStream stream = new YouTubeStream(quality, mime, streamURL);
				res.add(stream);
				
			}
			catch (MalformedURLException e) {
				assert false : "Should not result in malformed URL!";
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	/**
	 * A YouTube stream URL and its corresponding quality and type information.
	 * 
	 * @author R. Vliegendhart
	 */
	public static class YouTubeStream {
		/** Quality of this stream */
		public String quality;
		/** MIME type of this stream */
		public String mime;
		/** URL to this stream */
		public URL url;
		
		/**
		 * Constructs a YouTubeStream object.
		 * 
		 * @param quality Quality of the stream
		 * @param mime MIME type of the stream
		 * @param url URL to the stream
		 * @throws MalformedURLException In case of malformed URL
		 */
		private YouTubeStream(String quality, String mime, String url) throws MalformedURLException {
			this.quality = quality;
			this.mime = mime;
			this.url = new URL(url);
		}
		
		/**
		 * Download the stream to a file (in a blocking fashion).
		 * 
		 * @param path String
		 * @throws IOException In case of HTTP (e.g., 404) or network errors
		 */
		public void downloadTo(String path) throws IOException {
			final FileOutputStream fos = new FileOutputStream(path);
			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				
				final long downloadSize = Long.parseLong(connection.getHeaderField("Content-Length"));
				final String lastModified = connection.getHeaderField("Last-Modified");
				
				System.err.println("Download size: " + downloadSize);
				
				ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
				
				final long BLOCK_SIZE = Long.MAX_VALUE;
				long downloaded = 0;
				long prevDownloaded = -1;
				boolean done = false;
				while (!done ) {
					fos.getChannel().transferFrom(rbc, downloaded, BLOCK_SIZE);
					downloaded = fos.getChannel().size();
					
					boolean shouldResume = (downloadSize == -1)
							? prevDownloaded != downloaded
							: downloaded < downloadSize;
					prevDownloaded = downloaded;
					
					if (shouldResume) {
						System.err.println("Downloaded " + downloaded + "... Resuming...");
						connection.disconnect();
						connection = (HttpURLConnection) url.openConnection();
						connection.setRequestProperty("Range", "bytes="+downloaded+"-");
						connection.setRequestProperty("If-Range", lastModified);
						connection.connect();
						try {
							rbc = Channels.newChannel(connection.getInputStream());
						}
						catch (IOException e) {
							if (connection.getResponseCode() == 416) {
								System.err.println("HTTP 416, assume we are done");
								done = true;
							}
							else throw e;
						}
					}
					else done = true;
				}
				
				System.err.println("File size: " + fos.getChannel().size());
			}
			finally {
				fos.close();
			}
		}
		
		/**
		 * Derive and return the extension from the MIME type.
		 * 
		 * @return The extension for this stream ("." included)
		 */
		public String getExtension() {
			final String subtype = mime.split("/")[1];
			
			if (subtype.equals("x-flv"))
				return ".flv";
			else
				return "." + subtype;
		}
		
		@Override
		public String toString() {
			return "YouTubeStream(\n\t\"" + quality + "\",\n\t\"" + mime
					+ "\",\n\t\"" + url.toString() + "\"\n)";
		}
	}
	
	
	/**
	 * Helper method to compute the signature for a YouTube stream.
	 * 
	 * @param streamMap Map describing YouTube stream properties
	 * @param ageGate Must be set to true if the video is age-restricted, false otherwise
	 * @return Signature for stream
	 * @throws Exception In case of decryption error
	 */
	private static String signatureForStream(Map<String, String> streamMap, boolean ageGate) throws Exception {
		return streamMap.containsKey("s") ? staticDecryptSignature(streamMap.get("s"), ageGate) : streamMap.get("sig");
	}
	
	/**
	 * Helper method to decrypt an encrypted YouTube stream signature.
	 * 
	 * @param encryptedSignature Encrypted stream signature
	 * @param ageGate Must be set to true if the video is age-restricted, false otherwise
	 * @return
	 * @throws Exception In case decryption fails
	 */
	private static String staticDecryptSignature(String encryptedSignature, boolean ageGate) throws Exception {
		// based on:
		// https://github.com/rg3/youtube-dl/blob/15870e90b0aa7fe73040936a2ef4e41cf5eed931/youtube_dl/extractor/youtube.py#L1061
		final String s = encryptedSignature;
		final int N = encryptedSignature.length();
		
		if (ageGate)
			if (N == 86)
				return s.substring(2, 63) + scharAt(s, 82) + s.substring(64,82) + scharAt(s, 63);
		
		String res = null;
		switch (N) {
		case 93:
			res = rsubstring(s, 86, 29) + scharAt(s, 88) + rsubstring(s, 28, 5);
			break;
		case 92:
			res = scharAt(s, 25) + s.substring(3, 25) + scharAt(s, 0)
					+ s.substring(26, 42) + scharAt(s, 79)
					+ s.substring(43, 79) + scharAt(s, 91)
					+ s.substring(80, 83);
			break;
		case 91:
			res = rsubstring(s, 84, 27) + scharAt(s, 86) + rsubstring(s, 26, 5);
			break;
		case 90:
			res = scharAt(s, 25) + s.substring(3, 25) + scharAt(s, 2)
					+ s.substring(26, 40) + scharAt(s, 77)
					+ s.substring(41, 77) + scharAt(s, 89)
					+ s.substring(78, 81);
			break;
		case 89:
			res = rsubstring(s, 84, 78) + scharAt(s, 87) + rsubstring(s, 77, 60) + scharAt(s, 0) + rsubstring(s, 59, 3);
			break;
		case 88:
			res = s.substring(7, 28) + scharAt(s, 87) + s.substring(29, 45)
					+ scharAt(s, 55) + s.substring(46, 55) + scharAt(s, 2)
					+ s.substring(56, 87) + scharAt(s, 28);
			break;
		case 87:
			res = s.substring(6, 27) + scharAt(s, 4) + s.substring(28, 39)
					+ scharAt(s, 27) + s.substring(40, 59) + scharAt(s, 2)
					+ s.substring(60);
			break;
		case 86:
			res = rsubstring(s, 80, 72) + scharAt(s, 16)
					+ rsubstring(s, 71, 39) + scharAt(s, 72)
					+ rsubstring(s, 38, 16) + scharAt(s, 82)
					+ rsubstring(s, 15);
			break;
		case 85:
			res = s.substring(3,11) + scharAt(s, 0) + s.substring(12, 55) + scharAt(s, 84) + s.substring(56,84);
			break;
		case 84:
			res = rsubstring(s, 78, 70) + scharAt(s, 14)
					+ rsubstring(s, 69, 37) + scharAt(s, 70)
					+ rsubstring(s, 36, 14) + scharAt(s, 80)
					+ reversed(s.substring(0, 14));
			break;
		case 83:
			res = rsubstring(s, 80, 63) + scharAt(s, 0) + rsubstring(s, 62, 0) + scharAt(s, 63);
			break;
		case 82:
			res = rsubstring(s, 80, 37) + scharAt(s, 7) + rsubstring(s, 36, 7)
					+ scharAt(s, 0) + rsubstring(s, 6, 0) + scharAt(s, 37); 
			break;
		case 81:
			res = scharAt(s, 56) + rsubstring(s, 79, 56) + scharAt(s, 41)
					+ rsubstring(s, 55, 41) + scharAt(s, 80)
					+ rsubstring(s, 40, 34) + scharAt(s, 0)
					+ rsubstring(s, 33, 29) + scharAt(s, 34)
					+ rsubstring(s, 28, 9) + scharAt(s, 29)
					+ rsubstring(s, 8, 0) + scharAt(s, 9);
			break;
		case 80:
			res = s.substring(1, 19) + scharAt(s, 0) + s.substring(20, 68) + scharAt(s, 19) + s.substring(69,80);
			break;
		case 79:
			res = scharAt(s, 54) + rsubstring(s, 77, 54) + scharAt(s, 39)
					+ rsubstring(s, 53, 39) + scharAt(s, 78)
					+ rsubstring(s, 38, 34) + scharAt(s, 0)
					+ rsubstring(s, 33, 29) + scharAt(s, 34)
					+ rsubstring(s, 28, 9) + scharAt(s, 29)
					+ rsubstring(s, 8, 0) + scharAt(s, 9);
			break;
		default:
			throw new Exception("Cannot decrypt");
		}
		return res;
	}
	
	/**
	 * Helper method to get a reverse substring from a string.
	 * Corresponds to Python notation: s[beginIndex:endIndex:-1].
	 * 
	 * @param s String to get the reverse substring from
	 * @param beginIndex Starting index (included)
	 * @param endIndex Ending index (excluded)
	 * @return The substring s[beginIndex:endIndex:-1].
	 */
	private static String rsubstring(String s, int beginIndex, int endIndex) {
		return new StringBuilder(s.substring(endIndex+1, beginIndex+1)).reverse().toString();
	}
	
	/**
	 * Helper method to get a reverse substring from a string.
	 * Corresponds to Python notation: s[beginIndex::-1].
	 * 
	 * @param s String to get the reverse substring from
	 * @param beginIndex Starting index (included)
	 * @return The substring s[beginIndex::-1].
	 */
	private static String rsubstring(String s, int beginIndex) {
		return rsubstring(s, beginIndex, -1);
	}
	
	/**
	 * Helper method to get a 1-char substring from a string.
	 * Corresponds to Python notation: s[index].
	 * 
	 * @param s String to get the 1-char substring from
	 * @param index The index of the character
	 * @return The substring s[index].
	 */
	private static String scharAt(String s, int index) {
		return s.substring(index, index+1);
	}
	
	/**
	 * Helper method to reverse a String.
	 * Corresponds to Python notation: s[::-1].
	 * 
	 * @param s The string to reverse
	 * @return The reversed string
	 */
	private static String reversed(String s) {
		return new StringBuilder(s).reverse().toString();
	}
	
	/**
	 * Determines whether a video is age-restricted.
	 * (Can influence the decryption algorithm)
	 * 
	 * @param videoId The YouTube video id
	 * @return True if the video is age-restricted, false otherwise
	 * @throws IOException In case of HTTP (e.g., 404) or network errors
	 */
	public static boolean isAgeRestrictedVideo(String videoId) throws IOException {
		if (!validateYouTubeID(videoId))
			throw new IllegalArgumentException("Invalid YouTube ID");
		
		final String url = "http://gdata.youtube.com/feeds/api/videos/" + videoId + "?v=2";
		try {
			return downloadAsString(url).contains("media:rating");
		}
		catch (MalformedURLException e) {
			assert false : "This exception should not occur for valid video ids!";
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Helper method to read the contents from an URL
	 * 
	 * @param url URL to read contents from
	 * @return The contents of the given URL
	 * 
	 * @throws MalformedURLException In case the URL is malformed
	 * @throws IOException In case of HTTP (e.g., 404) or network errors
	 */
	private static String downloadAsString(final String url) throws MalformedURLException, IOException {
		InputStream is = null;
		try {
			URL url2 = new URL(url);
			is = url2.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb.toString();
		}
		finally {
	        try {
	            if (is != null)
	            	is.close();
	        } catch (IOException ioe) {}
		}
	}
	
	/**
	 * Helper method to parse a query string.
	 * If it contains a question mark (?), it will ignore it and the
	 * substring before it.
	 * 
	 * The helper method also decodes any URL-encoding that is in place.
	 * 
	 * @param query Query string to be parsed
	 * @return A key-value map.
	 */
	static Map<String, String> parseQueryString(String query) {
		if (query.contains("?"))
            query = query.substring(query.indexOf('?') + 1);
        
		final String[] params = query.split("&");
		final Map<String, String> map = new HashMap<String, String>();
		
		for (final String param : params) {
			final String[] parts = param.split("=");
			map.put(parts[0], parts.length == 2 ? urlDecode(parts[1]) : "");
		}
		return map;
	}
	
	/**
	 * Helper method to decode an URL-encoded String.
	 * 
	 * @param urlEncoded An URL-encoded String
	 * @return A decoded String
	 */
	static String urlDecode(String urlEncoded) {
		try {
			return URLDecoder.decode( urlEncoded, "UTF-8" );
		} catch (UnsupportedEncodingException e) {
			assert false : "UTF-8 should be a supported encoding";
			e.printStackTrace();
			return null;
		}
	}
}

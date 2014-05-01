package cubrikproject.tud.likelines.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The FrameExtractor class extracts a frame (max: 480p) 
 * from a given video at a given time-point.
 * 
 * Basically, it is a wrapper for:
 *   FAST:      ffmpeg -y -ss OFFSET -i SRC -frames:v 1 -vf scale=-1:480 DST
 *   ACCURATE:  ffmpeg -y -i SRC -ss OFFSET -frames:v 1 -vf scale=-1:480 DST
 *   MIXED:     ffmpeg -y -ss OFFSET1 -i SRC -ss OFFSET2 -frames:v 1 -vf scale=-1:480 DST 
 * 
 * @author R. Vliegendhart
 */
public class FrameExtractor {
	/** path to the binary */
	private final String ffmpegPath;
	
	/**
	 * Extraction methods.
	 * 
	 * @see <a href="https://trac.ffmpeg.org/wiki/Seeking%20with%20FFmpeg">FFmpeg wiki: Seeking with FFmpeg</a>
	 */
	public enum ExtractionMethod {
		FAST, ACCURATE, FAST_ACCURATE;
	}
	
	/** Jump window for FAST_ACCURATE mode */
	private static int FAST_ACCURATE_OFFSET2 = 30;
	
	/**
	 * Constructs a FrameExtractor object.
	 * @param ffmpegPath Path to ffmpeg
	 */
	public FrameExtractor(String ffmpegPath) {
		this.ffmpegPath = ffmpegPath;
	}
	
	/**
	 * Extracts a frame from a video at a given timestamp.
	 * 
	 * @param source The video to extract the frame from
	 * @param timestamp The timestamp in seconds (positive)
	 * @param destination The location to store the extracted frame
	 * @param method The extraction method to apply
	 * @return A Process handle
	 * @throws IllegalArgumentException 
	 * @throws IOException
	 */
	public Process extract(String source, double timestamp, String destination, ExtractionMethod method) throws IOException {
		if (timestamp < 0) {
			throw new IllegalArgumentException("Timestamp needs to be positive");
		}
		
		List<String> arguments = new ArrayList<String>();
		arguments.add(ffmpegPath);
		arguments.add("-y");
		double offset1, offset2 = 0;
				
		if (method == ExtractionMethod.FAST_ACCURATE && timestamp > FAST_ACCURATE_OFFSET2) {
			offset2 = FAST_ACCURATE_OFFSET2;
		}
		else if (method == ExtractionMethod.ACCURATE) {
			offset2 = timestamp;
		}
		
		offset1 = timestamp - offset2;
				
		if (offset1 > 0) {
			arguments.add("-ss");
			arguments.add("" + offset1);
		}
		
		arguments.add("-i");
		arguments.add(source);
		
		if (offset2 > 0) {
			arguments.add("-ss");
			arguments.add("" + offset2);
		}
		
		arguments.add("-frames:v");
		arguments.add("1");
		arguments.add("-vf");
		arguments.add("scale=-1:480");
		arguments.add(destination);
		
		final ProcessBuilder pb = new ProcessBuilder(arguments.toArray(new String[arguments.size()]));
		return pb.start();
	}
	
	/**
	 * Extracts a frame from a video at a given timestamp (blocking).
	 * 
	 * @param source The video to extract the frame from
	 * @param timestamp The timestamp in seconds (positive)
	 * @param destination The location to store the extracted frame
	 * @param method The extraction method to apply
	 * @return The exit value of the process
	 * @throws IllegalArgumentException 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int extractAndWait(String source, double timestamp, String destination, ExtractionMethod method)
			throws IOException, InterruptedException {
		final Process proc = extract(source, timestamp, destination, method);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		while (br.readLine() != null);
		return proc.waitFor();
	}
	
	/**
	 * Extracts a frame from a video at a given timestamp (blocking)
	 * using the fast and accurate method
	 * 
	 * @param source The video to extract the frame from
	 * @param timestamp The timestamp in seconds (positive)
	 * @param destination The location to store the extracted frame
	 * @param method The extraction method to apply
	 * @return The exit value of the process
	 * @throws IllegalArgumentException 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int extractAndWait(String source, double timestamp, String destination)
			throws IOException, InterruptedException {
		final Process proc = extract(source, timestamp, destination, ExtractionMethod.FAST_ACCURATE);
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		while (br.readLine() != null);
		return proc.waitFor();
	}
}

package cubrikproject.tud.likelines.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * The MotionActivityAnalyzer class performs an analysis on converted videos.
 * 
 * @see <a href="http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=927428">MPEG-7 visual motion descriptors</a>
 * @author R. Vliegendhart
 */
public class MotionActivityAnalyzer {
	/** path to the binary */
	private final String motionActivityPath;
	
	private final int SKIPPED_FRAME = -1;
	private final int NO_MOTION = -2;
	private final int DUPLICATE_FRAME = -3;
	
	/**
	 * Constructs a MotionActivityAnalyzer object.
	 * @param motionActivityPath Path to motionActivity
	 */
	public MotionActivityAnalyzer(String motionActivityPath) {
		this.motionActivityPath = motionActivityPath;
	}
	
	/**
	 * Performs motion activity analysis on a video file.
	 * 
	 * @param source The video to be analyzed
	 * @return The results of the analysis
	 * @throws IOException
	 */
	public double[] analyze(String source) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(motionActivityPath, source);
		
		final Process proc = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String line;
		
		ArrayList<Double> values = new ArrayList<Double>();
		double[] scores;
		
		// read proc's STDOUT
		while ((line = br.readLine()) != null) {
			values.add(Double.parseDouble(line));
		}
				
		scores = new double[values.size()];
		for (int i = 0; i < scores.length; i++) {
			double score = values.get(i);
			scores[i] = (score == NO_MOTION) ? 0 : score;
		}
		
		// TODO: process SKIPPED_FRAME and DUPLICATE_FRAME
		
		return scores;
	}
	
	
}

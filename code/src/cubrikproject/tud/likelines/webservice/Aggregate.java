package cubrikproject.tud.likelines.webservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cubrikproject.tud.likelines.util.ArrayFunctions;
import cubrikproject.tud.likelines.util.Range;
import cubrikproject.tud.likelines.util.SmoothedFunction;
import cubrikproject.tud.likelines.webservice.MCAData.TYPE;

/**
 * Representation of the aggregate JSON object returned by server.
 * 
 * @author R. Vliegendhart
 */
public class Aggregate {
	
	/** Original response from the server */
	private final JsonObject _aggregate;
	
	/** Read-only list of liked points */
	public final List<? extends Double> likedPoints;
	
	/** Read-only list of playback sessions */
	public final List<? extends PlaybackSession> playbacks;
	
	/** Read-only map of MCA data */
	public final Map<String, ? extends MCAData> mcaData;
	
	/** Estimate of the video's length */
	public final int durationEstimate;
	
	/** Default heat-map size */
	public final int DEFAULT_HEATMAP_SIZE = 425;
	
	/**
	 * Constructs a representation of the aggregate JSON object returned by the
	 * LikeLines server.
	 * 
	 * @param aggregate
	 *            JSON object returned by the LikeLines server
	 */
	public Aggregate(JsonObject aggregate) {
		_aggregate = aggregate;

		likedPoints = readLikedPoints(_aggregate);
		playbacks = readPlaybacks(_aggregate);
		mcaData = readMCAData(_aggregate);
		durationEstimate = estimateDuration(likedPoints, playbacks, mcaData);
	}
	
	/**
	 * Helper method to extract a list of liked points from the JSON object
	 * returned by the server.
	 * 
	 * @param aggregate
	 *            JSON object returned by the server
	 * @return A list of liked points
	 */
	private static List<Double> readLikedPoints(JsonObject aggregate) {
		JsonArray likedPoints = aggregate.get("likedPoints").getAsJsonArray();
		List<Double> res = new ArrayList<Double>(likedPoints.size());
		
		for (JsonElement jsonElement : likedPoints) {
			res.add(jsonElement.getAsDouble());
		}
		
		return res;
	}

	/**
	 * Helper method to extract a list of playback sessions from the JSON object
	 * returned by the server.
	 * 
	 * @param aggregate
	 *            JSON object returned by the server
	 * @return A list of playback sessions
	 */
	private static List<PlaybackSession> readPlaybacks(JsonObject aggregate) {
		JsonArray playbacks = aggregate.get("playbacks").getAsJsonArray();
		List<PlaybackSession> res = new ArrayList<PlaybackSession>(playbacks.size());
		
		for (JsonElement jsonElement : playbacks) {
			res.add(PlaybackSession.fromJSONArray(jsonElement.getAsJsonArray()));
		}
		
		return res;
	}
	
	/**
	 * Helper method to extract a map of MCA data from the JSON object
	 * returned by the server.
	 * 
	 * @param aggregate JSON object returned by the server 
	 * @return A map of MCA data
	 */
	private Map<String, ? extends MCAData> readMCAData(JsonObject aggregate) {
		Map<String, MCAData> res = new HashMap<String, MCAData>();
		JsonObject mca = aggregate.get("mca").getAsJsonObject();
		
		for (Map.Entry<String, JsonElement> entry : mca.entrySet()) {
			String name = entry.getKey();
			JsonObject value = entry.getValue().getAsJsonObject();
			
			res.put(name, MCAData.fromJSONObject(name, value));
		}
		
		return res;
	}
	
	/**
	 * Helper method to estimate the duration of a video.
	 * 
	 * @param likedPoints List of liked points
	 * @param playbacks List of playback sessions
	 * @param mcaData Map of MCA data
	 * @return Estimate of the video's duration
	 */
	private static int estimateDuration(List<? extends Double> likedPoints,
			List<? extends PlaybackSession> playbacks,
			Map<String, ? extends MCAData> mcaData) {
		
		double durationEstimate = likedPoints.isEmpty() ? 1 : Collections.max(likedPoints);
		
		for (PlaybackSession playback : playbacks)
			for (PlayedSegment playedSegment : playback)
				durationEstimate = Math.max(durationEstimate, playedSegment.end);
		
		for (Entry<String, ? extends MCAData> entry : mcaData.entrySet()) {
			String name = entry.getKey();
			MCAData curMCA = entry.getValue();
			
			// Skip deeplinks (noisy) and curves
			if (name.equals("deeplinks") || curMCA.type == TYPE.CURVE)
				continue;
			
			for (double point : curMCA.data)
				durationEstimate = Math.max(durationEstimate, point);
		}
		
		return (int) Math.ceil(durationEstimate);
	}
	
	/**
	 * Computes the total time in seconds people have watched this video.
	 * 
	 * @return Total time in seconds people have watched this video
	 */
	public double timeWatched() {
		double sum = 0;
		for (PlaybackSession playback : playbacks)
			for (PlayedSegment playedSegment : playback)
				sum += playedSegment.end - playedSegment.start;
		
		return sum;
	}

	/**
	 * Compute the playback histogram.
	 * 
	 * @return A playback histogram with a bin per video-second
	 * @see <a href="https://github.com/ShinNoNoir/likelines-player/blob/51d6d05a199e2de709fc5b2241e2f736664c10e6/js/likelines.js#L404">JavaScript reference implementation</a>
	 */
	public double[] playbackHistogram() {
		double[] histogram = new double[durationEstimate];
		
		for (PlaybackSession playback : playbacks)
			for (PlayedSegment playedSegment : playback)
				for (int i = (int) playedSegment.start; i <= playedSegment.end
						&& i <= durationEstimate; i++)
					histogram[i]++;
		
		return histogram;
	}
	
	/**
	 * Compute the combined MCA curve.
	 * 
	 * @return An MCA curve discretized per video-second
	 * @see <a href="https://github.com/ShinNoNoir/likelines-player/blob/f8b02034d460a2fced95e4183cbd50c62e6d29ea/js/likelines.js#L680">JavaScript reference implementation</a>
	 */
	public double[] combinedMCACurve() {
		final int curveSize = Math.max(durationEstimate, DEFAULT_HEATMAP_SIZE);
		double[] curve = new double[curveSize];
		
		for (MCAData curMCA : mcaData.values()) {
			double[] curCurve = null;
			double weight = curMCA.weight;
			
			switch (curMCA.type) {
				case POINT:
					SmoothedFunction f = new SmoothedFunction(curMCA.data);
					curCurve = ArrayFunctions.projectOntoArray(f, new Range(0, durationEstimate), curveSize);
					break;
				case CURVE:
					double[] dataArray = new double[curMCA.data.size()]; 
					for (int i = 0; i < dataArray.length; i++) {
						dataArray[i] = curMCA.data.get(i);
					}
					curCurve = ArrayFunctions.scaleArray(dataArray, curveSize);
					break;
				default:
					assert false : "Unexpected MCA Type";
					break;
			}
			
			ArrayFunctions.normalize(curCurve);
			
			for (int i = 0; i < curCurve.length; i++) {
				curve[i] += curCurve[i] * weight;
			}
		}
		return curve;
	}
	
	
	/**
	 * Compute the heat-map with default parameters set.
	 * 
	 * @return A heat-map for the video
	 * @see {@link #heatmap(int)}
	 */
	public double[] heatmap() {
		return heatmap(DEFAULT_HEATMAP_SIZE);
	}
	
	/**
	 * Compute the heat-map.
	 * 
	 * @param heatmapSize The number of bins in the heat-map
	 * @return A heat-map for the video
	 * @see <a href="https://github.com/ShinNoNoir/likelines-player/blob/f8b02034d460a2fced95e4183cbd50c62e6d29ea/js/likelines.js#L660">JavaScript reference implementation</a>
	 */
	public double[] heatmap(int heatmapSize) {
		final double[] heatmap = new double[heatmapSize];
		SmoothedFunction f = new SmoothedFunction(likedPoints);
		final double[] smoothedLikes = ArrayFunctions.projectOntoArray(f, new Range(0, durationEstimate), heatmapSize);
		final double[] scaledPlayback = ArrayFunctions.scaleArray(playbackHistogram(), heatmapSize);
		final double[] scaledMCACurve = ArrayFunctions.scaleArray(combinedMCACurve(), heatmapSize);
		
		ArrayFunctions.normalize(smoothedLikes);
		ArrayFunctions.normalize(scaledPlayback);
		ArrayFunctions.normalize(scaledMCACurve);
		
		// Weighted sum (for now, 1.0)
		final double[][] timecodeEvidences = new double[][]{ smoothedLikes, scaledPlayback, scaledMCACurve };
		final double WEIGHT = 1.0;

		double scale = 0;
		for (int i = 0; i < heatmapSize; i++) {
			for (double[] timecodeEvidence : timecodeEvidences) {
				heatmap[i] += Math.max(timecodeEvidence[i] * WEIGHT, 0);
				scale = Math.max(scale, heatmap[i]);
			}
		}
		if (scale != 0) {
			for (int i=0; i < heatmapSize; i++)
				heatmap[i] /= scale;
		}

		return heatmap;
	}
}

package cubrikproject.tud.likelines.webservice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Representation of a user's playback behaviour.
 * 
 * @author R. Vliegendhart
 */
public class PlaybackSession implements Iterable<PlayedSegment>{
	
	/** A list of played segments */
	private List<PlayedSegment> playedSegments;
	
	/**
	 * Constructs a PlaybackSession object.
	 * 
	 * @param playedSegments A list of PlayedSegments
	 */
	private PlaybackSession(List<PlayedSegment> playedSegments) {
		this.playedSegments = playedSegments;
	}
	
	/**
	 * Constructs a PlaybackSession object from a JSON array.
	 * 
	 * @param playbackSession JSON array containing played segments
	 * @return A PlaybackSession
	 */
	public static PlaybackSession fromJSONArray(JsonArray playbackSession) {
		final List<PlayedSegment> playedSegments = new ArrayList<PlayedSegment>();
		
		for (JsonElement playedSegment : playbackSession) {
			playedSegments.add(PlayedSegment.fromJSONArray(playedSegment.getAsJsonArray()));
		}
		
		return new PlaybackSession(playedSegments);
	}
	

	@Override
	public Iterator<PlayedSegment> iterator() {
		return playedSegments.iterator();
	}
}

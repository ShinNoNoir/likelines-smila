package cubrikproject.tud.likelines.webservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Represents a played fragment during a video watching session
 * 
 * @author R. Vliegendhart
 */
public class PlayedSegment {
	/** The starting point of the played segment */
	final public double start;
	
	/** The end-point of the played segment */
	final public double end;
	
	/**
	 * Constructs a PlayedSegment.
	 * 
	 * @param start Starting point of the played segment
	 * @param end End-point of the played segment
	 */
	public PlayedSegment(double start, double end) {
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Constructs a PlayedSegment object from a JSON array.
	 * 
	 * @param array JSON array with start and end point
	 * @return A PlayedSegment
	 */
	public static PlayedSegment fromJSONArray(JsonArray array) {
		JsonElement start = array.get(0);
		JsonElement end = array.get(1);
		
		return new PlayedSegment(start.isJsonNull() ? 0 : start.getAsDouble(), 
		                         end.isJsonNull() ? 0 : end.getAsDouble());
	}
}
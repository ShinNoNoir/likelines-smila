package cubrikproject.tud.likelines.util;

/**
 * Range consisting of a beginning and an end
 * 
 * @author R. Vliegendhart
 */
public class Range {
	/** Start of range */
	public int begin;
	/** End of range */
	public int end;
	
	/**
	 * Constructs a Range object.
	 * 
	 * @param begin Start of range
	 * @param end End of range
	 */
	public Range(int begin, int end) {
		this.begin = begin;
		this.end = end;
	}
}
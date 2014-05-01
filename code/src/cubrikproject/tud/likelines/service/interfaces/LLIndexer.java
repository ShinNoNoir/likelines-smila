package cubrikproject.tud.likelines.service.interfaces;

import java.util.List;

import cubrikproject.tud.likelines.webservice.LikeLinesWebService;

public interface LLIndexer {
	
	/**
	 * Schedules an MCA task for a given video.
	 * 
	 * @param videoId The ID of the video for which MCA needs to be performed
	 * @param llServer The LikeLines webservice to which the MCA needs to be posted
	 * @param contentAnalysisRequired Flag indicating whether content analysis is required
	 */
	public void scheduleMCA(String videoId, LikeLinesWebService llServer, boolean contentAnalysisRequired);

	/**
	 * Extracts frames from a video at given timestamps.
	 * 
	 * @param videoId The ID of the video which to extract frames from
	 * @param nKeyFrames A list of timestamps
	 * 
	 * @return A list of base64 encoded JPEGs
	 */
	public List<String> extractFrames(String videoId, double[] nKeyFrames);

}

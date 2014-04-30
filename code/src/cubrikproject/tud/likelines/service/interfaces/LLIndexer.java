package cubrikproject.tud.likelines.service.interfaces;

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

}

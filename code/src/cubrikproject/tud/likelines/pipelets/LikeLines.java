package cubrikproject.tud.likelines.pipelets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.smila.blackboard.Blackboard;
import org.eclipse.smila.blackboard.BlackboardAccessException;
import org.eclipse.smila.datamodel.AnyMap;
import org.eclipse.smila.datamodel.AnySeq;
import org.eclipse.smila.processing.Pipelet;
import org.eclipse.smila.processing.ProcessingException;
import org.eclipse.smila.processing.parameters.ParameterAccessor;
import org.eclipse.smila.processing.util.ProcessingConstants;
import org.eclipse.smila.processing.util.ResultCollector;
import org.eclipse.smila.utils.service.ServiceUtils;

import cubrikproject.tud.likelines.service.interfaces.LLIndexer;
import cubrikproject.tud.likelines.webservice.Aggregate;
import cubrikproject.tud.likelines.webservice.LikeLinesWebService;

/**
 * The LikeLines pipelet communicates with a LikeLines server in order to
 * compute the top N most interesting keyframes.
 * 
 * @author R. Vliegendhart
 */
public class LikeLines implements Pipelet {

	/** config property name for attribute name to read the video ID from. */
	private static final String PARAM_ATTRIBUTE = "input_field";

	/** config property name for the LikeLines server to use (opt.). */
	private static final String PARAM_SERVER = "server";

	/** default server */
	private static final String DEFAULT_SERVER = "http://likelines-shinnonoir.dotcloud.com";

	/** config property name for the number of top keyframes to find. */
	private static final String PARAM_N = "n";

	/** config property name-prefix for storing the output. */
	private static final String PARAM_OUTPUT = "output_field";

	/** the pipelet's configuration. */
	private AnyMap _config;

	/** local logger. */
	private final Log _log = LogFactory.getLog(getClass());
	
	/** LikeLines indexer service */
	private LLIndexer _indexer;
	
	@Override
	public void configure(AnyMap configuration) {
		_config = configuration;
	}
	
	private synchronized LLIndexer getLLIndexer() throws ProcessingException {
		if (_indexer == null) {
			try {
				_indexer = ServiceUtils.getService(LLIndexer.class);
			} catch (final Exception e) {
				_log.warn("Error while waiting for LLIndexer service to come up.", e);
			}

			if (_indexer == null) {
				throw new ProcessingException("No LLIndexer service available, giving up");
			}
		}
		return _indexer;
	}
	
	@Override
	public String[] process(Blackboard blackboard, String[] recordIds)
			throws ProcessingException {
		
		_indexer = getLLIndexer();
		System.out.println(">>> getLLIndexer() = " + _indexer.toString());
		
		final ParameterAccessor paramAccessor = new ParameterAccessor(blackboard, _config);
		final ResultCollector resultCollector = 
				new ResultCollector(paramAccessor, _log, ProcessingConstants.FAIL_ON_ERROR_DEFAULT);

		final String serverUrl = paramAccessor.getParameter(PARAM_SERVER, DEFAULT_SERVER);
		final int N = Integer.parseInt(paramAccessor.getRequiredParameter(PARAM_N));
		final String inputField = paramAccessor.getRequiredParameter(PARAM_ATTRIBUTE);
		final String outputField = paramAccessor.getRequiredParameter(PARAM_OUTPUT);
		
		for (String id : recordIds) {
			try {
				final String videoId = blackboard.getMetadata(id).getStringValue(inputField);
				final AnySeq timecodes = blackboard.getMetadata(id).getSeq(outputField, true);
				
				if (videoId == null)
					throw new ProcessingException("Missing videoId");
				
				LikeLinesWebService server = new LikeLinesWebService(serverUrl);
				
				Aggregate agg = server.aggregate(videoId);
				for (double d : server.getNKeyFrames(N, agg)) {
					timecodes.add(d);
				}
				resultCollector.addResult(id);
			}
			catch (Exception e) {
				e.printStackTrace();
				resultCollector.addFailedResult(id, e);
			}
			
		}

		return recordIds;
	}

}

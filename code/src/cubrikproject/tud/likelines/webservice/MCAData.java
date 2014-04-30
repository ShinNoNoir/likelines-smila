package cubrikproject.tud.likelines.webservice;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Representation of the output of some MCA algorithm.
 * 
 * @author R. Vliegendhart
 */
public class MCAData {
	
	/** 
	 * The output type of an MCA algorithm is either: 
	 *   - a continuous interest curve;
	 *   - a list of interesting points.
	 */
	public enum TYPE { CURVE, POINT	}
	
	/** The MCA algorithm's name */
	public final String name;
	
	/** The MCA algorithm's output type */
	public final TYPE type;
	
	/** The MCA algorithm's output weight (if missing: 1.0) */
	public final double weight;
	
	/** The MCA algorithm's output */
	public List<? extends Double> data;
	
	/**
	 * Constructs an MCAData object.
	 * 
	 * @param name
	 *            The name of the MCA algorithm
	 * @param type
	 *            The type of the MCA algorithm's output
	 * @param weight
	 *            The weight of the MCA algorithm's output
	 * @param data
	 *            The output of the MCA algorithm
	 */
	private MCAData(String name, TYPE type, double weight, List<? extends Double> data) {
		this.name = name;
		this.type = type;
		this.weight = weight;
		this.data = data;
	}
	
	/**
	 * Constructs an MCAData object from a JSON object.
	 * 
	 * @param name
	 *            The name of the MCA algorithm
	 * @param mca
	 *            JSON object containing type, data and weight of the
	 *            algorithm's output
	 * @return An MCAData object
	 */
	public static MCAData fromJSONObject(String name, JsonObject mca) {
		TYPE type = TYPE.valueOf(mca.get("type").getAsString().toUpperCase());
		
		JsonElement jsonWeight = mca.get("weight");
		double weight = jsonWeight == null ? 1.0 : jsonWeight.getAsDouble();
		
		JsonArray jsonData = mca.get("data").getAsJsonArray();
		List<Double> data = new ArrayList<Double>(jsonData.size());
		for (JsonElement jsonValue : jsonData) {
			data.add(jsonValue.getAsDouble());
		}
		
		return new MCAData(name, type, weight, data);
	}
}

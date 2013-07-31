import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cubrikproject.tud.likelines.service.Aggregate;
import cubrikproject.tud.likelines.util.ArrayFunctions;


public class Test {

	
	public static void main(String[] args) {
		double[] playback = new double[] {10.0, 20.0, 30.0, 40.0};
		double[] points = new double[] {30.0, 60.0};
		double duration = 120;
		
		double[] xs = new double[] {-120, 30, 60};
		
		ArrayFunctions.normalize(xs);
		points = ArrayFunctions.scaleArray(playback, 3);
		
		xs = ArrayFunctions.scaleArray(new double[]{0.0, 10}, 30);
		
		System.out.println(join(", ", points));
		System.out.println(join(", ", xs));
	}
	
	static <T> String join(String sep, T[] array) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for (T t : array) {
			sb.append(delim);
			sb.append(t);
			delim = sep;
		}
		return sb.toString();
	}
	
	static String join(String sep, double[] array) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for (double t : array) {
			sb.append(delim);
			sb.append(t);
			delim = sep;
		}
		return sb.toString();
	}
	
	static <T> String join(String sep, Collection<? extends T> array) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for (T t : array) {
			sb.append(delim);
			sb.append(t);
			delim = sep;
		}
		return sb.toString();
	}
}

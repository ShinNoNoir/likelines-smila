package cubrikproject.tud.likelines.util;


public class ArrayFunctions {

	/**
	 * Resizes an array.
	 * 
	 * @param data The input array
	 * @param newSize The new size of the resized array
	 * @return Resized array
	 * @see <a href="https://github.com/ShinNoNoir/likelines-player/blob/51d6d05a199e2de709fc5b2241e2f736664c10e6/js/likelines.js#L954">JavaScript reference implementation</a>
	 */
	public static double[] scaleArray(double[] data, int newSize) {
		final int n = data.length;
		double scaledArray[] = new double[newSize];
		
		if (n == 0 || newSize == 0) {
			// zeros(newSize)
		}
		else if (n <= 2) {
			// linspace
			final double d1 = data[0];
			final double d2 = data[n-1];
			final int numPoints = newSize;
			
			double step = (d2-d1) / (numPoints-1);
			for (int i = 0; i < numPoints-1; i++) {
				scaledArray[i] = d1 + i*step;
			}
			scaledArray[newSize-1] = d2;
		}
		else {
			// interpolate
			final double step = (n-1.0)/(newSize-1);
			
			for (int j = 0; j < newSize-1; j++) {
				double x = j*step;
				int i = (int) Math.floor(x);
				scaledArray[j] = data[i] + (x-i) * (data[i+1] - data[i]);
			}
			scaledArray[newSize-1] = data[n-1];
		}
		
		return scaledArray;
	}

	/**
	 * Helper method to project a function onto an array.
	 * 
	 * @param f The function to project onto an Array
	 * @param range The range of the function to be projected
	 * @param size The size of the array
	 * @return An array containing f(range.begin) ... f(range.end) 
	 * @see <a href="https://github.com/ShinNoNoir/likelines-player/blob/51d6d05a199e2de709fc5b2241e2f736664c10e6/js/likelines.js#L630">JavaScript reference implementation</a>
	 */
	public static double[] projectOntoArray(SmoothedFunction f, Range range, int size) {
		final double[] smoothed = new double[size];
		final double step = (range.end-1.0 - range.begin) / (size-1);
		
		double x;
		for (int i = 0; i < size-1; i++) {
			x = i*step;
			smoothed[i] = f.apply(x);
		}
		x = size-1;
		smoothed[size-1] = f.apply(x);
		
		return smoothed;
	}

	/**
	 * Normalizes the input array's values to lie between [-1,1].
	 * 
	 * @param array The input array
	 */
	public static void normalize(double[] array) {
		double min, max;
		min = max = array[0];
		for (int i = 1; i < array.length; i++) {
			max = Math.max(max, array[i]);
			min = Math.min(min, array[i]);
		}
		double scale = Math.max(Math.abs(max), Math.abs(min));
		
		if (scale != 0)
			for (int i = 0; i < array.length; i++)
				array[i] /= scale;
	}
}

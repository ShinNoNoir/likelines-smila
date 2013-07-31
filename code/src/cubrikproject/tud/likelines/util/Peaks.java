package cubrikproject.tud.likelines.util;

import java.util.ArrayList;
import java.util.List;

public class Peaks {
	/** List of valleys */
	public final List<Point> valleys;
	/** List of peaks */
	public final List<Point> peaks;
	
	/**
	 * Constructs a Peaks object (collection of valleys and peaks).
	 * 
	 * @param valleys List of valleys.
	 * @param peaks List of peaks.
	 */
	private Peaks(List<Point> valleys, List<Point> peaks) {
		this.valleys = valleys;
		this.peaks = peaks;
	}

	/**
	 * Extract peaks from an array of points.
	 * 
	 * Adapted from <a href="http://billauer.co.il/peakdet.html">http://billauer.co.il/peakdet.html</a>.
	 * 
	 * @param points
	 *            An array of points (y-coordinates) to extract peaks from.
	 * @param x
	 *            Optional (null). Corresponding list of x-coordinates.
	 * @param delta
	 *            A point is considered a maximum peak if it has the maximal
	 *            value, and was preceded (to the left) by a value lower by
	 *            DELTA.
	 * @return A set of peaks
	 */
	public static Peaks extract(double[] points, double[] x, double delta) {
		List<Point> maxtab = new ArrayList<>();
		List<Point> mintab = new ArrayList<>();
		Peaks res = new Peaks(mintab, maxtab);
		
		if (x == null) {
			x = new double[points.length];
			for (int i = 0; i < x.length; i++) {
				x[i] = i+1;
			}
		}
		
		assert points.length == x.length : "Input arguments points and x must have the same length";
		assert delta > 0 : "Input argument delta must be positive";
		
		double mn = Double.POSITIVE_INFINITY;
		double mx = Double.NEGATIVE_INFINITY;
		double mnpos = Double.NaN;
		double mxpos = Double.NaN;
		
		boolean lookformax = true;
		
		for (int i = 0; i < points.length; i++) {
			double cur = points[i];
			if (cur > mx) {
				mx = cur;
				mxpos = x[i];
			}
			if (cur < mn) {
				mn = cur;
				mnpos = x[i];
			}
			
			if (lookformax) {
				if (cur < mx-delta) {
					maxtab.add(new Point(mxpos, mx));
					mn = cur;
					mnpos = x[i];
					lookformax = false;
				}
			}
			else {
				if (cur > mn+delta) {
					mintab.add(new Point(mnpos, mn));
					mx = cur;
					mxpos = x[i];
					lookformax = true;
				}
			}
		}
				
		return res;
	}
	
	
	/** Representation of point (x,y). */
	public static class Point {
		/** x-coordinate */
		public final double x;
		/** y-coordinate */
		public final double y;
			
		/**
		 * Constructs a Point object.
		 * 
		 * @param x x-coordinate
		 * @param y y-coordinate
		 */
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

	}
}

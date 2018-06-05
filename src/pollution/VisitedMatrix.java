package pollution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class VisitedMatrix {
	HashMap<Integer, HashSet<Integer>> visited;
	public VisitedMatrix() {
		visited = new HashMap<Integer, HashSet<Integer>>();
	}
	
	void add(Point p) {
		add(p.getX(), p.getY());
	}
	void add(int a, int b) {
		if (!visited.containsKey(a)) visited.put(a, new HashSet<Integer>());
		visited.get(a).add(b);
	}
	
	boolean contains(Point p) {
		return contains(p.getX(), p.getY());
	}
	boolean contains(int a, int b) {
		if (visited.containsKey(a)) return visited.get(a).contains(b);
		return false;
	}
	
	void remove(Point p) {
		remove(p.getX(), p.getY());
	}
	void remove(int a, int b) {
		if (visited.containsKey(a)) {
			visited.get(a).remove(b);
			if (visited.get(a).isEmpty()) visited.remove(a);
		}
	}
	
	public Point[] toArray() {
		ArrayList<Point> al = new ArrayList<Point>();
		
		Iterator<Integer> i1 = visited.keySet().iterator();
		int e;
		Iterator<Integer> i2;
		
		while(i1.hasNext()) {
			e = i1.next();
			i2 = visited.get(e).iterator();
			while(i2.hasNext()) {
				al.add(new Point(e, i2.next()));
			}
		}
		return al.toArray(new Point[0]);
	}
	
	public String toString() {
		Arrays.deepToString(this.toArray());
		return null;
	}
	
}
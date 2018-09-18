package pollution.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class PointSet {
	private HashMap<Integer, HashSet<Integer>> pSet;
	public PointSet() {
		pSet = new HashMap<Integer, HashSet<Integer>>();
	}
	
	public void add(Point p) {
		add(p.getX(), p.getY());
	}
	public void add(int a, int b) {
		if (!pSet.containsKey(a)) pSet.put(a, new HashSet<Integer>());
		pSet.get(a).add(b);
	}
	
	public boolean contains(Point p) {
		return contains(p.getX(), p.getY());
	}
	public boolean contains(int a, int b) {
		if (pSet.containsKey(a)) return pSet.get(a).contains(b);
		return false;
	}
	
	public void remove(Point p) {
		remove(p.getX(), p.getY());
	}
	public void remove(int a, int b) {
		if (pSet.containsKey(a)) {
			pSet.get(a).remove(b);
			if (pSet.get(a).isEmpty()) pSet.remove(a);
		}
	}
	
	public Point[] toArray() {
		ArrayList<Point> al = new ArrayList<Point>();
		
		Iterator<Integer> i1 = pSet.keySet().iterator();
		int e;
		Iterator<Integer> i2;
		
		while(i1.hasNext()) {
			e = i1.next();
			i2 = pSet.get(e).iterator();
			while(i2.hasNext()) {
				al.add(new Point(e, i2.next()));
			}
		}
		return al.toArray(new Point[0]);
	}
	
	@Override
	public String toString() {
		return Arrays.deepToString(this.toArray());
	}
	
	public Iterator<Point> iterator() {
		return new Itr();
	}
	
	public boolean isEmpty() {
		return pSet.isEmpty();
	}
	
	private class Itr implements Iterator<Point> {
		Iterator<Integer> itA;
		Iterator<Integer> itB;
		int a;

		public Itr() {
			itA = pSet.keySet().iterator();
			a = itA.next();
			itB = pSet.get(a).iterator();
		}

		@Override
		public boolean hasNext() {
			if(itA.hasNext()) return true;
			return itB.hasNext();
		}

		@Override
		public Point next() {
			while(!itB.hasNext() && itA.hasNext()) {
				a = itA.next();
				itB = pSet.get(a).iterator();
			}
			if(itB.hasNext()) return new Point(a, itB.next());
			return null;
		}
		
	}
	
}
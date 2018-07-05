package pollution;

import java.util.Iterator;

import api.*;
import api.pojo.UTMCoordinates;
import smile.data.SparseDataset;

public class Pollution extends Thread {
	
	boolean [][] visited;
	
	public static UTMCoordinates origin;
	int sizeX, sizeY;
	
	// Move to a point within the grid
	void move(Point p) {
		move(p.x, p.y);
	}
	void move(int x, int y) {
		Copter.moveUAV(0, Tools.UTMToGeo(origin.Easting + (x * PollutionParam.density), origin.Northing + (y * PollutionParam.density)), (float) PollutionParam.altitude, 1.0, 1.0);
	}
	
	double moveAndRead(Point p) {
		double m;
		move(p);
		m = PollutionParam.sensor.read();
		PollutionParam.measurements.set(p.getX(), p.getY(), m);
		visited[p.getX()][p.getY()] = true;
		GUI.log("Read: [" + p.getX() + ", " + p.getY() + "] = " + m);
		return m;
	}
	

	@Override
	public void run() {
		// TODO Implement PdUC
		//long startTime = System.currentTimeMillis();
		
		Point p1, p2;
		double m1, m2;
		Point pTemp;
		int round, radius;
		int i, j;
		PointSet points;
		Iterator<Point> pts;
				
		// Calculate grid size and origin
		sizeX = (int) ((double) PollutionParam.width / PollutionParam.density);
		sizeY = (int) ((double) PollutionParam.length / PollutionParam.density);
		origin = api.Tools.geoToUTM(PollutionParam.startLocation.latitude, PollutionParam.startLocation.longitude);
		origin.Easting -= PollutionParam.width/2.0;
		origin.Northing -= PollutionParam.length/2.0;
		
		// new booleans are initialized to false by default, this is what we want
		visited = new boolean[sizeX][sizeY];
		
		/* Wait until takeoff has finished */
		try {
			while(!PollutionParam.ready) sleep(100);
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
		PollutionParam.measurements = new SparseDataset();
		//data = new HashMap<Double, HashMap<Double, Double>>();
		
		/* Start Algorithm */
		GUI.log("Start PdUC Algorithm");
		
		// Set initial measurement location to the grid centre
		p1 = new Point(sizeX / 2, sizeY / 2);
		
		// Initial p1 measurement
		m1 = moveAndRead(p1);
		
		// Initial p2 measurement
		p2 = p1.clone();
		p2.addY(1);	
		m2 = moveAndRead(p2);
		
		boolean isMax = false;
		boolean found;
		//boolean finished = false;

		
		/* Main loop */
		while(!isMax) {
			while(!isMax) {
				/* Run & Tumble */
				
				// Chemotaxis + PSO
				if(m2 - m1 > PollutionParam.pThreshold) {
					// Run
					GUI.log("Chemotaxis: Run");
					pTemp = p1;
					p1 = p2.clone();
					p2.add(p2.distVector(pTemp));
					
					if(p2.isInside(sizeX, sizeY)) {
						// Measure next step
						m2 = moveAndRead(p2);
					} else {
						p2 = p1.clone();
						m2 = m1;
					}
					

					
				} else {
					// Tumble
					GUI.log("Chemotaxis: Tumble");
					found = false;
					
					points = new PointSet();
					for(i = -1; i < 2; i++)
						for(j = -1; j< 2; j++) {
							pTemp = new Point(p1.getX() + i, p1.getY() + j);
							if(pTemp.isInside(sizeX, sizeY) && !(visited[pTemp.getX()][pTemp.getY()])) points.add(pTemp);
						}
					
					if(!points.isEmpty()) {
						pts = points.iterator();
						
						// -- Get closest point
						Point pt, minPt;
						double dist, minDist;
						
						// ---- First element is temporary closest point
						minPt = pts.next();
						minDist = p2.distance(minPt);
						// ---- Iterate through all elements to find closest point
						while(pts.hasNext()) {
							pt = pts.next();
							dist = p2.distance(pt);
							//GUI	.log(pt.toString() + " > " + dist);
							if (dist < minDist) {
								minDist = dist;
								minPt = pt;
							}
						}
						
						// ---- Read closest point
						p2 = minPt;
						m2 = moveAndRead(p2);
					} else {
						isMax = true;
					}
					
					
					
//					for(i = -1; i < 2 && !found; i++)
//						for(j = -1; j< 2 && !found; j++) {
//							pTemp = new Point(p1.getX() + i, p1.getY() + j);
//							//GUI.log("\t" + pTemp);
//							if(!(visited[pTemp.getX()][pTemp.getY()]) && pTemp.isInside(sizeX, sizeY)) {
//								p2 = pTemp;
//								found = true;
//								//GUI.log("\tFound");
//								// Measure next step
//								m2 = moveAndRead(p2);
//							}
//						}
//					if(!found) {
//						isMax = true;
//					}
				}
			} 
			
			/* Explore fase */
			GUI.log("Explore - Start");
			
			// Initial round. Initial radius = 3 to take into account Tumble
			round = 1;
			radius = 3;
			
			points = new PointSet();
			//p2 = p1.clone();
			
			// Measure until radius covers all the grid
			while(isMax && (radius < sizeX || radius < sizeY)) {
				/* Spiral */
				GUI.log("Explore - Round " + round);
				
				// Generate points for this round
				// -- Generate corner points
				pTemp = p1.clone().add(radius, radius); // Top-right
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = p1.clone().add(-radius, radius); // Top-left
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = p1.clone().add(radius, -radius); // Bottom-right
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = p1.clone().add(-radius, -radius); // Bottom-left
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				
				// -- Generate points (except corners)
				for(i = (-radius) + round; i < radius; i+= round) {
					pTemp = p1.clone().add(i, radius); // Top
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = p1.clone().add(i, -radius); // Bottom
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = p1.clone().add(-radius, i); // Left
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = p1.clone().add(radius, i); // Right
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				}
				
				//GUI.log(points.toString());
				
				
				// Iterate until all points have been visited or a new maximum is found
				while(!points.isEmpty() && isMax) {
					pts = points.iterator();
					
					// -- Get closest point
					Point pt, minPt;
					double dist, minDist;
					
					// ---- First element is temporary closest point
					minPt = pts.next();
					minDist = p2.distance(minPt);
					// ---- Iterate through all elements to find closest point
					while(pts.hasNext()) {
						pt = pts.next();
						dist = p2.distance(pt);
						//GUI	.log(pt.toString() + " > " + dist);
						if (dist < minDist) {
							minDist = dist;
							minPt = pt;
						}
					}
					
					// ---- Read closest point
					p2 = minPt;
					m2 = moveAndRead(p2);
					points.remove(p2);
					
					// ---- If the point is a new max, exit spiral and return to run & tumble
					if(m2 - m1 > PollutionParam.pThreshold) {
						isMax = false;
						// Set p1 to p2, keep both the same so algorithm goes to tumble on next step
						p1 = p2.clone();
						m1 = m2;
					}
					
				}
				
				round++;
				radius += round;
				
			}
			
		}
		
		GUI.log("Finished PdUC Algorithm");
		// TODO Go home
		
	}
	
}

package chemotaxis.logic;

import java.util.Iterator;

import api.*;
import api.pojo.FlightMode;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Location3D;
import api.pojo.location.Location3DUTM;
import chemotaxis.pojo.Point;
import chemotaxis.pojo.PointSet;
import chemotaxis.pojo.Value;
import main.api.ArduSimNotReadyException;
import main.api.Copter;
import main.api.GUI;
import main.api.MoveToListener;
import main.api.MoveTo;
import smile.data.SparseDataset;

public class ChemotaxisThread extends Thread {
	
	boolean [][] visited;
	
	int sizeX, sizeY;
	
	private Copter copter;
	private GUI gui;
	
	public ChemotaxisThread() {
		this.copter = API.getCopter(0);
		this.gui = API.getGUI(0);
	}
	// Move to a point within the grid
	void move(Point p) throws ArduSimNotReadyException {
		move(p.getX(), p.getY());
	}
	void move(int x, int y) throws ArduSimNotReadyException {
		Double xTarget = ChemotaxisParam.origin.x + (x * ChemotaxisParam.density);
		Double yTarget = ChemotaxisParam.origin.y + (y * ChemotaxisParam.density);
		
		MoveTo moveTo = copter.moveTo(new Location3D(xTarget, yTarget, ChemotaxisParam.altitude), new MoveToListener() {
			
			@Override
			public void onFailure() {
				// TODO tratar el error
			}
			
			@Override
			public void onCompleteActionPerformed() {
				// No necesario ya que esperamos a que el hilo termine
			}
		});
		moveTo.start();
		try {
			moveTo.join();
		} catch (InterruptedException e) {}
		
	}
	
	double moveAndRead(Point p) throws ArduSimNotReadyException {
		double m;
		move(p);
		m = ChemotaxisParam.sensor.read();
		synchronized(ChemotaxisParam.measurements_set) {
			ChemotaxisParam.measurements.set(p.getX(), p.getY(), m);
			ChemotaxisParam.measurements_temp.add(new Value(p.getX(), p.getY(), m));
		}
		visited[p.getX()][p.getY()] = true;
		gui.log("Read: [" + p.getX() + ", " + p.getY() + "] = " + m);
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
		sizeX = (int) ((double) ChemotaxisParam.width / ChemotaxisParam.density);
		sizeY = (int) ((double) ChemotaxisParam.length / ChemotaxisParam.density);
		
		// new booleans are initialized to false by default, this is what we want
		visited = new boolean[sizeX][sizeY];
		
		/* Wait until takeoff has finished */
		try {
			while(!ChemotaxisParam.ready) sleep(100);
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
		ChemotaxisParam.measurements = new SparseDataset();
		//data = new HashMap<Double, HashMap<Double, Double>>();
		
		/* Start Algorithm */
		gui.log("Start PdUC Algorithm");
		
		// Set initial measurement location to the grid centre
		p1 = new Point(sizeX / 2, sizeY / 2);
		
		// Initial p1 measurement
		try {
			m1 = moveAndRead(p1);
		} catch (ArduSimNotReadyException e) {
			this.exit(e);
			return;
		}
		
		// Initial p2 measurement
		p2 = new Point(p1);
		p2.addY(1);	
		try {
			m2 = moveAndRead(p2);
		} catch (ArduSimNotReadyException e) {
			this.exit(e);
			return;
		}
		
		boolean isMax = false;
		boolean found;
		//boolean finished = false;

		
		/* Main loop */
		while(!isMax) {
			while(!isMax) {
				/* Run & Tumble */
				
				// Chemotaxis + PSO
				if(m2 - m1 > ChemotaxisParam.pThreshold) {
					// Run
					gui.log("Chemotaxis: Run");
					pTemp = p1;
					p1 = new Point(p2);
					p2.add(p2.distVector(pTemp));
					
					if(p2.isInside(sizeX, sizeY)) {
						// Measure next step
						try {
							m2 = moveAndRead(p2);
						} catch (ArduSimNotReadyException e) {
							this.exit(e);
							return;
						}
					} else {
						p2 = new Point(p1);
						m2 = m1;
					}
					

					
				} else {
					// Tumble
					gui.log("Chemotaxis: Tumble");
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
						try {
							m2 = moveAndRead(p2);
						} catch (ArduSimNotReadyException e) {
							this.exit(e);
							return;
						}
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
			gui.log("Explore - Start");
			
			// Initial round. Initial radius = 3 to take into account Tumble
			round = 1;
			radius = 3;
			
			points = new PointSet();
			//p2 = new Point(p1);
			
			// Measure until radius covers all the grid
			while(isMax && (radius < sizeX || radius < sizeY)) {
				/* Spiral */
				gui.log("Explore - Round " + round);
				
				// Generate points for this round
				// -- Generate corner points
				pTemp = new Point(p1).add(radius, radius); // Top-right
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = new Point(p1).add(-radius, radius); // Top-left
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = new Point(p1).add(radius, -radius); // Bottom-right
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				pTemp = new Point(p1).add(-radius, -radius); // Bottom-left
				if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
				
				// -- Generate points (except corners)
				for(i = (-radius) + round; i < radius; i+= round) {
					pTemp = new Point(p1).add(i, radius); // Top
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = new Point(p1).add(i, -radius); // Bottom
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = new Point(p1).add(-radius, i); // Left
					if(pTemp.isInside(sizeX, sizeY) && !visited[pTemp.getX()][pTemp.getY()]) points.add(pTemp);
					pTemp = new Point(p1).add(radius, i); // Right
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
					try {
						m2 = moveAndRead(p2);
					} catch (ArduSimNotReadyException e) {
						this.exit(e);
						return;
					}
					points.remove(p2);
					
					// ---- If the point is a new max, exit spiral and return to run & tumble
					if(m2 - m1 > ChemotaxisParam.pThreshold) {
						isMax = false;
						// Set p1 to p2, keep both the same so algorithm goes to tumble on next step
						p1 = new Point(p2);
						m1 = m2;
					}
					
				}
				
				round++;
				radius += round;
				
			}
			
		}
		
		gui.log("Finished PdUC Algorithm");
		// TODO Go home
		
	}
	
	//Private method to return to land and exit from ArduSim
	private void exit(ArduSimNotReadyException e) {
		e.printStackTrace();
		if (copter.setFlightMode(FlightMode.RTL)) {//TODO gui.exit ya pasa a RTL si el dron es real (no hace falta esto)
			gui.log("Landing for being unable to calculate the target coordinates.");
		} else {
			gui.log("Unable to return to land.");
		}
		gui.exit(e.getMessage());
	}
	
}

package pollution;

import api.*;
import api.pojo.UTMCoordinates;
import smile.data.SparseDataset;

public class Pollution extends Thread {
	
	//VisitedMatrix sVisited;
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
	
	//HashMap<Double, HashMap<Double, Double>> data;
	/*
	void addData(double x, double y, double v) {
		HashMap<Double, Double> aux;
		aux = data.get(x);
		if (aux == null) {
			data.put(x, new HashMap<Double, Double>());
		}
		data.get(x).put(y, v);
	}
	*/
	
	enum stateType {
		SEARCH,
		EXPLORE,
		HOME
	}
	

	@Override
	public void run() {
		// TODO Implement PdUC
		//long startTime = System.currentTimeMillis();
		
		Point p1, p2;
		double m1, m2;
		
		
		//sVisited = new VisitedMatrix();
		
		// Calculate grid size and origin
		sizeX = (int) ((double) PollutionParam.width * PollutionParam.density);
		sizeY = (int) ((double) PollutionParam.length * PollutionParam.density);
		origin = api.Tools.geoToUTM(PollutionParam.startLocation.latitude, PollutionParam.startLocation.longitude);
		origin.Easting -= PollutionParam.width/2.0;
		origin.Northing -= PollutionParam.length/2.0;
		
		// new booleans are initialized to false by default, this is what we want
		visited = new boolean[sizeX][sizeY];
		
		/* Wait until takeoff has finished */
		try {
			while(!PollutionParam.ready) sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PollutionParam.measurements = new SparseDataset();
		//data = new HashMap<Double, HashMap<Double, Double>>();
		
		/* Start Algorithm */
		GUI.log("Start PdUC Algorithm");
		
		// Move to the grid centre
		p1 = new Point(sizeX / 2, sizeY / 2);
		move(p1);
		
		// Initial measurements
		m1 = PollutionParam.sensor.read();
		PollutionParam.measurements.set(p1.getX(), p1.getY(), m1);
		//sVisited.add(p1);
		visited[p1.getX()][p1.getY()] = true;
		
		p2 = p1.copy();
		p2.addY(1);		
		
		stateType state = stateType.SEARCH;
		boolean isMax = false;
		boolean found;
		Point pTemp;
		
		while (state != stateType.HOME) {
			switch (state) {
			case SEARCH:
				// Read Pollution
				move(p2);
				m2 = PollutionParam.sensor.read();
				PollutionParam.measurements.set(p2.getX(), p2.getY(), m2);
				visited[p2.getX()][p2.getY()] = true;
				
				// Chemotaxis + PSO
				if(m2 - m1 > PollutionParam.pThreshold) {
					// Run
					p1 = p2.copy();
					p2.add(p2.distVector(p1));
				} else {
					// Tumble
					found = false;
					for(int i = -1; i < 2 && !found; i++)
						for(int j = -1; j< 2 && !found; j++) {
							pTemp = new Point(p1.getX() + i, p1.getY() + j);
							if(!(visited[pTemp.getX()][pTemp.getY()] || pTemp.isInside(sizeX, sizeY))) {
								p2 = pTemp;
								found = true;
							}
						}
					if(!found) state = stateType.EXPLORE;
				}
			case EXPLORE:
				//TODO Calculate round path
				//TODO Calculate next step
				//TODO Move & read
				//TODO if isMax
				break;
			case HOME:
				//TODO Return to starting position
				break;
			}
		}
		
		//addData(point.getX(), point.getY(), measurement);
		//PollutionParam.measurements.set((int) point.getX(), (int) point.getY(), measurement);
		
	}
	
}

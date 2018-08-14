package lander.logic;


import api.Copter;
import api.Tools;
import lander.tools.Point;
import main.Param;


public class LanderMain extends Thread{
	
		
	@Override
	public void run() {
		// TODO Implement PdUC
		//long startTime = System.currentTimeMillis();
		
		
		/* Wait until takeoff has finished */
		try {
			while(!LanderParam.ready) sleep(100);
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	//Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);
	

	
	
	
	double moveAndRead(Point p) {
		double m;
		move(p);
		//m = LanderParam.sensor.read();
		//PollutionParam.measurements.set(p.getX(), p.getY(), m);
		//visited[p.getX()][p.getY()] = true;
		//GUI.log("Read: [" + p.getX() + ", " + p.getY() + "] = " + m);
		return m=15;
	}
	
	// Move to a point within the grid
	private void move(Point p) {
		move(p.getX(), p.getY());
	}

	private void move(int x, int y) {
			//Copter.moveUAV(0, Tools.UTMToGeo(origin.Easting + (x * PollutionParam.density), origin.Northing + (y * PollutionParam.density)), (float) PollutionParam.altitude, 1.0, 1.0);
			Copter.moveUAV(Tools.getNumUAVs()-1, LanderParam.LocationEnd, LanderParam.altitude, 20, 2);
	}
		

}

package Fake;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.FlightMode;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location3DGeo;
import main.api.Copter;
import main.api.GUI;
import main.api.TakeOff;
import main.api.TakeOffListener;
import vision.logic.visionParam;

public class FakeHelper extends ProtocolHelper{

	@Override
	public void setProtocol() {
		this.protocolString = "FAKE";
	}

	@Override
	public boolean loadMission() {return false;}

	@Override
	public JDialog openConfigurationDialog() {
		return null;
	}

	@Override
	public void initializeDataStructures() {
	}

	@Override
	public String setInitialState() {
		return null;
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startingLocations = new Pair[numUAVs];
		for(int id=0;id<numUAVs;id++) {
			// just a random location e.g. UPV campus vera
			startingLocations[id] = Pair.with(new Location2DGeo(39.725064, -0.733661),0.0);
		}
		return startingLocations;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;
	}

	@Override
	public void startThreads() {
	}

	@Override
	public void setupActionPerformed() {
		Copter copter = API.getCopter(0);
		TakeOff takeOff = copter.takeOff(5, new TakeOffListener() {
			
			@Override
			public void onFailure() {
				
			}
			
			@Override
			public void onCompleteActionPerformed() {
				// Waiting with Thread.join()
			}
		});
		takeOff.start();
		try {
			takeOff.join();
		} catch (InterruptedException e1) {}
	}

	@Override
	public void startExperimentActionPerformed() {
		double diff = 40;
		
		Copter copter = API.getCopter(0);
		copter.setFlightMode(FlightMode.GUIDED);
		double alt = copter.getAltitude();
		Location3DGeo loc = new Location3DGeo(copter.getLocationGeo(), alt + diff);
		Long time = System.currentTimeMillis();
		copter.moveTo(loc);
		boolean reached = false;
		do {
			if(Math.abs(copter.getAltitude() - (alt + diff)) < 1) {
				reached = true;
			}
			API.getArduSim().sleep(100);
		}while(!reached);
		System.out.println("time = " + (System.currentTimeMillis() - time)*2 );
		copter.setFlightMode(FlightMode.LAND);
		copter.land();
	}

	@Override
	public void forceExperimentEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getExperimentResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO Auto-generated method stub
		
	}

}

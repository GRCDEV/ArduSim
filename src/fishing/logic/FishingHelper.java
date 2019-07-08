package fishing.logic;

import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.location.Location2DGeo;
import api.pojo.location.Location2DUTM;
import api.pojo.location.Waypoint;
import fishing.gui.FishingConfigDialog;
import fishing.pojo.VectorMath;
import main.api.GUI;
import main.sim.board.BoardPanel;

public class FishingHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Fishing";
	}
	
	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public JDialog openConfigurationDialog() {
		GUI gui = API.getGUI(0);
		gui.log("Mostrando Ventana de Configuración");
		FishingConfigDialog dialog = new FishingConfigDialog();
		gui.log("Configuración Terminada");
		return dialog;
	}

	@Override
	public void initializeDataStructures() {
		// TODO 
	}

	@Override
	public String setInitialState() {
		// TODO 
		return null;
	}

	@Override
	public void rescaleDataStructures() {
		// TODO 
	}

	@Override
	public void loadResources() {
		// TODO 
	}
	
	@Override
	public void rescaleShownResources() {
		// TODO 
	}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {
		// TODO 
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		
		GUI gui = API.getGUI(0);
		
		int numUAVs = API.getArduSim().getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<Location2DGeo, Double>[] startCoordinatesArray = new Pair[numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		Location2DUTM p1UTM, p2UTM;
		double incX, incY;
		
		List<Waypoint> mission = FishingParam.boatMission[FishingParam.boatID];
		
		long id = API.getCopter(FishingParam.boatID).getID();
		if (mission != null) {
			waypointFound = false;
			for (int j=0; j<mission.size() && !waypointFound; j++) {
				waypoint1 = mission.get(j);
				if (waypoint1.getLatitude()!=0 || waypoint1.getLongitude()!=0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				gui.exit("No valid coordinates could be found to stablish the home of the UAV" + " " + id);
			}
			waypointFound = false;
			for (int j=waypoint1pos+1; j<mission.size() && !waypointFound; j++) {
				waypoint2 = mission.get(j);
				if (waypoint2.getLatitude()!=0 || waypoint2.getLongitude()!=0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				// We only can set a heading if at least two points with valid coordinates are found
				p1UTM = waypoint1.getUTM();
				p2UTM = waypoint2.getUTM();
				incX = p2UTM.x - p1UTM.x;
				incY = p2UTM.y - p1UTM.y;
				if (incX != 0 || incY != 0) {
					if (incX == 0) {
						if (incY > 0)	heading = 0.0;
						else			heading = 180.0;
					} else if (incY == 0) {
						if (incX > 0)	heading = 90;
						else			heading = 270.0;
					} else {
						double gamma = Math.atan(incY/incX);
						if (incX >0)	heading = 90 - gamma * 180 / Math.PI;
						else 			heading = 270.0 - gamma * 180 / Math.PI;
					}
				}
			}
		} else {
			// Assuming that all UAVs have a mission loaded
			gui.exit("ArduSim" + ": " + "Failed locating the home position of the UAV" + " " + id + ".");
		}
		
		
		FishingParam.startLocationBoat= new Location2DGeo(waypoint1.getLatitude(), waypoint1.getLongitude());
		FishingParam.startLocationUAV = new Location2DGeo(waypoint1.getLatitude(), waypoint1.getLongitude());
		startCoordinatesArray[0] = new Pair<Location2DGeo, Double>(FishingParam.startLocationBoat,heading);
		if (numUAVs>1) {
		startCoordinatesArray[1] = new Pair<Location2DGeo, Double>(FishingParam.startLocationUAV,heading);
		
		FishingParam.vOrigin = VectorMath.getUnitaryVector(FishingParam.vOrigin);
		FishingParam.vOrigin[0] *= FishingParam.radius;
		FishingParam.vOrigin[1] *= FishingParam.radius;
		gui.log("Vector Origen Inicial: ( " + FishingParam.vOrigin[0] + "," + FishingParam.vOrigin[1] + " )");
		}
		
		return startCoordinatesArray;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		/*GUI.log("Send initial Configuration for UAV:"+ numUAV);
		if (numUAV == UavFishingParam.boatID) {
			GUI.log("Sending Mission to UAV: " + numUAV );
			return Copter.cleanAndSendMissionToUAV(numUAV, Tools.getLoadedMissions()[numUAV]);
			
		}
		else*/
			return true;
	}

	@Override
	public void startThreads() {
		GUI gui = API.getGUI(0);
		
		gui.log("Creating uavFishing threads");
		new BoatThread(FishingParam.boatID).start();
		new FisherControllerThread(FishingParam.fisherID).start();
		new FisherReceiverThread(FishingParam.fisherID).start();
		gui.log("UavFishing threads created");
	}//TODO

	@Override
	public void setupActionPerformed() {

	}

	@Override
	public void startExperimentActionPerformed() {
		
		//Boat
		API.getCopter(FishingParam.boatID).getMissionHelper().start();
		//Copter
		FisherControllerThread.startExperiment = true;
		
	}//TODO

	@Override
	public void forceExperimentEnd() {
		// TODO 
	}

	@Override
	public String getExperimentResults() {
		// TODO 
		return null;
	}
	
	@Override
	public String getExperimentConfiguration() {
		// TODO 
		return null;
	}
	
	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO 
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO 
	}
	

}
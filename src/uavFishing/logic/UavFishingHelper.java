package uavFishing.logic;

import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import mbcap.logic.MBCAPText;
import sim.board.BoardPanel;
import uavFishing.gui.UavFishingConfigDialog;
import uavFishing.pojo.ToolsFishing;
import uavFishing.pojo.VectorMath;

public class UavFishingHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Fishing";
	}
	
	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		GUI.log("Mostrando Ventana de Configuración");
		UavFishingConfigDialog dialog = new UavFishingConfigDialog();
		GUI.log("Configuración Terminada");
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
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		
		int numUAVs = Tools.getNumUAVs();
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[numUAVs];
		double heading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		
		List<Waypoint> mission = UavFishingParam.boatMission[UavFishingParam.boatID];
		
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
				GUI.exit("No valid coordinates could be found to stablish the home of the UAV" + " " + Tools.getIdFromPos(UavFishingParam.boatID));
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
				p1UTM = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
				p2UTM = Tools.geoToUTM(waypoint2.getLatitude(), waypoint2.getLongitude());
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
			GUI.exit("ArduSim" + ": " + "Failed locating the home position of the UAV" + " " + Tools.getIdFromPos(UavFishingParam.boatID) + ".");
		}
		
		
		UavFishingParam.startLocationBoat= new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude());
		UavFishingParam.startLocationUAV = new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude());
		startCoordinatesArray[0] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationBoat,heading);
		if (numUAVs>1) {
		startCoordinatesArray[1] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationUAV,heading);
		
		UavFishingParam.vOrigin = VectorMath.getUnitaryVector(UavFishingParam.vOrigin);
		UavFishingParam.vOrigin[0] *= UavFishingParam.radius;
		UavFishingParam.vOrigin[1] *= UavFishingParam.radius;
		GUI.log("Vector Origen Inicial: ( " + UavFishingParam.vOrigin[0] + "," + UavFishingParam.vOrigin[1] + " )");
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
		GUI.log("Creating uavFishing threads");
		new BoatThread(UavFishingParam.boatID).start();
		new FisherControllerThread(UavFishingParam.fisherID).start();
		new FisherReceiverThread(UavFishingParam.fisherID).start();
		GUI.log("UavFishing threads created");
	}//TODO

	@Override
	public void setupActionPerformed() {

	}

	@Override
	public void startExperimentActionPerformed() {
		
		//Boat
		Copter.startMissionFromGround(UavFishingParam.boatID);
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
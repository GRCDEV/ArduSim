package uavFishing.logic;

import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.javatuples.Pair;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
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
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startCoordinatesArray = new Pair[2];
		startCoordinatesArray[0] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationBoat,0.0);
		startCoordinatesArray[1] = new Pair<GeoCoordinates, Double>(UavFishingParam.startLocationUAV,0.0);
		
		UavFishingParam.vOrigin = new double [2];
		UavFishingParam.vOrigin[0] = ToolsFishing.geoToUTM(UavFishingParam.startLocationUAV).x - ToolsFishing.geoToUTM(UavFishingParam.startLocationBoat).x;
		UavFishingParam.vOrigin[1] = ToolsFishing.geoToUTM(UavFishingParam.startLocationUAV).y - ToolsFishing.geoToUTM(UavFishingParam.startLocationBoat).y;
		
		UavFishingParam.vOrigin = VectorMath.getUnitaryVector(UavFishingParam.vOrigin);
		UavFishingParam.vOrigin[0] *= UavFishingParam.radius;
		UavFishingParam.vOrigin[1] *= UavFishingParam.radius;
		return startCoordinatesArray;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
	
		if (numUAV == UavFishingParam.boatID) return Copter.cleanAndSendMissionToUAV(numUAV, Tools.getLoadedMissions()[numUAV]);
		else return false;
	}

	@Override
	public void startThreads() {
		
		new FisherControllerThread(UavFishingParam.fisherID).start();
		new FisherReceiverThread(UavFishingParam.fisherID).start();
		new BoatThread(UavFishingParam.boatID).start();
		
	}//TODO

	@Override
	public void setupActionPerformed() {
//		Param.simStatus = SimulatorState.READY_FOR_TEST;//TODO Francisco ha comentado esto. Estab en el siguiente método y no sirve
	}

	@Override
	public void startExperimentActionPerformed() {
		
		//Barco
		Copter.setFlightMode(0, FlightMode.STABILIZE);
		Copter.armEngines(0);
		Copter.setFlightMode(0, FlightMode.AUTO);
		Copter.setHalfThrottle(0);
		//Dron
		Copter.setFlightMode(1, FlightMode.STABILIZE);
		Copter.armEngines(1);
		Copter.setFlightMode(1, FlightMode.GUIDED);
		Copter.guidedTakeOff(1, 25);
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
	public void logData(String folder, String baseFileName) {
		// TODO 
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO 
	}
	

}
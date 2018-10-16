package followme.logic;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.javatuples.Pair;

import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;
import followme.pojo.Nodo;
import followme.pojo.RecursoCompartido;
import main.Param;
import sim.board.BoardPanel;

public class FollowMeHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = FollowMeText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public void openConfigurationDialog() {
		FollowMeDialog ConfigDialog = new FollowMeDialog();
		ConfigDialog.setVisible(true);
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = Tools.getNumUAVs();
		FollowMeParam.uavs = new FollowMeState[numUAVs];
		for (int i = 0; i < numUAVs; i++) {
			FollowMeParam.uavs[i] = FollowMeState.START;
		}

		// Analyze which UAV is master
		int posMaster = -1;
		boolean realUAVisMaster = false;
		if (Tools.getArduSimRole() == Tools.MULTICOPTER) { //verificar
			long id = Tools.getIdFromPos(0);
			for (int i = 0; i < FollowMeParam.MASTER_ID_REAL.length && !realUAVisMaster; i++) {
				if (id == FollowMeParam.MASTER_ID_REAL[i]) {
					posMaster = i;
					realUAVisMaster = true;
				}
			}
		} else {
			for (int i = 0; i < numUAVs && posMaster == -1; i++) {
				if (Tools.getIdFromPos(i) == FollowMeParam.MASTER_ID_SIM) {
					posMaster = i;
				}
			}
			// TODO tratar error si no lo encuentra

		}
		FollowMeParam.posMaster = posMaster;
		FollowMeParam.realUAVisMaster = realUAVisMaster;
		FollowMeParam.posFormacion = new ConcurrentHashMap<Integer, Integer>();
	}

	@Override
	public String setInitialState() {
		return FollowMeParam.uavs[0].getName();
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
		Pair<GeoCoordinates, Double>[] iniLocation = new Pair[Tools.getNumUAVs()];

		GeoCoordinates geoMaster = new GeoCoordinates(39.482588, -0.345971);
		GeoCoordinates geoSlave = new GeoCoordinates(39.482111, -0.346857);
		iniLocation[0] = Pair.with(geoMaster, 0.0);
		iniLocation[1] = Pair.with(geoSlave, 0.0);
		GeoCoordinates geoSep = new GeoCoordinates(0, 0.00002);
		for (int i = 1; i < Tools.getNumUAVs() - 1; i++) {
			iniLocation[i + 1] = Pair.with(new GeoCoordinates(geoSlave.latitude, geoSlave.longitude), 0.0);
		}
		return iniLocation;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		return true;
	}

	@Override
	public void startThreads() {
		switch (Tools.getArduSimRole())  
		{
			case 0: // Tools.MULTICOPTER
				if (FollowMeParam.realUAVisMaster) {
				// MasterMando sendTh = new MasterMando();
				// sendTh.start();
				// SwarmHelper.log("Thread send start");
				// MasterThread masterTh = new MasterThread();
				// masterTh.start();
				} else {
				// FollowerThread followerTh = new FollowerThread(0);
				// followerTh.start();
				}
				break;
			case 1: //Tools.SIMULATOR 
				for (int i = 0; i < Tools.getNumUAVs(); i++) {
					if (i == FollowMeParam.posMaster) {
						MasterTalker masterTalker = new MasterTalker(i);
						MasterListener masterListener = new MasterListener(i);
						MasterMando masterMando = new MasterMando();
						GUI.log("Iniciando Master");
						masterTalker.start();
						masterListener.start();
						masterMando.start();
						} 
					else 
						{
						AtomicReference<Point2D.Double> point = new AtomicReference<Point2D.Double>(null);;
						SlaveTalker slaveTalker = new SlaveTalker(i,point);
						SlaveListener slaveListener = new SlaveListener(i,point);
						GUI.log("Iniciando Slave " + i);
						slaveTalker.start();
						slaveListener.start();
						}
					}
				break;
			}
	}

	@Override
	public void setupActionPerformed() {
		while (!FollowMeParam.setupFinished) {
			Tools.waiting(100);
		}
	}

	@Override
	public void startExperimentActionPerformed() {
		
	}

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
		GUI.log("Willian Final"+ Tools.getExperimentStartTime());
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO
	}

}

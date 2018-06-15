package followme.logic;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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
		// GUI.log("SwarmConfigurationDialog --> Mas tarde");
		// Cargar fichero de logs msg
		// GUI.log("Cargando Archivo");

		String SEPARATOR = ",";
		RecursoCompartido recurso = new RecursoCompartido();

		BufferedReader br = null;
		FileReader fr = null;

		try {

			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

			int result = fileChooser.showOpenDialog(new JDialog());

			File f = null;

			if (result == JFileChooser.APPROVE_OPTION) {
				// user selects a file
				f = fileChooser.getSelectedFile();

			}
			// System.out.println(f.getPath());
			fr = new FileReader(f);

			br = new BufferedReader(fr);
			String line = br.readLine();
			String[] fields = null;
			long time = 0;

			while (null != line) {
				fields = line.split(SEPARATOR);
				Nodo n = null;
				int tipo = Integer.parseInt(fields[0]);
				long tiempo = Long.parseLong(fields[1]);

				if (tipo == 0) {
					double east = Double.parseDouble(fields[2]);
					double north = Double.parseDouble(fields[3]);
					double z = Double.parseDouble(fields[4]);
					double zRel = Double.parseDouble(fields[5]);
					double speed = Double.parseDouble(fields[6]);
					double heading = Double.parseDouble(fields[7]);
					n = new Nodo(tipo, tiempo, east, north, z, zRel, speed, heading);
				} else if (tipo == 1) {
					int ch1 = Integer.parseInt(fields[2]);
					int ch2 = Integer.parseInt(fields[3]);
					int ch3 = Integer.parseInt(fields[4]);
					int ch4 = Integer.parseInt(fields[5]);
					n = new Nodo(tipo, tiempo, ch1, ch2, ch3, ch4);
				}

				recurso.put(n);
				long t = Long.parseLong(fields[1]);
				time = t;

				line = br.readLine();
			}
		} catch (Exception e) {
			// ...
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		FollowMeParam.recurso.set(headingInterpolar(recurso));

		Tools.setProtocolConfigured(true);
	}

	private static RecursoCompartido headingInterpolar(RecursoCompartido recurso) {
		RecursoCompartido rc = null;
		rc = recurso;
		rc.bubbleSort();

		ArrayList<Nodo> listaAux = new ArrayList<Nodo>();
		while (!rc.vacio) {
			Nodo aux = rc.pop();
			listaAux.add(aux);
		}
		// Copiar Heading de los primeros type 1 desde el primer type 0
		double headingInicial = 0;

		int i;
		for (i = 0; i < listaAux.size(); i++) {
			if (listaAux.get(i).type == 0) {
				headingInicial = listaAux.get(i).heading;
				break;
			}
		}
		for (int j = 0; j < i; j++) {
			listaAux.get(j).heading = headingInicial;
		}

		// Calcular heading para todos los type 1 que se encuentrean entre 2 type 0

		int x = 0, y = 0;
		while (listaAux.get(x).type != 0 && x < listaAux.size())
			x++;
		y = x + 1;
		while (y < listaAux.size()) {
			double headingX = 0.0, headingY = 0.0;
			long timeX = 0, timeY = 0;

			if (listaAux.get(x).type == 0) {
				timeX = listaAux.get(x).time;
				headingX = listaAux.get(x).heading;
			}

			while (y < listaAux.size() && listaAux.get(y).type != 0)
				y++;
			if (y >= listaAux.size())
				break;
			if (listaAux.get(y).type == 0) {
				timeY = listaAux.get(y).time;
				headingY = listaAux.get(y).heading;
			}

			double headingDif = headingY - headingX;
			long timeDif = timeY - timeX;

			while (x < y) {
				x++;
				listaAux.get(x).heading = headingX + ((listaAux.get(x).time - timeX) * (headingDif) / (timeDif));
			}
			x = y;
			y = x + 1;
		}
		for (Nodo nodo : listaAux) {
			rc.put(nodo);
		}

		return rc;
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
		if (Tools.isRealUAV()) {
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
		if (Tools.isRealUAV()) {
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
		} else {
			for (int i = 0; i < Tools.getNumUAVs(); i++) {
				if (i == FollowMeParam.posMaster) {
					MasterTalker masterTalker = new MasterTalker(i);
					MasterListener masterListener = new MasterListener(i);
					MasterMando masterMando = new MasterMando();
					GUI.log("Iniciando Master");
					masterTalker.start();
					masterListener.start();
					masterMando.start();

				} else {
					SlaveTalker slaveTalker = new SlaveTalker(i);
					SlaveListener slaveListener = new SlaveListener(i);
					GUI.log("Iniciando Slave " + i);
					slaveTalker.start();
					slaveListener.start();

				}
			}
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
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO
	}

}

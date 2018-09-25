package scanv2.logic;

import static scanv2.pojo.State.*;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import api.Copter;
import api.GUI;
import api.ProtocolHelper;
import api.Tools;
import api.pojo.AtomicDoubleArray;
import api.pojo.GeoCoordinates;
import api.pojo.UTMCoordinates;
import api.pojo.Waypoint;
import scanv2.gui.ScanConfigDialog;
import scanv2.pojo.Permutation;
import scanv2.pojo.uav2DPosition;
import sim.board.BoardPanel;

public class ScanHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = ScanText.PROTOCOL_TEXT;
	}

	@Override
	public boolean loadMission() {
		// Only the master UAV has a mission (on real UAV, numUAV == 0)
		boolean isMaster = ScanHelper.isMaster(0);
		if (isMaster) {
			ScanParam.idMaster = Tools.getIdFromPos(ScanParam.MASTER_POSITION);
		}//	The slave in a real UAV has SwarmProtParam.idMaster == null
		return isMaster;
	}

	@Override
	public void openConfigurationDialog() {
		ScanParam.idMaster = ScanParam.SIMULATION_MASTER_ID;

		new ScanConfigDialog();
	}

	@Override
	public void initializeDataStructures() {
		int numUAVs = Tools.getNumUAVs();
		
		ScanParam.idPrev = new AtomicLongArray(numUAVs);
		ScanParam.idNext = new AtomicLongArray(numUAVs);
		ScanParam.takeoffAltitude = new AtomicDoubleArray(numUAVs);
		ScanParam.data = new AtomicReferenceArray<>(numUAVs);
		ScanParam.uavMissionReceivedUTM = new AtomicReferenceArray<>(numUAVs);
		ScanParam.uavMissionReceivedGeo = new AtomicReferenceArray<>(numUAVs);
		
		ScanParam.state = new AtomicIntegerArray(numUAVs);
		ScanParam.moveSemaphore = new AtomicIntegerArray(numUAVs);
		ScanParam.wpReachedSemaphore = new AtomicIntegerArray(numUAVs);
	}

	@Override
	public String setInitialState() {
		return ScanText.START;
	}

	@Override
	public void rescaleDataStructures() {}

	@Override
	public void loadResources() {}

	@Override
	public void rescaleShownResources() {}

	@Override
	public void drawResources(Graphics2D g2, BoardPanel p) {}

	@Override
	public Pair<GeoCoordinates, Double>[] setStartingLocation() {
		/** Position the master on the map */
		ScanParam.masterHeading = 0.0;
		Waypoint waypoint1, waypoint2;
		waypoint1 = waypoint2 = null;
		int waypoint1pos = 0;
		boolean waypointFound;
		List<Waypoint>[] missions = Tools.getLoadedMissions();
		if (missions != null && missions[ScanParam.MASTER_POSITION] != null) {
			waypointFound = false;
			for (int j = 0; j < missions[ScanParam.MASTER_POSITION].size() && !waypointFound; j++) {
				waypoint1 = missions[ScanParam.MASTER_POSITION].get(j);
				if (waypoint1.getLatitude() != 0 || waypoint1.getLongitude() != 0) {
					waypoint1pos = j;
					waypointFound = true;
				}
			}
			if (!waypointFound) {
				GUI.exit(ScanText.UAVS_START_ERROR_2 + " " + Tools.getIdFromPos(ScanParam.MASTER_POSITION));
			}
			waypointFound = false;
			for (int j = waypoint1pos + 1; j < missions[ScanParam.MASTER_POSITION].size()
					&& !waypointFound; j++) {
				waypoint2 = missions[ScanParam.MASTER_POSITION].get(j);
				if (waypoint2.getLatitude() != 0 || waypoint2.getLongitude() != 0) {
					waypointFound = true;
				}
			}
			if (waypointFound) {
				ScanParam.masterHeading = ScanHelper.getMasterHeading(waypoint1, waypoint2);
			}
		} else {
			JOptionPane.showMessageDialog(null, ScanText.UAVS_START_ERROR_1 + " " + (ScanParam.MASTER_POSITION + 1) + ".",
					ScanText.FATAL_ERROR, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		int numUAVs = Tools.getNumUAVs();
		GeoCoordinates[] startingLocations = posSlaveFromMasterGeo(ScanParam.masterHeading, waypoint1, numUAVs - 1,
				ScanParam.initialDistanceBetweenUAV);
		@SuppressWarnings("unchecked")
		Pair<GeoCoordinates, Double>[] startingLocationsFinal = new Pair[numUAVs];
		int aux = 0;
		int destAux = 0;
		while (aux < ScanParam.MASTER_POSITION) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], ScanParam.masterHeading);
			aux++;
			destAux++;
		}

		startingLocationsFinal[destAux] = Pair
				.with(new GeoCoordinates(waypoint1.getLatitude(), waypoint1.getLongitude()), ScanParam.masterHeading);
		destAux++;
		while (aux < startingLocations.length) {
			startingLocationsFinal[destAux] = Pair.with(startingLocations[aux], ScanParam.masterHeading);
			aux++;
			destAux++;
		}

		return startingLocationsFinal;
	}

	/** Calculates the master heading on simulation. */
	private static double getMasterHeading(Waypoint wp1, Waypoint wp2) {
		// We only can set a heading if at least two points with valid coordinates are found
		UTMCoordinates p1UTM, p2UTM;
		double incX, incY;
		double heading = 0.0;
		p1UTM = Tools.geoToUTM(wp1.getLatitude(), wp1.getLongitude());
		p2UTM = Tools.geoToUTM(wp2.getLatitude(), wp2.getLongitude());
		incX = p2UTM.Easting - p1UTM.Easting;
		incY = p2UTM.Northing - p1UTM.Northing;
		if (incX != 0 || incY != 0) {
			if (incX == 0) {
				if (incY > 0)
					heading = 0.0;
				else
					heading = 180.0;
			} else if (incY == 0) {
				if (incX > 0)
					heading = 89.9;
				else
					heading = 270.0;
			} else {
				double gamma = Math.atan(incY / incX);
				if (incX > 0)
					heading = 90 - gamma * 180 / Math.PI;
				else
					heading = 270.0 - gamma * 180 / Math.PI;
			}
		}
		return heading;
	}

	/** Returns the locations of slave UAVs given the master location, in geographic coordinates, for simulation. */
	private static GeoCoordinates[] posSlaveFromMasterGeo(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] aux = posSlaveFromMasterUTM(heading, waypoint1, numOfUAVs, initialDistanceBetweenUAV);
		GeoCoordinates[] result = new GeoCoordinates[aux.length];
		for (int i = 0; i < aux.length; i++) {
			result[i] = Tools.UTMToGeo(aux[i].x, aux[i].y);
		}
		return result;
	}

	/** Returns the locations of slave UAVs given the master location, in UTM coordinates, for simulation. */
	private static Point2D.Double[] posSlaveFromMasterUTM(double heading, Waypoint waypoint1, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		UTMCoordinates initialPoint = Tools.geoToUTM(waypoint1.getLatitude(), waypoint1.getLongitude());
		return posSlaveFromMasterLinear(heading, new Point2D.Double(initialPoint.Easting, initialPoint.Northing),
				numOfUAVs, initialDistanceBetweenUAV);

	}

	/** Locates the slaves on the map according to master position, forming a straight line perpendicular to the master heading. */
	protected static Point2D.Double[] posSlaveFromMasterLinear(double heading, Point2D.Double initialPoint, int numOfUAVs,
			int initialDistanceBetweenUAV) {
		Point2D.Double[] startingLocations = new Point2D.Double[numOfUAVs];
		Point2D.Double destPointP = new Point2D.Double(initialPoint.x, initialPoint.y);
		Point2D.Double destPointI = new Point2D.Double(initialPoint.x, initialPoint.y);

		for (int i = 0; i < numOfUAVs; i++) {
			if (i % 2 == 0) {
				destPointP.x = destPointP.x
						+ initialDistanceBetweenUAV * Math.cos((heading * Math.PI) / 180)
						+ 0 * Math.sin((heading * Math.PI) / 180);
				destPointP.y = destPointP.y + 0 * Math.cos((heading * Math.PI) / 180)
				- initialDistanceBetweenUAV * Math.sin((heading * Math.PI) / 180);
				startingLocations[i] = new Point2D.Double(destPointP.x, destPointP.y);

			} else {
				destPointI.x = destPointI.x
						- (initialDistanceBetweenUAV * Math.cos((heading * Math.PI) / 180)
								+ 0 * Math.sin((heading * Math.PI) / 180));
				destPointI.y = destPointI.y - (0 * Math.cos((heading * Math.PI) / 180)
						- initialDistanceBetweenUAV * Math.sin((heading * Math.PI) / 180));
				startingLocations[i] = new Point2D.Double(destPointI.x, destPointI.y);
			}

		}
		return startingLocations;
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// Only the master UAV has a mission
		if (ScanHelper.isMaster(numUAV)) {
			return Copter.cleanAndSendMissionToUAV(numUAV, Tools.getLoadedMissions()[numUAV]);
		} else {
			return true;
		}
	}

	@Override
	public void startThreads() {
		int numUAVs = Tools.getNumUAVs();
		for (int i = 0; i < numUAVs; i++) {
			(new ListenerThread(i)).start();
			(new TalkerThread(i)).start();
		}
		GUI.log(ScanText.ENABLING);
	}

	@Override
	public void setupActionPerformed() {
		int numUAVs = Tools.getNumUAVs();
		boolean allFinished = false;
		while (!allFinished) {
			allFinished = true;
			for (int i = 0; i < numUAVs && allFinished; i++) {
				if (ScanParam.state.get(i) < SETUP_FINISHED) {
					allFinished = false;
				}
			}
			if (!allFinished) {
				Tools.waiting(ScanParam.STATE_CHANGE_TIMEOUT);
			}
		}
	}

	@Override
	public void startExperimentActionPerformed() {}

	@Override
	public void forceExperimentEnd() {}

	@Override
	public String getExperimentResults() {
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName) {}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		//TODO
	}

	/** Provides the best formation for the initial locations, given the target heading:
	 * <p>Long. Id of the UAV in the center of the formation.
	 * <p>Integer. Position of the master UAV in the formation.
	 * <p>uav2DPosition[]. Formation that better suits a safe takeoff.
	 * <p>Method only used by the master UAV. */
	public static Triplet<Long, Integer, uav2DPosition[]> getBestFormation(uav2DPosition[] currentLocations, double masterHeading) {

		int masterPosReo = 0;
		uav2DPosition[] bestFormation = null;
		double distanceOfBestFormation = Double.MAX_VALUE;

		// We look for the drone that behave like center drone
		for (int i = 0; i < currentLocations.length; i++) {
			// We obtain coordinates for the drones that surround the central.
			Point2D.Double[] formation = ScanHelper.posSlaveFromMasterLinear(masterHeading,
					currentLocations[i], currentLocations.length - 1, ScanParam.initialDistanceBetweenUAVreal);
			Point2D.Double[] flightFormationFinal = Arrays.copyOf(formation, formation.length + 1);

			// Add to the end of the array the one that is being viewed if it is the center
			flightFormationFinal[flightFormationFinal.length - 1] = currentLocations[i];

			// Copy of Ids
			Long[] ids = new Long[flightFormationFinal.length];
			for (int j = 0; j < currentLocations.length; j++) {
				ids[j] = currentLocations[j].id;
			}

			// Calculate all possible combinations of ids
			Permutation<Long> prueba = new Permutation<Long>(ids);
			long[] bestForCurrentCenter = null;
			double dOfCurrentCenter = Double.MAX_VALUE;
			int j = ScanHelper.factorial(ids.length);
			while (j > 0) {
				// Test of a permutation
				Long[] mix = prueba.next();
				double acum = 0;
				// Calculation of the sum of distances to the square of each drone on the ground
				// to the position that would correspond
				// to it in the air according to the current combination of ids
				for (int k = 0; k < currentLocations.length; k++) {
					uav2DPosition c = currentLocations[k];
					boolean found = false;
					int pos = -1;
					// Find the position "pos" in the air for current Ids combination
					for (int n = 0; n < mix.length && !found; n++) {

						if (mix[n] == c.id) {
							pos = n;
							found = true;
						}
					}
					// Adjustment of least squares. The distance is exaggerated to know which is
					// closest
					acum = acum + Math.pow(c.distance(flightFormationFinal[pos]), 2);
				}

				if (i == 1) {
					GUI.logVerbose(Arrays.toString(mix));
				}

				if (bestForCurrentCenter == null || acum < dOfCurrentCenter) {
					bestForCurrentCenter = new long[mix.length];
					for (int t = 0; t < mix.length; t++) {
						bestForCurrentCenter[t] = mix[t];
					}
					dOfCurrentCenter = acum;

				}
				j--;
			}

			if (bestFormation == null || dOfCurrentCenter < distanceOfBestFormation) {
				j = 0;
				bestFormation = new uav2DPosition[bestForCurrentCenter.length];
				while (j < bestForCurrentCenter.length) {
					bestFormation[j] = new uav2DPosition(flightFormationFinal[j].x, flightFormationFinal[j].y,
							bestForCurrentCenter[j], masterHeading);
					distanceOfBestFormation = dOfCurrentCenter;
					boolean found = false;
					for (int x = 0; x < bestForCurrentCenter.length; x++) {
						if (bestForCurrentCenter[j] == ScanParam.idMaster) {
							found = true;
						}
					}
					if (found) {
						masterPosReo = j;
					}
					j++;
				}

			}
		}

		// Calculus of the UAV in the center of the formation
		long centerGlobal = Long.MAX_VALUE;
		double globalError = Double.MAX_VALUE;
		for (int i = 0; i < bestFormation.length; i++) {
			double acum = 0;
			for (int j = 0; j < bestFormation.length; j++) {
				if (i != j) {
					acum += Math.pow(bestFormation[i].distance(bestFormation[j]), 2);
				}
			}
			if (acum < globalError) {
				centerGlobal = bestFormation[i].id;
			}
		}

		return Triplet.with(centerGlobal, masterPosReo, bestFormation);
	}

	/** Provides the factorial of a number. */
	public static int factorial(int n) {
		int resultado = 1;
		for (int i = 1; i <= n; i++) {
			resultado *= i;
		}
		return resultado;
	}
	
	/** Asserts if the UAV is master. */
	public static boolean isMaster(int numUAV) {
		boolean b = false;
		int role = Tools.getArduSimRole();
		if (role == Tools.MULTICOPTER) {
			/** You get the id = MAC for real drone */
			long idMaster = Tools.getIdFromPos(ScanParam.MASTER_POSITION);
			for (int i = 0; i < ScanParam.MAC_ID.length; i++) {
				if (ScanParam.MAC_ID[i] == idMaster) {
					return true;
				}
			}
		} else if (role == Tools.SIMULATOR) {
			if (numUAV == ScanParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}

}

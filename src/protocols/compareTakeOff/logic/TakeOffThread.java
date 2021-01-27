package protocols.compareTakeOff.logic;

import api.API;
import api.pojo.FlightMode;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import main.api.ArduSim;
import main.api.Copter;
import main.api.GUI;
import main.api.SafeTakeOffHelper;
import main.api.masterslavepattern.MasterSlaveHelper;
import main.api.masterslavepattern.safeTakeOff.SafeTakeOffContext;
import main.uavController.UAVParam;
import protocols.compareTakeOff.gui.CompareTakeOffSimProperties;
import protocols.compareTakeOff.pojo.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TakeOffThread extends Thread{

    private final ArduSim arduSim;
    private final GUI gui;
    private final Copter copter;
    private boolean isMaster;
    private String experimentOutput ="\n";
    private int numUAV;

    public TakeOffThread(int numUAV){
        this.numUAV = numUAV;
        this.arduSim = API.getArduSim();
        this.gui = API.getGUI(numUAV);
        this.copter = API.getCopter(numUAV);
        experimentOutput += arduSim.getNumUAVs() + ";" + CompareTakeOffSimProperties.groundFormation.getName() + ";"
                + CompareTakeOffSimProperties.flyingFormation.getName() + ";" +
                CompareTakeOffSimProperties.takeOffStrategy.getName() + ";" + CompareTakeOffSimProperties.altitude + ";";
    }

    @Override
    public void run(){
        while (!arduSim.isAvailable()) {arduSim.sleep(CompareTakeOffSimProperties.timeout);}
        long startFullTakeoff = System.currentTimeMillis();
		// discover and take of the UAVS
		Map<Long, Location2DUTM> UAVsDetected = setup();
		while(!arduSim.isExperimentInProgress()) { arduSim.sleep(CompareTakeOffSimProperties.timeout); }
		takeOff(UAVsDetected);
		experimentOutput += System.currentTimeMillis() - startFullTakeoff + ";";
		land();
		if(isMaster){
            try {
                File f = new File(CompareTakeOffSimProperties.outputFile);
                boolean writeHeader = !f.exists();
                FileWriter fr = new FileWriter(f, true);
                if(writeHeader){
                    fr.write("numUAVs;GroundFormation;AirFormation;TakeoffStrategy;altitude;setupTime;" +
                            "calculationTime;totalDistance;takeoffTime;numberCollisions;fullTime");
                }
                fr.write(experimentOutput);
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private Map<Long, Location2DUTM> setup() {
        long startSetup = System.currentTimeMillis();
        // DISCOVER MASTER AND SLAVES
		gui.logUAV(Text.START);
		// Let the master detect slaves until the setup button is pressed
		Map<Long, Location2DUTM> UAVsDetected = null;
		MasterSlaveHelper msHelper = copter.getMasterSlaveHelper();
		isMaster = msHelper.isMaster();
		if(isMaster) {
			final AtomicInteger totalDetected = new AtomicInteger();
			UAVsDetected = msHelper.DiscoverSlaves(numUAVs -> {
				// Just for logging purposes
				if(numUAVs > totalDetected.get()) {
					totalDetected.set(numUAVs);
					gui.log(Text.MASTER_DETECTED_UAVS + numUAVs);
				}
				//We decide to continue when all UAVs are discovered
				return numUAVs == arduSim.getNumUAVs()-1;
			});
		}else {
			msHelper.DiscoverMaster();
		}
		experimentOutput += System.currentTimeMillis() - startSetup + ";";
		return UAVsDetected;
    }

    private void takeOff(Map<Long, Location2DUTM> UAVsDetected) {
        long startCalculateFit = System.currentTimeMillis();
        // TAKE OFF PHASE
        gui.logUAV(protocols.shakeup.pojo.Text.START_TAKE_OFF);
        gui.updateProtocolState(protocols.shakeup.pojo.Text.TAKE_OFF);
        // 1. Synchronize master with slaves to get the takeoff sequence in the take off context object
        SafeTakeOffContext takeOff;
        SafeTakeOffHelper takeOffHelper = copter.getSafeTakeOffHelper();
        if(isMaster) {
            double formationYaw;
            if(arduSim.getArduSimRole() == ArduSim.MULTICOPTER) {
                formationYaw = copter.getHeading();
            }else {
                formationYaw = CompareTakeOffSimProperties.masterInitialYaw;
            }
            takeOff = takeOffHelper.getMasterContext(UAVsDetected,
                    API.getFlightFormationTools().getFlyingFormation(UAVsDetected.size() + 1),
                    formationYaw, CompareTakeOffSimProperties.altitude, false, false);
            experimentOutput += System.currentTimeMillis() - startCalculateFit + ";";
        }else {
            takeOff = takeOffHelper.getSlaveContext(false);
        }
        experimentOutput += String.format("%.4f",takeOffHelper.getTotalDistance()) + ";";
        long startTakeoff = System.currentTimeMillis();
        // 2. Take off all the UAVs
        takeOffHelper.start(takeOff, CompareTakeOffHelper.isTakeOffFinished::incrementAndGet);

        double minDist = Double.MAX_VALUE;
        Set<Map.Entry<Integer,Integer>> collisionList= new HashSet<>();
        while (CompareTakeOffHelper.isTakeOffFinished.get() != main.Param.numUAVs) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < main.Param.numUAVs - 1; i++) {
                for (int j = i + 1; j < main.Param.numUAVs; j++) {
                    Location3DUTM loc1 = new Location3DUTM(UAVParam.uavCurrentData[i].getUTMLocation(), UAVParam.uavCurrentData[i].getZ());
                    Location3DUTM loc2 = new Location3DUTM(UAVParam.uavCurrentData[j].getUTMLocation(), UAVParam.uavCurrentData[j].getZ());
                    double distance = loc1.distance3D(loc2);
                    if(distance < minDist) {minDist = distance;}
                    if(distance < 4.9) {
                        java.util.Map.Entry<Integer,Integer> pair=new java.util.AbstractMap.SimpleEntry<>(i,j);
                        collisionList.add(pair);
                    }
                }
            }
            long timeDiff = System.currentTimeMillis() - start;
            if(timeDiff < CompareTakeOffSimProperties.timeout){
                arduSim.sleep(CompareTakeOffSimProperties.timeout - timeDiff);
            }
        }
        experimentOutput += System.currentTimeMillis() - startTakeoff + ";" + collisionList.size() + ";";
    }

    private void land() {
        gui.logUAV(Text.LAND);
        copter.setFlightMode(FlightMode.LAND);
        copter.land();
    }
}

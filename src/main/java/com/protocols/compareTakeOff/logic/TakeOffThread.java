package com.protocols.compareTakeOff.logic;
import com.api.API;
import com.api.pojo.FlightMode;
import com.api.swarm.Swarm;
import com.api.swarm.discovery.BasicDiscover;
import com.api.swarm.takeoff.TakeoffAlgorithm;
import com.api.ArduSim;
import com.api.copter.Copter;
import com.protocols.compareTakeOff.gui.CompareTakeOffSimProperties;

public class TakeOffThread extends Thread{

    private final ArduSim arduSim;
    private final Copter copter;
    private final int numUAV;
    private Swarm swarm;
    private boolean setupDone=false;

    public TakeOffThread(int numUAV){
        this.numUAV = numUAV;
        this.arduSim = API.getArduSim();
        this.copter = API.getCopter(numUAV);
    }

    @Override
    public void run(){
        while (!arduSim.isSetupInProgress()) {arduSim.sleep(CompareTakeOffSimProperties.timeout);}
		setup();
        setupDone = true;
        while (!arduSim.isExperimentInProgress()){arduSim.sleep(CompareTakeOffSimProperties.timeout);}
        takeOff();
        land();
    }

    public boolean isSetupDone(){
        return setupDone;
    }

    private void setup() {
		swarm =  new Swarm.Builder(copter.getID())
                .discover(new BasicDiscover(numUAV))
                .assignmentAlgorithm(CompareTakeOffSimProperties.assignmentAlgorithm)
                .airFormationLayout(CompareTakeOffSimProperties.flyingFormation.getLayout(),CompareTakeOffSimProperties.flyingMinDistance)
                .takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms.SIMULTANEOUS,CompareTakeOffSimProperties.altitude)
                .build();
    }

    public void takeOff() {
        swarm.takeOff(numUAV);
    }

    private void land() {
        copter.setFlightMode(FlightMode.LAND);
        copter.land();
    }
}

package com.protocols.muscop.logic;

import com.api.*;
import com.api.communications.HighlevelCommLink;
import com.api.copter.Copter;
import com.api.MissionHelper;
import com.api.copter.MoveToListener;
import com.api.pojo.location.Waypoint;
import com.api.swarm.Swarm;
import com.api.swarm.discovery.BasicDiscover;
import com.api.swarm.formations.Formation;
import com.api.swarm.takeoff.TakeoffAlgorithm;
import com.protocols.muscop.gui.MuscopSimProperties;

import static com.protocols.muscop.logic.Message.*;

import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class DroneThread extends Thread{

    private final int numUAV;

    private boolean setupDone = false;
    private final ArduSim arduSim;
    private final GUI gui;
    private final HighlevelCommLink commLink;
    private final AtomicInteger wpReachedSemaphore = new AtomicInteger(0);

    private ArrayList<Location3DUTM> missionWaypoints = new ArrayList<>();
    private int numUAVs;
    private Swarm swarm;
    private Map<Integer,Long> timeStampsUAVs;
    private final long TTL = 5000;

    private enum State {
        SETUP, OBTAIN_WAYPOINTS, TAKING_OFF,GO_TO_WP,lANDING,FINISHED
    }

    public DroneThread(int numUAV){
        this.numUAV = numUAV;
        this.gui = API.getGUI(numUAV);
        this.arduSim = new ArduSim();
        this.commLink = new HighlevelCommLink(numUAV);
    }

    @Override
    public void run() {
        setup();
        takeOff();
        fly();
        land();
    }

    private void setup() {
        while (!arduSim.isSetupInProgress()) {arduSim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);}
        buildSwarm();
        numUAVs = swarm.getIDs().size();
        obtainWaypoints();
        setupDone = true;
        while (!arduSim.isExperimentInProgress()){arduSim.sleep(MuscopSimProperties.STATE_CHANGE_TIMEOUT);}
    }

    private void buildSwarm() {
        gui.updateProtocolState(State.SETUP.name());
        swarm =  new Swarm.Builder(numUAV)
                .discover(new BasicDiscover(numUAV))
                .assignmentAlgorithm(MuscopSimProperties.assignmentAlgorithm)
                .airFormationLayout(MuscopSimProperties.flyingFormation.getLayout(),10)
                .takeOffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms.SIMULTANEOUS,MuscopSimProperties.altitude)
                .build();
    }

    private void obtainWaypoints() {
        gui.updateProtocolState(State.OBTAIN_WAYPOINTS.name());
        if(swarm.isMaster(numUAV)){
            obtainWaypointsMaster();
        }else{
            obtainMissionDataSlave();
        }
    }

    private void obtainWaypointsMaster() {
        missionWaypoints = getWaypointsFromMissionHelper();
        Set<Integer> slaveIds = swarm.getIDs();
        slaveIds.remove(numUAV);
        commLink.sendJSONUntilACKsReceived(missionData(numUAV,numUAVs, missionWaypoints),slaveIds);
    }

    private ArrayList<Location3DUTM> getWaypointsFromMissionHelper() {
        ArrayList<Location3DUTM> waypoints = new ArrayList<>();
        MissionHelper missionHelper = API.getCopter(numUAV).getMissionHelper();
        List<Waypoint>[] missions = missionHelper.getMissionsLoaded();
        for (int wp_index =1;wp_index<missions[0].size();wp_index++) {
            Waypoint wp = missions[0].get(wp_index);
            waypoints.add(new Location3DUTM(wp.getUTM(),MuscopSimProperties.altitude));
        }
        return waypoints;
    }

    private void obtainMissionDataSlave() {
        while(missionWaypoints.size() == 0){
            JSONObject msg = commLink.receiveMessageReplyACK(missionData(),5);
            if(msg!=null) {
                processMissionDataMsg(msg);
            }
        }
    }

    private void processMissionDataMsg(JSONObject msg) {
        numUAVs = msg.getInt("numUAVs");
        JSONObject waypoints = msg.getJSONObject("waypoints");
        Formation f = swarm.getAirFormation();
        for(String wpId: waypoints.keySet()){
            JSONObject wp = waypoints.getJSONObject(wpId);
            double x = wp.getDouble("x");
            double y = wp.getDouble("y");
            double z = wp.getDouble("z");
            Location3DUTM centerLoc = new Location3DUTM(x,y,z);
            Location3DUTM locInSwarm = f.get3DUTMLocation(centerLoc,numUAV);
            missionWaypoints.add(locInSwarm);
        }
    }

    private void takeOff() {
        gui.updateProtocolState(State.TAKING_OFF.name());
        swarm.takeOff(numUAV);
    }

    private void fly() {
        initTimeStamps();
        while(wpReachedSemaphore.get() < missionWaypoints.size()-1) {
            //if(wpReachedSemaphore.get() == 1 && numUAV == 1){ break;}
            waypointReached();
            goToWaypoint();
        }
    }

    private void initTimeStamps() {
        timeStampsUAVs = new HashMap<>();
        for(int i=0;i<numUAVs;i++){
            if(i==numUAV){continue;}
            timeStampsUAVs.put(i,System.currentTimeMillis());
        }
    }

    private Set<Integer> checkTimeStamps() {
        Set<Integer> uavsToBeRemoved = new HashSet<>();
        for(Map.Entry<Integer,Long> e:timeStampsUAVs.entrySet()){
            if(e.getValue() < System.currentTimeMillis() - TTL){
                uavsToBeRemoved.add(e.getKey());
                gui.logUAV("UAV with ID " + e.getKey() + " has died");
            }
        }
        return uavsToBeRemoved;
    }

    private void goToWaypoint() {
        int wpReached = wpReachedSemaphore.get();
        gui.updateProtocolState(State.GO_TO_WP.name());

        Location3D wp = getNextWp(wpReached);
        Thread t = move(wp);

        while(wpReached == wpReachedSemaphore.get()){
            if(swarm.isMaster(numUAV)){
                commLink.sendJSON(moveToWaypoint(numUAV, wpReached+1));
            }else{
                commLink.sendJSON(Message.waypointReached(numUAV, wpReached));
            }
            read10Messages();
        }
        join(t);
    }

    private void join(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Location3D getNextWp(int currentWp) {
        int nextWp = currentWp +1;
        Location3D wp = null;
        try {
            wp = new Location3D(missionWaypoints.get(nextWp));
        } catch (LocationNotReadyException e) {
            e.printStackTrace();
        }
        return wp;
    }

    private Thread move(Location3D wp) {
        Thread t = API.getCopter(numUAV).moveTo(wp, new MoveToListener() {
            @Override
            public void onCompleteActionPerformed() {
                wpReachedSemaphore.incrementAndGet();
            }

            @Override
            public void onFailure() {

            }
        });
        t.start();
        return t;
    }

    private void read10Messages() {
        for(int i = 0; i< 10; i++) {
            JSONObject msg = commLink.receiveMessage();
            if (msg != null) {
                updateTimeStamp(msg);
            }
        }
    }

    private void waypointReached(){
        gui.updateProtocolState("reached wp");
        if(swarm.isMaster(numUAV)){
            waitForWPReachedMsgs();
        }else{
            sendWpReachedUntilMoveToWaypointReceived();
        }
    }

    private void sendWpReachedUntilMoveToWaypointReceived() {
        int wpReached = wpReachedSemaphore.get();
        boolean receivedMoveToMSG = false;
        while(!receivedMoveToMSG) {
            commLink.sendJSON(Message.waypointReached(numUAV, wpReached));
            JSONObject msg = commLink.receiveMessage(moveToWaypoint(wpReached+1));
            if(msg != null){
                updateTimeStamp(msg);
                receivedMoveToMSG = true;
            }
        }
    }

    private void waitForWPReachedMsgs() {
        int wpReached = wpReachedSemaphore.get();
        Set<Integer> receivedWPReached = new HashSet<>();
        while(!receivedWPReached.equals(timeStampsUAVs.keySet())) {
            JSONObject msg = commLink.receiveMessage(Message.waypointReached(wpReached));
            if (msg != null) {
                updateTimeStamp(msg);
                int senderID = msg.getInt(HighlevelCommLink.Keywords.SENDERID);
                receivedWPReached.add(senderID);
            }
        }
    }

    private void land() {
        gui.updateProtocolState(State.lANDING.name());
        Copter copter = API.getCopter(numUAV);
        copter.land();
        while (copter.isFlying()) {
            arduSim.sleep(500);
        }
        gui.updateProtocolState(State.FINISHED.name());
    }

    private void updateTimeStamp(JSONObject msg) {
        int senderID = msg.getInt(HighlevelCommLink.Keywords.SENDERID);
        timeStampsUAVs.put(senderID, System.currentTimeMillis());
        updateSwarm();
    }

    private void updateSwarm() {
        Set<Integer> uavsToBeRemoved = checkTimeStamps();
        removeFailedUAVs(uavsToBeRemoved);
    }

    private void removeFailedUAVs(Set<Integer> uavsToBeRemoved) {
        //TODO fix failed master once master order is known in the
        for(Integer uav: uavsToBeRemoved){
            timeStampsUAVs.remove(uav);
            numUAVs--;
        }
    }

    public boolean isSetupDone() {
        return setupDone;
    }
}

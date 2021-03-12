package com.api.masterslavepattern.safeTakeOff;

import com.api.API;
import com.api.ArduSimTools;
import com.api.Copter;
import com.api.GUI;
import com.api.communications.CommLink;
import com.api.formations.Formation;
import com.api.masterslavepattern.MSMessageID;
import com.api.masterslavepattern.MSParam;
import com.api.masterslavepattern.MSText;
import com.esotericsoftware.kryo.io.Input;
import com.setup.Text;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DUTM;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TakeOffMasterDataListenerThread extends Thread{

    public static volatile TakeOffAlgorithm selectedAlgorithm = TakeOffAlgorithm.SIMPLIFIED;
    private final long selfID;
    private final Map<Long, Location2DUTM> groundLocations;
    private int numSlaves;
    private final int numUAVs;
    private final Formation flightFormation;
    private final double formationYaw;
    private final double targetAltitude;
    private final boolean isCenterUAV;
    private final boolean exclude;
    private final AtomicReference<SafeTakeOffContext> result;
    private final CommLink commLink;
    private byte[] inBuffer;
    private final Input input;
    private final GUI gui;
    private final Copter copter;
    private double totalErrorInMatch = -1;

    private final AtomicInteger state = new AtomicInteger(MSParam.STATE_EXCLUDE);
    // TODO find right datastructure for ackMap
    // the boolean doesn't have a real meaning but a list allows for duplicates and for a set you cannot get a value at index n
    private volatile Map<Long,Boolean> ackMap = new HashMap<>((int)Math.ceil(numSlaves/0.75) + 1);

    public TakeOffMasterDataListenerThread(int numUAV, Map<Long, Location2DUTM> groundLocations,
                                           Formation flightFormation, double formationYaw, double targetAltitude, boolean isCenterUAV,
                                           boolean exclude, AtomicReference<SafeTakeOffContext> result){

        super(Text.SAFE_TAKE_OFF_MASTER_CONTEXT_LISTENER + numUAV);
        this.copter = API.getCopter(numUAV);
        this.selfID = this.copter.getID();
        this.groundLocations = groundLocations;
        this.numSlaves = groundLocations.size();
        this.numUAVs = this.numSlaves + 1;
        this.flightFormation = flightFormation;
        this.formationYaw = formationYaw;
        this.targetAltitude = targetAltitude;
        this.isCenterUAV = isCenterUAV;
        this.exclude = exclude;
        this.result = result;
        this.commLink = CommLink.getCommLink(numUAV,UAVParam.internalBroadcastPort);
        this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        this.input = new Input(inBuffer);
        this.gui = API.getGUI(numUAV);
    }

    @Override
    public void run(){
        ArduSimTools.logGlobal(Text.TAKEOFF_ALGORITHM_IN_USE + " " + TakeOffMasterDataListenerThread.selectedAlgorithm.getName());
        TakeOffMasterDataTalkerThread talker = new TakeOffMasterDataTalkerThread(this,numUAVs,selfID);
        talker.start();

        waitForSlavesExcludes(state);
        setTakeOffData(talker);
        state.set(MSParam.STATE_SENDING_TAKEOFFDATA);
        waitForSlavesTakeOffDataAck(state);
        waitForSlavesLeaderOrderAck(state);
    }

    private void waitForSlavesLeaderOrderAck(AtomicInteger state) {
        gui.logVerboseUAV("Master listener waiting slaves ack after receiving the leader order");
        while(state.get() == MSParam.STATE_SENDING_LIDERORDER){
            inBuffer = commLink.receiveMessage();
            if(inBuffer != null){
                input.setBuffer(inBuffer);
                short type = input.readShort();
                if(type == MSMessageID.LIDER_ORDER_ACK){
                    long idSlave = input.readLong();
                    ackMap.put(idSlave,true);
                    if(ackMap.size() == numSlaves){
                        gui.logVerbose("all UAVs sent there leader ack message");
                        state.set(MSParam.STATE_SENDING_FINISHED);
                        ackMap.clear();
                    }
                }
            }
        }
    }

    private void waitForSlavesTakeOffDataAck(AtomicInteger state) {
        gui.logVerboseUAV(MSText.MASTER_LISTENER_WAITING_DATA_ACK);
        while(state.get() == MSParam.STATE_SENDING_TAKEOFFDATA){
            inBuffer = commLink.receiveMessage();
            if (inBuffer != null) {
                input.setBuffer(inBuffer);
                short type = input.readShort();
                if(type == MSMessageID.TAKE_OFF_DATA_ACK){
                    long idSlave = input.readLong();
                    ackMap.put(idSlave,true);
                    if(ackMap.size() == numSlaves){
                        gui.logVerbose("all UAVs sent there takeOffdata ack messages");
                        ackMap.clear();
                        state.set(MSParam.STATE_SENDING_LIDERORDER);
                    }
                }
            }
        }
    }

    private void setTakeOffData(TakeOffMasterDataTalkerThread talker) {
        gui.logVerboseUAV(MSText.MASTER_LISTENER_CALCULATING_TAKE_OFF);
        Quartet<Integer, Long, Location2DUTM, Double>[] match = getMatch();

        // Get the target location and ID of the UAV that should be in the center of the formation
        Quartet<Integer, Long, Location2DUTM, Double> centerMatch = getCenter(match);
        long centerUAVID = centerMatch.getValue1();
        Location2DUTM centerUAVLocationUTM = centerMatch.getValue2();

        long[] ids = getmasterAssignment(match);
        talker.setDataParameters(match,centerUAVID,centerUAVLocationUTM,ids,targetAltitude,
                flightFormation,formationYaw,exclude);
    }

    private void waitForSlavesExcludes(AtomicInteger state) {
        gui.logVerboseUAV(MSText.MASTER_LISTENER_WAITING_EXCLUDES);
        while (state.get() == MSParam.STATE_EXCLUDE) {
            inBuffer = commLink.receiveMessage();
            if (inBuffer != null) {
                input.setBuffer(inBuffer);
                short type = input.readShort();

                if (type == MSMessageID.EXCLUDE) {
                    long idSlave = input.readLong();
                    boolean exclude = input.readBoolean();
                    ackMap.put(idSlave,exclude);
                    if (ackMap.size() == numSlaves) {
                        gui.logVerbose("all UAVs have sent there exclude message");
                        ackMap.clear();
                        break;
                    }
                }
            }
        }
    }

    private Quartet<Integer, Long, Location2DUTM, Double>[] getMatch() {
        // 2. Get the best match between current ground locations and flight locations
        // First, we add the master UAV
        groundLocations.put(selfID, copter.getLocationUTM());
        Long masterID = null;
        if (isCenterUAV) {
            masterID = selfID;
        }
        // Then, we get the match between ground and air locations
        MatchCalculusThread calculus = new MatchCalculusThread(groundLocations, flightFormation, formationYaw, masterID);
        calculus.start();
        // It waits the end of the calculus discarding received messages from slaves
        while (calculus.isAlive()) {
            commLink.receiveMessage(50);
        }
        Quartet<Integer, Long, Location2DUTM, Double>[] match = calculus.getResult();
        totalErrorInMatch = calculateTotalError(match);
        return match;
    }

    private long[] getmasterAssignment(Quartet<Integer, Long, Location2DUTM, Double>[] match) {
        // Get the master assignment order from the match (fault tolerance)
        long[] ids = new long[numUAVs];
        for (int i = 0, j = numUAVs-1; i < numUAVs; i++, j--) {
            ids[i] = match[j].getValue1();
        }
        return ids;
    }

    private Quartet<Integer, Long, Location2DUTM, Double> getCenter(Quartet<Integer, Long, Location2DUTM, Double>[] match) {
        int centerUAVAirPos = flightFormation.getCenterIndex();
        Quartet<Integer, Long, Location2DUTM, Double> centerMatch = null;
        for (int i = 0; i < numUAVs && centerMatch == null; i++) {
            if (match[i].getValue0() == centerUAVAirPos) {
                centerMatch = match[i];
            }
        }
        if (centerMatch == null) {
            gui.exit(MSText.CENTER_ID_NOT_FOUND);
        }
        return centerMatch;
    }

    private double calculateTotalError(Quartet<Integer, Long, Location2DUTM, Double>[] match) {
        double totalError = 0;
        for(Quartet<Integer,Long,Location2DUTM,Double> q : match){
            totalError += q.getValue3();
        }
        return  Math.sqrt(totalError);
    }

    public static Pair<Long, Location2DUTM> getCenterUAV(Map<Long, Location2DUTM> groundLocations, double formationYaw,
                      boolean isCenterUAV) {
        // 1. Get the best match between current ground locations and flight locations
        Long masterID = null;
        if (isCenterUAV) {
            masterID = API.getCopter(0).getID();
        }
        int numUAVs = groundLocations.size();
        Formation flightFormation = UAVParam.airFormation.get();
        MatchCalculusThread calculus = new MatchCalculusThread(groundLocations, flightFormation, formationYaw, masterID);
        calculus.start();
        try {
            calculus.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Quartet<Integer, Long, Location2DUTM, Double>[] match = calculus.getResult();

        // 2. Get the target location of the UAV that should be in the center of the formation
        int centerUAVAirPos = flightFormation.getCenterIndex();
        Quartet<Integer, Long, Location2DUTM, Double> centerMatch = null;
        for (int i = 0; i < numUAVs && centerMatch == null; i++) {
            if (match[i].getValue0() == centerUAVAirPos) {
                centerMatch = match[i];
            }
        }
        if (centerMatch == null) {
            API.getGUI(0).exit(MSText.CENTER_ID_NOT_FOUND);
        }

        return Pair.with(centerMatch.getValue1(), centerMatch.getValue2());
    }

    public double getTotalErrorInMatch(){
        return totalErrorInMatch;
    }

    public synchronized int getDataState(){
        return state.get();
    }

    public synchronized Map<Long,Boolean> getAckSet(){
        return ackMap;
    }

    public synchronized void setResult(SafeTakeOffContext data) {
        this.result.set(data);
    }
}

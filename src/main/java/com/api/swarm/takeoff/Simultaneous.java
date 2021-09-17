package com.api.swarm.takeoff;

import com.api.*;
import com.api.communications.HighlevelCommLink;
import com.api.copter.*;
import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class Simultaneous extends TakeoffAlgorithm {

    private final boolean isMaster;
    private int masterId;
    private HighlevelCommLink commLink;
    private Copter copter;

    public Simultaneous(Map<Long, Location3DUTM> assignment) {
        this.assignment = assignment;
        this.isMaster = assignment != null;
    }

    @Override
    public void takeOff(int numUAV) {
        this.commLink = new HighlevelCommLink(numUAV);
        this.numUAV = numUAV;
        this.copter = API.getCopter(numUAV);
        if(isMaster){this.masterId = numUAV;}
        GUI gui = API.getGUI(numUAV);

        gui.updateProtocolState("Take off");
        obtainTargetLocation();
        gui.logVerbose("UAV:" + numUAV + "\t target location: " + targetLocation);

        moveToTarget();
        waitUntilEverybodyHasReachedTarget();
    }

    private void obtainTargetLocation() {
        if(isMaster){
            setMasterTargetLocation();
            sendTargetLocationsToSlaves();
        }else{
            receiveTargetLocationFromMaster();
        }
    }

    private void setMasterTargetLocation() {
        targetLocation = assignment.get((long)numUAV);
    }

    private void sendTargetLocationsToSlaves() {
        for(Map.Entry<Long,Location3DUTM> e:assignment.entrySet()){
            int id = e.getKey().intValue();
            if(id == numUAV){continue;}
            JSONObject msg = Message.location(numUAV,id,e.getValue());
            commLink.sendJSONUntilACKReceived(msg,id,1);
        }
    }

    private void receiveTargetLocationFromMaster() {
        JSONObject msg = commLink.receiveMessageReplyACK(Message.location(numUAV),1);
        targetLocation = Message.processLocation(msg);
        masterId = msg.getInt(HighlevelCommLink.Keywords.SENDERID);
    }

    private void moveToTarget() {
        if(isMaster){
            sendMoveMsgToSlaves();
        }else{
            commLink.receiveMessageReplyACK(Message.move(numUAV),1);
        }
        ascend(5);
        moveDiagonallyToTarget();
    }

    private void sendMoveMsgToSlaves() {
        for(Long idl:assignment.keySet()){
            int id = idl.intValue();
            if(id == numUAV){continue;}
            JSONObject msg = Message.move(numUAV,id);
            commLink.sendJSONUntilACKReceived(msg,id,1);
        }
    }

    private void moveDiagonallyToTarget() {
        try {
            MoveTo t = copter.moveTo(new Location3D(targetLocation), new MoveToListener() {
                @Override
                public void onCompleteActionPerformed() {

                }

                @Override
                public void onFailure() {

                }
            });
            t.start();
            join(t);
        } catch (LocationNotReadyException e) {
            e.printStackTrace();
        }
    }

    private void ascend(int altitude) {
        TakeOff t = copter.takeOff(altitude, new TakeOffListener() {
            @Override
            public void onCompleteActionPerformed() {}
            @Override
            public void onFailure() { }
        });
        t.start();
        join(t);
    }

    private void waitUntilEverybodyHasReachedTarget() {
        if(isMaster){
            waitForSlaveToReachTarget();
            sendDone();
        }else{
            sendReachedTarget();
            waitForMsgDone();
        }
    }

    private void waitForSlaveToReachTarget() {
        Set<Integer> UAVsReachedTarget = new HashSet<>();
        while(UAVsReachedTarget.size() < assignment.size()-1) {
            JSONObject msg = commLink.receiveMessage(Message.targetReached(numUAV));
            if(msg != null){
                int senderId = msg.getInt(HighlevelCommLink.Keywords.SENDERID);
                if(!UAVsReachedTarget.contains(senderId)){
                    UAVsReachedTarget.add(senderId);
                    commLink.sendACK(msg);
                    commLink.sendACK(msg);
                }
            }
        }
    }

    private void sendDone() {
        Set<Integer> slaveIds = assignment.keySet().stream().map(Long::intValue).collect(Collectors.toSet());
        slaveIds.remove(numUAV);
        commLink.sendJSONUntilACKsReceived(Message.done(numUAV),slaveIds);
    }

    private void sendReachedTarget() {
        commLink.sendJSONUntilACKReceived(Message.targetReached(numUAV,masterId),masterId,1);
    }

    private void waitForMsgDone() {
        while(commLink.receiveMessageReplyACK(Message.done(),2) == null){
            API.getArduSim().sleep(200);
        }
    }

    private void join(Thread t){
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

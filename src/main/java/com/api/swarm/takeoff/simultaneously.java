package com.api.swarm.takeoff;

import com.api.*;
import com.api.communications.HighlevelCommLink;
import com.api.communications.HighlevelCommLink.Keywords;
import com.api.swarm.SwarmParam;
import es.upv.grc.mapper.Location3D;
import es.upv.grc.mapper.Location3DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class simultaneously extends TakeoffAlgorithm {
    HighlevelCommLink commLink;

    public simultaneously(Map<Long, Location3DUTM> assignment) {
        this.assignment = assignment;
    }

    @Override
    public void takeOff(int numUAV) {
        System.out.println(numUAV + " start takeoff process");
        this.numUAV = numUAV;
        this.commLink = new HighlevelCommLink(numUAV);
        boolean isMaster = assignment!=null;
        if(isMaster){
            setOwnTargetLocation();
            setSlaveTargetLocation();
            moveUAVToTargetLocation(sendOrderToTakeOff());
            System.out.println("master done taking off");
        }else{
            targetLocation = waitForTargetLocation();
            sendLocACKAndWaitUntilTakeOffMsg();
            moveUAVToTargetLocation(ACKOrderToTakeOff());
            System.out.println(numUAV + " slave done taking off");
        }
    }

    private void moveUAVToTargetLocation(Thread thread) {
        thread.start();
        moveToTargetLocation();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setOwnTargetLocation() {
        targetLocation = assignment.get((long) numUAV);
    }

    private void setSlaveTargetLocation() {
        Set<Integer> yetToSendLocation = createSlavesIDSet();
        while(yetToSendLocation.size()>0){
            sendLocation(yetToSendLocation);
            Set<Integer> acks = receiveAckLocation();
            yetToSendLocation.removeAll(acks);
        }
    }

    private Set<Integer> createSlavesIDSet() {
        Set<Integer> yetToSendLocation = new HashSet<>();
        for(int i=0;i<assignment.size();i++){
            if(i==numUAV){continue;}
            yetToSendLocation.add(i);
        }
        return yetToSendLocation;
    }

    private void sendLocation(Set<Integer> yetToSendLocation) {
        JSONObject locationMsg = new JSONObject();
        locationMsg.put(Keywords.SENDERID,numUAV);
        locationMsg.put(Keywords.MESSAGEID,SwarmParam.MSG_TAKEOFFLOC);
        for(long slaveId:yetToSendLocation){
            locationMsg.put(Keywords.RECEIVERID,slaveId);
            Location3DUTM loc = assignment.get(slaveId);
            locationMsg.put("x",loc.x);
            locationMsg.put("y",loc.y);
            locationMsg.put("z",loc.z);
            commLink.sendJSON(locationMsg);
        }
    }

    private Set<Integer> receiveAckLocation() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,-SwarmParam.MSG_TAKEOFFLOC);
        Set<Integer> acks = new HashSet<>();
        for(int i=0;i<5;i++) {
            JSONObject msg = commLink.receiveMessage(mandatoryFields);
            if (msg != null) {
                acks.add(msg.getInt(Keywords.SENDERID));
            }
        }
        return acks;
    }

    private Thread sendOrderToTakeOff() {
        JSONObject takeOffOrder = new JSONObject();
        takeOffOrder.put(Keywords.MESSAGEID,SwarmParam.MSG_TAKEOFF);
        takeOffOrder.put(Keywords.SENDERID,numUAV);
        return new Thread(()->{
            commLink.sendJSONUntilACKsReceived(takeOffOrder,createSlavesIDSet());
        });
    }

    private Thread ACKOrderToTakeOff() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,SwarmParam.MSG_TAKEOFF);
        return new Thread(() ->{
            commLink.receiveMessageReplyACK(mandatoryFields,10);
        });
    }

    private Location3DUTM waitForTargetLocation() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,SwarmParam.MSG_TAKEOFFLOC);
        mandatoryFields.put(Keywords.RECEIVERID,numUAV);
        while(true){
            JSONObject msg = commLink.receiveMessage(mandatoryFields);
            if(msg != null){
                return new Location3DUTM(msg.getDouble("x"), msg.getDouble("y"), msg.getDouble("z"));
            }
        }
    }

    private void sendLocACKAndWaitUntilTakeOffMsg() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,SwarmParam.MSG_TAKEOFF);
        while(true){
            sendLocACK();
            JSONObject msg = commLink.receiveMessage(mandatoryFields);
            if(msg != null){
                break;
            }
        }
    }

    private void sendLocACK() {
        JSONObject locACK = new JSONObject();
        locACK.put(Keywords.SENDERID,numUAV);
        locACK.put(Keywords.MESSAGEID,-SwarmParam.MSG_TAKEOFFLOC);
        commLink.sendJSON(locACK);
    }

    private void moveToTargetLocation() {
        ascend(SwarmParam.minimalTakeoffAltitude);
        moveDiagonallyToTarget();
    }

    private void ascend(double altitude) {
        AtomicBoolean altitudeReached = new AtomicBoolean(false);
        Copter copter = API.getCopter(numUAV);
        copter.takeOff(altitude, new TakeOffListener() {
            @Override
            public void onCompleteActionPerformed() {
                altitudeReached.set(true);
            }

            @Override
            public void onFailure() {
            }
        }).start();

        while(!altitudeReached.get()){
            API.getArduSim().sleep(200);
        }
    }

    private void moveDiagonallyToTarget() {
        AtomicBoolean locationReached = new AtomicBoolean(false);
        try {
            Location3D airLocation = new Location3D(targetLocation);
            Copter copter = API.getCopter(numUAV);
            copter.moveTo(airLocation, new MoveToListener() {
                @Override
                public void onCompleteActionPerformed() {
                    locationReached.set(true);
                }

                @Override
                public void onFailure() {

                }
            }).start();
        } catch (LocationNotReadyException e) {
            e.printStackTrace();
        }

        while(!locationReached.get()){
            API.getArduSim().sleep(500);
        }
    }

}

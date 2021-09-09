package com.api.swarm.discovery;

import com.api.API;
import com.api.ArduSim;
import com.api.Copter;
import com.api.communications.HighlevelCommLink;
import com.api.communications.HighlevelCommLink.Keywords;
import com.api.swarm.SwarmParam;
import com.setup.Param;
import es.upv.grc.mapper.Location3DUTM;
import org.json.JSONObject;
import java.util.*;

public class Discover{

    private int tempMasterId;
    private int masterUAVId;
    private final int numUAV;
    private final int numUAVs;
    private final Map<Long,Location3DUTM> UAVsDiscovered = new HashMap<>();
    private final HighlevelCommLink commLink;

    public Discover(int numUAV){
        System.out.println("creating discover object");
        this.numUAV = numUAV;
        this.numUAVs = API.getArduSim().getNumUAVs();
        setTempMasterId();
        commLink = new HighlevelCommLink(numUAV);
    }

    public void start() {
        System.out.println("starting discovery");
        if (isTempMaster(numUAV)) {
            masterDiscovering();
            System.out.println("DISCOVERY DONE");
        } else {
            slaveDiscovering();
        }
        setMasterUAVId();
    }

    public Map<Long, Location3DUTM> getUAVsDiscovered() {
        return UAVsDiscovered;
    }

    public Location3DUTM getCenterLocation() {
        double x=0,y=0,z=0;
        int numUAVs = UAVsDiscovered.size();
        for(Location3DUTM loc:UAVsDiscovered.values()){
            x += loc.x;
            y += loc.y;
            z += loc.z;
        }
        return new Location3DUTM(x/numUAVs,y/numUAVs,z/numUAVs);
    }

    public long getMasterUAVId() {
        return masterUAVId;
    }

    private void setMasterUAVId() {
        masterUAVId = tempMasterId;
    }

    private void masterDiscovering(){
        addMasterLocToDiscovered();
        addSlavesLocToDiscovered();
        waitUntilNoMessagesAreSendAnyMore();
    }

    private void addMasterLocToDiscovered() {
        Copter copter = API.getCopter(numUAV);
        Location3DUTM loc = new Location3DUTM(copter.getLocationUTM(),copter.getAltitude());
        UAVsDiscovered.put((long) numUAV,loc);
    }

    private void addSlavesLocToDiscovered() {
        List<Integer> yetToReceiveLoc = createSlaveList();
        while(yetToReceiveLoc.size() >0){
            System.out.println("still discovering:" + yetToReceiveLoc.size());
            sendAskLocation(yetToReceiveLoc);
            Set<Integer> sendACKList = processReceivedMessages(yetToReceiveLoc);
            sendLocACK(sendACKList);
        }
    }

    private List<Integer> createSlaveList() {
        List<Integer> slaveList = new ArrayList<>();
        for(int i=0;i<numUAVs;i++){
            if(i==numUAV){continue;}
            slaveList.add(i);
        }
        return slaveList;
    }

    private void sendAskLocation(List<Integer> yetToReceiveLocation) {
        JSONObject ackLocMsg = new JSONObject();
        ackLocMsg.put(Keywords.MESSAGEID, SwarmParam.MSG_ASK_FOR_LOCATION);
        ackLocMsg.put(Keywords.SENDERID, numUAV);
        for(int id:yetToReceiveLocation) {
            ackLocMsg.put(Keywords.RECEIVERID, id);
            commLink.sendJSON(ackLocMsg);
        }
    }

    private Set<Integer> processReceivedMessages(List<Integer> yetToReceiveLoc) {
        Set<Integer> sendACKList = new HashSet<>();
        int nrOfMsgToProcess = 5;
        for(int i = 0;i<nrOfMsgToProcess;i++) {
            JSONObject receivedMsg = commLink.receiveMessage();
            if(receivedMsg == null){break;}
            processLocationMsg(yetToReceiveLoc, sendACKList, receivedMsg);
        }
        return sendACKList;
    }

    private void processLocationMsg(List<Integer> yetToReceiveLoc, Set<Integer> sendACKList, JSONObject receivedMsg) {
        if (HighlevelCommLink.doesMessageContainAllMandatoryFields(createMandatoryParametersLocationMsg(), receivedMsg)) {
            saveLocation(receivedMsg);
            int id = receivedMsg.getInt(Keywords.SENDERID);
            yetToReceiveLoc.remove(Integer.valueOf(id));
            sendACKList.add(id);
        }
    }

    private void saveLocation(JSONObject receivedMsg) {
        long id = receivedMsg.getLong(Keywords.SENDERID);
        double x = receivedMsg.getDouble("x");
        double y = receivedMsg.getDouble("y");
        double z = receivedMsg.getDouble("z");
        Location3DUTM loc = new Location3DUTM(x,y,z);
        UAVsDiscovered.put(id,loc);
    }

    private void sendLocACK(Set<Integer> sendACKList) {
        JSONObject ackLocMsg = new JSONObject();
        ackLocMsg.put(Keywords.MESSAGEID, -SwarmParam.MSG_LOCATION);
        ackLocMsg.put(Keywords.SENDERID, numUAV);
        for(int id:sendACKList) {
            ackLocMsg.put(Keywords.RECEIVERID, id);
            commLink.sendJSON(ackLocMsg);
        }
    }

    private void waitUntilNoMessagesAreSendAnyMore() {
        long time = System.currentTimeMillis();
        boolean waitingUntilNoMessageIsReceivedAnymore = true;
        while(waitingUntilNoMessageIsReceivedAnymore){
            Set<Integer> ids = new HashSet<>();
            int nrOfMessagesToProcessAtOnce = 50;
            for(int i=0;i<nrOfMessagesToProcessAtOnce;i++) {
                JSONObject lostMsg = commLink.receiveMessage();
                if(lostMsg==null){break;}
                if (HighlevelCommLink.doesMessageContainAllMandatoryFields(createMandatoryParametersLocationMsg(), lostMsg)) {
                    ids.add(lostMsg.getInt(Keywords.SENDERID));
                    time = System.currentTimeMillis();
                }
            }
            sendLocACK(ids);
            waitingUntilNoMessageIsReceivedAnymore = (System.currentTimeMillis() - time < 1000);
            sleep(200);
        }
    }

    private Map<String, Object> createMandatoryParametersLocationMsg() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,SwarmParam.MSG_LOCATION);
        return mandatoryFields;
    }

    private void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void slaveDiscovering(){
        boolean startSendingLoc = false;
        while(true){
            JSONObject msg = commLink.receiveMessage();
            if(HighlevelCommLink.doesMessageContainAllMandatoryFields(createMandatoryParametersACKLoc(),msg)){
                break;
            }
            if(HighlevelCommLink.doesMessageContainAllMandatoryFields(createMandatoryParametersAskLoc(),msg)){
                startSendingLoc = true;
            }
            if(startSendingLoc){
                sendLocation();
            }
            sleep(5);
        }
    }

    private Map<String, Object> createMandatoryParametersACKLoc() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,-SwarmParam.MSG_LOCATION);
        mandatoryFields.put(Keywords.RECEIVERID,numUAV);
        mandatoryFields.put(Keywords.SENDERID,tempMasterId);
        return mandatoryFields;
    }

    private Map<String, Object> createMandatoryParametersAskLoc() {
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(Keywords.MESSAGEID,SwarmParam.MSG_ASK_FOR_LOCATION);
        mandatoryFields.put(Keywords.RECEIVERID,numUAV);
        mandatoryFields.put(Keywords.SENDERID,tempMasterId);
        return mandatoryFields;
    }

    private void sendLocation() {
        Copter copter = API.getCopter(numUAV);
        Location3DUTM loc = new Location3DUTM(copter.getLocationUTM(),copter.getAltitude());
        JSONObject locationMsg = new JSONObject();
        locationMsg.put(Keywords.MESSAGEID, SwarmParam.MSG_LOCATION);
        locationMsg.put(Keywords.SENDERID,numUAV);
        locationMsg.put("x",loc.x);
        locationMsg.put("y",loc.y);
        locationMsg.put("z",loc.z);
        commLink.sendJSON(locationMsg);
    }

    private boolean isTempMaster(int i) {
        return i==tempMasterId;
    }

    private void setTempMasterId() {
        int role = Param.role;
        if (role == ArduSim.MULTICOPTER) {
            /* You get the id = f(MAC addresses) for real drone */
            long thisUAVID = Param.id[0];
            for (int i = 0; i < SwarmParam.macIDs.length; i++) {
                if (thisUAVID == SwarmParam.macIDs[i]) {
                    tempMasterId = i;
                }
            }
        } else if (role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
            tempMasterId = 0;
        }
    }
}

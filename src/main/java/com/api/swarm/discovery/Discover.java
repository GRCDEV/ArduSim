package com.api.swarm.discovery;

import com.api.API;
import com.api.ArduSim;
import com.api.GUI;
import com.api.communications.HighlevelCommLink;
import com.api.copter.Copter;
import com.api.swarm.SwarmParam;
import com.setup.Param;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Discover {

    private int tempMasterId;
    private final int numUAV;
    private final int numUAVs;
    private final Map<Long, Location3DUTM> UAVsDiscovered = new HashMap<>();
    private final HighlevelCommLink commLink;
    private final GUI gui;

    public Discover(int numUAV){
        this.numUAV = numUAV;
        this.gui = API.getGUI(numUAV);
        this.numUAVs = API.getArduSim().getNumUAVs();
        setTempMasterId();
        commLink = new HighlevelCommLink(numUAV, UAVParam.internalBroadcastPort);
    }

    public void start() {
        gui.updateProtocolState("DISCOVERING");
        if (numUAV == tempMasterId) {
            masterDiscovering();
        } else {
            slaveDiscovering();
        }
    }

    public int getMasterUAVId(){return tempMasterId;}

    public Location3DUTM getCenterLocation(){
        if(numUAV == tempMasterId){
            return getLocation3DUTM();
        }
        return null;
    }

    public Map<Long, Location3DUTM> getUAVsDiscovered(){
        return UAVsDiscovered;
    }

    private void masterDiscovering() {
        Location3DUTM loc = getLocation3DUTM();
        UAVsDiscovered.put((long)numUAV,loc);
        while(UAVsDiscovered.size() != numUAVs){
            JSONObject msg = commLink.receiveMessage(Message.location(numUAV));
            if(msg != null){
                Pair<Long, Location3DUTM> p = Message.processLocation(msg);
                if(!UAVsDiscovered.containsKey(p.getValue0())) {
                    UAVsDiscovered.put(p.getValue0(), p.getValue1());
                    commLink.sendACK(msg);
                    gui.logVerbose("Discovered UAV: "  + p.getValue0() + "\t UAVs discovered: " + UAVsDiscovered.size() + "/" + numUAVs);
                }
            }
        }
        logVerboseUAVsDiscovered();
    }

    private void logVerboseUAVsDiscovered() {
        for(Map.Entry<Long,Location3DUTM> e:UAVsDiscovered.entrySet()){
            gui.logVerbose(e.getKey() + "\t" + e.getValue());
        }
    }

    private void slaveDiscovering() {
        Location3DUTM loc = getLocation3DUTM();
        JSONObject locationMsg = Message.location(numUAV,tempMasterId,loc);
        commLink.sendJSONUntilACKReceived(locationMsg,tempMasterId,1);
        gui.logVerbose(numUAV + " done");
    }


    private Location3DUTM getLocation3DUTM() {
        Copter copter = API.getCopter(numUAV);
        return new Location3DUTM(copter.getLocation().getUTMLocation(), copter.getAltitude());
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

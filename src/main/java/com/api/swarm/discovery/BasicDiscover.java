package com.api.swarm.discovery;

import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.json.JSONObject;
import java.util.Map;

public class BasicDiscover extends Discover {

    public BasicDiscover(int numUAV){
        super(numUAV);
    }

    void masterDiscovering() {
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
        gui.log("Locations of UAVs discovered");
        for(Map.Entry<Long,Location3DUTM> e:UAVsDiscovered.entrySet()){
            gui.logVerbose(e.getKey() + "\t" + e.getValue());
        }
    }

    void slaveDiscovering() {
        Location3DUTM loc = getLocation3DUTM();
        JSONObject locationMsg = Message.location(numUAV, masterId,loc);
        commLink.sendJSONUntilACKReceived(locationMsg, masterId,2);
        gui.logVerbose(numUAV + " done");
    }

}

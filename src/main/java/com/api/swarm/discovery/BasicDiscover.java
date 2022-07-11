package com.api.swarm.discovery;

import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.json.JSONObject;
import java.util.Map;

public class BasicDiscover extends Discover {

    public BasicDiscover(int numUAV){
        super(numUAV);
    }

    @Override
    public void start() {
        discoverUAVs(10000l);
    }

    private void discoverUAVs(long time) {
        UAVsDiscovered.put((long) numUAV,getLocation3DUTM());
        long start = System.currentTimeMillis();

        while(System.currentTimeMillis() - start < time){
            sendLocation();
            receiveLocationFromOthersAndUpdateUAVsDiscovered();
        }
    }

    private void receiveLocationFromOthersAndUpdateUAVsDiscovered() {
        JSONObject msg = commLink.receiveMessage(Message.location());
        while(msg != null){
            Pair<Long, Location3DUTM> p = Message.processLocation(msg);
            if(!UAVsDiscovered.containsKey(p.getValue0())) {
                UAVsDiscovered.put(p.getValue0(), p.getValue1());
                gui.logVerbose("Discovered UAV: "  + p.getValue0() + "\t UAVs discovered: " + UAVsDiscovered.size() + "/" + numUAVs);
            }
            msg = commLink.receiveMessage(Message.location());
        }
    }

    private void sendLocation() {
        Location3DUTM loc = getLocation3DUTM();
        JSONObject locationMsg = Message.location(numUAV,loc);
        commLink.sendJSON(locationMsg);
    }

    private void logVerboseUAVsDiscovered() {
        gui.log("Locations of UAVs discovered");
        for(Map.Entry<Long,Location3DUTM> e:UAVsDiscovered.entrySet()){
            gui.logVerbose(e.getKey() + "\t" + e.getValue());
        }
    }

}

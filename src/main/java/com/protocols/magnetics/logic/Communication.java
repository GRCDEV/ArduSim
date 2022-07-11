package com.protocols.magnetics.logic;

import com.api.API;
import com.api.communications.HighlevelCommLink;
import com.api.copter.Copter;
import com.protocols.magnetics.pojo.Message;
import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Communication extends Thread{

    private final int numUAV;
    private final Copter copter;
    private final HighlevelCommLink commLink;
    private final Map<Integer, Pair<Long,Location3DUTM>> locations;
    private Boolean running;

    public Communication(int numUAV){
        this.numUAV = numUAV;
        this.copter = API.getCopter(numUAV);
        commLink = new HighlevelCommLink(numUAV,1,1);
        locations = new ConcurrentHashMap<>();
        running = true;
    }

    public void run(){
        while(running){
            long start = System.currentTimeMillis();
            commLink.sendJSON(Message.location(numUAV,getCopterLocation()));

            long timeDif = System.currentTimeMillis() - start;
            JSONObject msg = commLink.receiveMessage(Message.location(numUAV));
            while(msg != null && timeDif < 1000){
                int senderId = (Integer) msg.get(HighlevelCommLink.Keywords.SENDERID);
                Location3DUTM obstacle = Message.processLocation(msg);
                long timeStamp = System.currentTimeMillis();
                locations.put(senderId,new Pair<>(timeStamp,obstacle));

                msg = commLink.receiveMessage(Message.location(numUAV));
                timeDif = System.currentTimeMillis() - start;
            }

            if(timeDif < 1000) {
                API.getArduSim().sleep(1000 - timeDif);
            }
        }
    }

    public List<Location3DUTM> getObstacles(){
        Set<Integer> expired = new HashSet<>();
        long now = System.currentTimeMillis();
        for(int i: locations.keySet()){
            if(now - locations.get(i).getValue0() > 5000) {
                expired.add(i);
            }
        }
        locations.keySet().removeAll(expired);
        List<Location3DUTM> obstacles = new ArrayList<>();
        for(Pair<Long,Location3DUTM> p:locations.values()){
            obstacles.add(p.getValue1());
        }
        return obstacles;
    }

    public void stopCommunication(){
        running = false;
    }

    private Location3DUTM getCopterLocation() {
        return new Location3DUTM(copter.getLocationUTM(),copter.getAltitude());
    }

}

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

import java.util.HashMap;
import java.util.Map;

public abstract class Discover {

    protected int masterId = -1;
    protected final int numUAV;
    protected final int numUAVs;
    protected final Map<Long, Location3DUTM> UAVsDiscovered = new HashMap<>();
    protected final HighlevelCommLink commLink;
    protected final GUI gui;

    public Discover(int numUAV){
        this.numUAV = numUAV;
        this.gui = API.getGUI(numUAV);
        this.numUAVs = API.getArduSim().getNumUAVs();
        commLink = new HighlevelCommLink(numUAV, UAVParam.internalBroadcastPort);
    }

    abstract public void start();

    public Map<Long, Location3DUTM> getUAVsDiscovered(){
        return UAVsDiscovered;
    }

    public int getMasterUAVId(){
        if(masterId == -1) {
            masterId = getCenterUAV();
        }
        return masterId;
    }

    public Location3DUTM getMasterLocation(){
        long master = getMasterUAVId();
        return UAVsDiscovered.get(master);
    }

    private int getCenterUAV(){
        Location3DUTM centre = calculateCentreOfUAVsDiscovered();
        return getUAVClosedToCentre(centre);
    }

    private int getUAVClosedToCentre(Location3DUTM centre) {
        long centreId = -1;
        double closestDistance = Double.MAX_VALUE;
        for(Map.Entry<Long, Location3DUTM> e: UAVsDiscovered.entrySet()){
            double d = e.getValue().distance3D(centre);
            if(d < closestDistance){
                closestDistance = d;
                centreId = e.getKey();
            }
        }
        return (int) centreId;
    }

    private Location3DUTM calculateCentreOfUAVsDiscovered() {
        int x = 0;
        int y = 0;
        int z = 0;
        for(Location3DUTM loc: UAVsDiscovered.values()){
            x += loc.x;
            y += loc.y;
            z += loc.z;
        }
        int n = UAVsDiscovered.size();
        return new Location3DUTM(x/n,y/n,z/n);
    }

    protected Location3DUTM getLocation3DUTM() {
        Copter copter = API.getCopter(numUAV);
        return new Location3DUTM(copter.getLocation().getUTMLocation(), copter.getAltitude());
    }
}

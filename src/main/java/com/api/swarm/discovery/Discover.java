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

    protected int masterId;
    protected final int numUAV;
    protected final int numUAVs;
    protected final Map<Long, Location3DUTM> UAVsDiscovered = new HashMap<>();
    protected final HighlevelCommLink commLink;
    protected final GUI gui;

    public Discover(int numUAV){
        this.numUAV = numUAV;
        this.gui = API.getGUI(numUAV);
        this.numUAVs = API.getArduSim().getNumUAVs();
        setTempMasterId();
        commLink = new HighlevelCommLink(numUAV, UAVParam.internalBroadcastPort);
    }

    public void start() {
        gui.updateProtocolState("DISCOVERING");
        if (numUAV == masterId) {
            masterDiscovering();
        } else {
            slaveDiscovering();
        }
        //TODO change the master UAV afterwards so that it is the center UAV and not just UAV 0
    }

    public Map<Long, Location3DUTM> getUAVsDiscovered(){
        return UAVsDiscovered;
    }

    public int getMasterUAVId(){return masterId;}

    public Location3DUTM getCenterLocation(){
        if(numUAV == masterId){
            return getLocation3DUTM();
        }
        return null;
    }

    protected void setTempMasterId() {
        int role = Param.role;
        if (role == ArduSim.MULTICOPTER) {
            /* You get the id = f(MAC addresses) for real drone */
            long thisUAVID = Param.id[0];
            for (int i = 0; i < SwarmParam.macIDs.length; i++) {
                if (thisUAVID == SwarmParam.macIDs[i]) {
                    masterId = i;
                }
            }
        } else if (role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
            masterId = 0;
        }
    }

    protected Location3DUTM getLocation3DUTM() {
        //TODO update this method because now it only works if it is invoked by the master
        Copter copter = API.getCopter(numUAV);
        return new Location3DUTM(copter.getLocation().getUTMLocation(), copter.getAltitude());
    }


    abstract void masterDiscovering();
    abstract void slaveDiscovering();
}

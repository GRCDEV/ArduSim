package com.uavController;

import com.api.API;
import com.api.copter.Copter;
import com.setup.Param;
import es.upv.grc.mapper.Location2DUTM;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class OmnetppTalker extends Thread{

    private DatagramSocket socket;
    private InetAddress address;
    private long lastMsgSendTimestamp;
    private final int sendToOmnetInterval = 1000; //ms
    private boolean running = false;
    private int numUAV;
    private Copter copter;

    public OmnetppTalker(int numUAV){
        if(UAVParam.usingOmnetpp){
            running = true;
            this.numUAV = numUAV;
            this.copter = API.getCopter(numUAV);
            try {
                socket = new DatagramSocket();
                address = InetAddress.getByName("localhost");
                lastMsgSendTimestamp = System.currentTimeMillis();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run(){
        while(Param.simStatus.getStateId() < Param.SimulatorState.SETUP_IN_PROGRESS.getStateId()){
            API.getArduSim().sleep(200);
        }
        while(running) {
            sendPositionData();
        }
    }

    public void end(){
        running = false;
    }

    private void sendPositionData() {
        if(System.currentTimeMillis() - lastMsgSendTimestamp > sendToOmnetInterval){
            Location2DUTM loc = copter.getLocation().getUTMLocation();
            if(loc != null) {
                JSONObject msg = new JSONObject();
                msg.put("OMNETPP_TYPE", "STATUS");
                msg.put("numUAV", numUAV);
                JSONObject pos = new JSONObject();
                pos.put("x", loc.x);
                pos.put("y", loc.y);
                pos.put("z", copter.getAltitude());
                msg.put("pos", pos);
                lastMsgSendTimestamp = System.currentTimeMillis();
            }
        }
    }

}

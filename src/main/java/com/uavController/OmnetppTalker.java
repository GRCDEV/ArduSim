package com.uavController;

import com.api.API;
import com.api.copter.Copter;
import com.setup.Param;
import es.upv.grc.mapper.Location2DUTM;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;


public class OmnetppTalker extends Thread{

    private DatagramSocket socket;
    private InetAddress address;
    private long lastMsgSendTimestamp;
    private final int sendToOmnetInterval = 1000; //ms
    private boolean running = false;
    private int numUAV;
    private Copter copter;

    private double centerX, centerY, centerZ;
    private int lastX, lastY, lastZ;
    BufferedWriter writer;
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
            centerX = 728673.70; //728356.09;
            centerY = 4373883.52; //4374246.00;
            centerZ = 0;
            try {
                writer = new BufferedWriter(new FileWriter("outputPos.csv"));
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
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void end(){
        running = false;
    }

    private void sendPositionData() {
        if(System.currentTimeMillis() - lastMsgSendTimestamp > sendToOmnetInterval){
            Location2DUTM loc = copter.getLocation().getUTMLocation();
            if(loc != null && copter.getAltitude() > 39.5) {
                int x = (int)(loc.x - centerX);
                int y = (int)(centerY - loc.y );
                int z = (int)(copter.getAltitude());

                if( (lastX != x) || (lastY != y) || (lastZ != z)){
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    String msg = x + "," + y + "," + z;
                    if(numUAV == 0){
                        try {
                            writer.write(msg + "\n");
                            writer.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
                    int port = 8000 + numUAV;
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}

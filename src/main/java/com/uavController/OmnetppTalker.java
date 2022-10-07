package com.uavController;

import com.api.API;
import com.api.copter.Copter;
import com.setup.Param;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import es.upv.grc.mapper.Mapper;

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
    private final int sendToOmnetInterval = 200; //ms
    private boolean running = false;
    private int numUAV;
    private Copter copter;

    private double centerX, centerY, centerZ;
    private int lastX, lastY, lastZ;

    private boolean loggingPos = false;
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
            centerX = 728634.65; //728356.09;
            centerY = 4374173.90; //4374246.00;

            centerZ = 0;
            if(loggingPos) {
                try {
                    String filename = "outputPos" + numUAV + ".csv";
                    writer = new BufferedWriter(new FileWriter(filename));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void run(){
        if(UAVParam.usingOmnetpp) {
            while (Param.simStatus.getStateId() < Param.SimulatorState.SETUP_IN_PROGRESS.getStateId()) {
                API.getArduSim().sleep(200);
            }
            while (running) {
                sendPositionData();
            }
            if(loggingPos) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void end(){
        running = false;
    }

    private void sendPositionData() {
        if(System.currentTimeMillis() - lastMsgSendTimestamp > sendToOmnetInterval){
            Location2DUTM loc = copter.getLocation().getUTMLocation();
            if(loc != null && copter.getAltitude() > 25) {

                int x = (int) (loc.x - centerX);
                int y = (int) (centerY - loc.y)*-1;
                int z = (int) (copter.getAltitude());

                if ((lastX != x) || (lastY != y) || (lastZ != z)) {
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    String msg = x + "," + y + "," + z;
                    if(loggingPos) {
                        try {
                            writer.write(numUAV + "," + msg + "\n");
                            writer.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
                    int port = 8000 + numUAV;
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                    lastMsgSendTimestamp = System.currentTimeMillis();
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

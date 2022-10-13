package com.protocols.omnetTest;

import com.api.API;
import com.api.communications.HighlevelCommLink;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class OmnetTestThread extends Thread{

    private int numUAV;
    private HighlevelCommLink commLink;
    private int frequency = 1;

    private Random random;

    public OmnetTestThread(int numUAV){
        this.numUAV = numUAV;
        this.commLink = new HighlevelCommLink(numUAV);
        random = new Random(numUAV * 19L);
    }

    public void run(){
        if(numUAV == 0){
            receiveMessages();
        }else{
            int delay = (int) (random.nextFloat()*1000);
            API.getArduSim().sleep(delay);
            System.out.println(delay);
            sendMessages();
        }
    }

    private void sendMessages() {
        for(int msgNr=0;msgNr<100;msgNr++){
            JSONObject msg = new JSONObject();
            msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
            msg.put(HighlevelCommLink.Keywords.RECEIVERID,0);
            msg.put("status","sending");
            msg.put("frequency",1);
            msg.put("msgNr",msgNr);
            msg.put("sendTime",System.currentTimeMillis());
            commLink.sendJSON(msg);
            API.getArduSim().sleep(1000/frequency + (int) (random.nextFloat()*100));
        }
        API.getCopter(numUAV).land();
    }

    private void receiveMessages() {
        int numUAVs = API.getArduSim().getNumUAVs();
        long[][] packetInfo = new long[numUAVs-1][100];

        boolean receivingMessages = false;
        long lastMessageTime = System.currentTimeMillis();
        while(!receivingMessages) {
            JSONObject msg = commLink.receiveMessage();
            if(msg != null){
                lastMessageTime = System.currentTimeMillis();
                int sender = msg.getInt(HighlevelCommLink.Keywords.SENDERID);
                int msgNr = msg.getInt("msgNr");
                long timeDiff = System.currentTimeMillis() - msg.getLong("sendTime");
                packetInfo[sender-1][msgNr] = timeDiff;
            }else{
                receivingMessages = (lastMessageTime + 5000 < System.currentTimeMillis());
            }
        }
        printStatistics(packetInfo);
        API.getCopter(numUAV).land();
    }

    private void printStatistics (long[][] packetInfo){

        int lostPacket = 0;
        int sizeGrid = 0;
        int sum = 0;


        for (long[] rows : packetInfo) {
            for (long cell: rows) {
                sizeGrid++;
                if(cell == 0){
                    lostPacket++;
                }else{
                    sum += cell;
                }
            }
        }

        int actualValues = sizeGrid - lostPacket;
        double mean = (double) sum/actualValues;
        double std = 0.0;

        String fileName = "scalingData_" + API.getArduSim().getNumUAVs() + "UAV_" + frequency + "hz" + ".csv";
        try {

            FileWriter rawData = new FileWriter(fileName);
            for (long[] rows : packetInfo) {
                rawData.write("\n");
                for (long cell : rows) {
                    rawData.write(cell + ",");
                    if(cell != 0){
                        std += Math.pow(cell - mean,2);
                    }
                }
            }
            std = Math.sqrt(std/actualValues);
            rawData.close();

            FileWriter compressedData = null;
            compressedData = new FileWriter("scalingDataProcessed.csv",true);
            compressedData.write(API.getArduSim().getNumUAVs() + ",");
            compressedData.write(frequency + ",");
            compressedData.write((float) lostPacket/sizeGrid + ",");
            compressedData.write(mean + ",");
            compressedData.write(std + "\n");
            compressedData.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

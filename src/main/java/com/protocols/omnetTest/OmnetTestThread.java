package com.protocols.omnetTest;

import com.api.API;
import com.api.communications.HighlevelCommLink;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class OmnetTestThread extends Thread{

    private int numUAV;
    private HighlevelCommLink commLink;

    public OmnetTestThread(int numUAV){
        this.numUAV = numUAV;
        this.commLink = new HighlevelCommLink(numUAV);
    }

    public void run(){
        if(numUAV == 0){
            sendMessages();
        }else{
            receiveMessages();
        }
    }

    private void sendMessages() {
        for(int frequency=1;frequency<12;frequency++){
            for(int msgNr=0;msgNr<100;msgNr++){
                JSONObject msg = new JSONObject();
                msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
                msg.put("status","sending");
                msg.put("frequency",frequency);
                msg.put("msgNr",msgNr);
                msg.put("sendTime",System.currentTimeMillis());
                commLink.sendJSON(msg);

                API.getArduSim().sleep(1000/frequency);
            }
        }
        JSONObject msg = new JSONObject();
        msg.put("status","end");
        commLink.sendJSON(msg);
        System.out.println("sender done");
    }

    private void receiveMessages() {
        ArrayList<Long> timeDiff = new ArrayList();
        boolean running = true;
        int currentFrequency= 1;
        System.out.println("mode,frequency,avg_msg_time[ms],std");
        while(running) {
            JSONObject msg = commLink.receiveMessage();
            if(msg != null){
                String status = (String) msg.get("status");
                if(Objects.equals(status, "end")){
                    running= false;
                }else {
                    if (currentFrequency != (int) msg.get("frequency")) {
                        double avg = getAverage(timeDiff);
                        double std = calculateSD(timeDiff);
                        System.out.println("omnet," + currentFrequency + "," + avg + "," + std);
                        timeDiff.clear();
                        currentFrequency = (int) msg.get("frequency");
                    }

                    long sendTime = (Long) msg.get("sendTime");
                    long now = System.currentTimeMillis();
                    timeDiff.add(now - sendTime);
                }
            }
        }
        System.out.println("receiver done");
    }


    private double getAverage(ArrayList<Long> data){
        long sum = data.stream().mapToLong(Long::longValue).sum();
        return (sum + 0.0) / data.size();
    }

    private double calculateSD(ArrayList<Long> data)
    {
        double standardDeviation = 0.0;
        double mean = getAverage(data);

        for(long num: data) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/ data.size());
    }

}

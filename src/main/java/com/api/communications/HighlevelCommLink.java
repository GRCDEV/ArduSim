package com.api.communications;
import com.api.communications.lowLevel.LowLevelCommLink;
import com.uavController.UAVParam;
import org.json.JSONObject;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class HighlevelCommLink {

    private final int numUAV;
    private final LowLevelCommLink commLink;
    private final int sendingTimeout;
    private final int readingTimeout;

    public static class Keywords{
        public static final String MESSAGEID = "msgID";
        public static final String SENDERID = "senderID";
        public static final String RECEIVERID = "receiverID";
    }

    public HighlevelCommLink(int numUAV){
        this.numUAV = numUAV;
        this.commLink = LowLevelCommLink.getCommLink(numUAV);
        this.sendingTimeout = 0; //200
        this.readingTimeout = 50;
    }

    public HighlevelCommLink(int numUAV, int portnumber){
        this.numUAV = numUAV;
        this.commLink = LowLevelCommLink.getCommLink(numUAV,portnumber);
        this.sendingTimeout = 0; //200
        this.readingTimeout = 50;
    }

    public HighlevelCommLink(int numUAV, int sendingTimeout, int readingTimeout){
        this.numUAV = numUAV;
        this.commLink = LowLevelCommLink.getCommLink(numUAV);
        this.sendingTimeout = sendingTimeout;
        this.readingTimeout = readingTimeout;
    }

    public void sendJSON(JSONObject message){
        if(UAVParam.usingOmnetpp){
            message.put("OMNETPP_TYPE","ARDUSIMMSG");
        }
        String s = message.toString();
        commLink.sendBroadcastMessage(s.getBytes());
        sleep(sendingTimeout);
    }

    public JSONObject receiveMessage(){
        JSONObject message = null;
        byte[] inBuffer = commLink.receiveMessage(readingTimeout);
        if (inBuffer != null) {
            String msg = new String(inBuffer, Charset.defaultCharset());
            message = new JSONObject(msg);
            sleep(readingTimeout);
        }
        return message;
    }

    public JSONObject receiveMessage(Map<String,Object> mandatoryFields){
        JSONObject message = receiveMessage();
        if(doesMessageContainAllMandatoryFields(mandatoryFields,message)){
            //sleep(readingTimeout);
            return message;
        }
        sleep(5);
        return null;
    }

    public static boolean doesMessageContainAllMandatoryFields(Map<String,Object> mandatoryFields, JSONObject message){
        if(message==null){return false;}
        for(Map.Entry<String,Object> entry:mandatoryFields.entrySet()){
            String key = entry.getKey();
            try {
                Object o = message.get(key);
                if(!o.equals(entry.getValue())) {
                    return false;
                }
            }catch(Exception e){
                return false;
            }
        }
        return true;
    }

    public JSONObject receiveMessageReplyACK(Map<String,Object> mandatoryFields, int nrOfRepliesSend){
        //TODO make nrOfRepliesSend automatic (based on experiments)
        JSONObject message = null;
        while(message == null){
            message = receiveMessage(mandatoryFields);

        }
        JSONObject finalMessage = message;
        for(int i=0;i<nrOfRepliesSend;i++) {
            sendACK(finalMessage);
        }
        return message;
    }

    public void sendJSONUntilACKsReceived(JSONObject message, Set<Integer> ackIDs){
        while(ackIDs.size() > 0){
            sendJSON(message);
            int ackID = -message.getInt(Keywords.MESSAGEID);
            Map<String,Object> mandatoryFields = new HashMap<>();
            mandatoryFields.put(Keywords.MESSAGEID,ackID);
            JSONObject receivedMsg = receiveMessage(mandatoryFields);
            if(receivedMsg != null){
                int senderId = receivedMsg.getInt(Keywords.SENDERID);
                ackIDs.remove(senderId);
            }
        }
    }

    public void sendJSONUntilACKReceived(JSONObject message, int ackID, int minNrOfACKs){
        int acksReceived = 0;
        while(acksReceived < minNrOfACKs){
            sendJSON(message);
            JSONObject receivedMsg = receiveMessage();
            if(receivedMsg != null){
                boolean msgIDCorrect = -message.getInt(Keywords.MESSAGEID) == receivedMsg.getInt(Keywords.MESSAGEID);
                boolean ackIDCorrect = ackID == receivedMsg.getInt(Keywords.SENDERID);
                if(msgIDCorrect && ackIDCorrect){
                    acksReceived++;
                }
            }
        }
    }

    public void sendACK(JSONObject message){
        int msgID = message.getInt(Keywords.MESSAGEID);
        int receiverID = message.getInt(Keywords.SENDERID);
        JSONObject ackMessage = new JSONObject();
        ackMessage.put(Keywords.SENDERID,numUAV);
        ackMessage.put(Keywords.RECEIVERID,receiverID);
        ackMessage.put(Keywords.MESSAGEID,-msgID);
        sendJSON(ackMessage);
    }

    private void sleep(int millies){
        try {
            Thread.sleep(millies);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

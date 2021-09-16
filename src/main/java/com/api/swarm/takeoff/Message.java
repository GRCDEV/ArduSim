package com.api.swarm.takeoff;

import com.api.communications.HighlevelCommLink;
import es.upv.grc.mapper.Location3DUTM;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class Message {

    private final static int LOCATION  = 1;
    private final static int MOVE = 2;
    private final static int TARGET_REACHED = 3;
    private final static int DONE = 4;

    public static JSONObject location(int numUAV, int receiver, Location3DUTM loc){
        JSONObject msg = new JSONObject();
        msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
        msg.put(HighlevelCommLink.Keywords.RECEIVERID,receiver);
        msg.put(HighlevelCommLink.Keywords.MESSAGEID,LOCATION);
        JSONObject location = new JSONObject();
        location.put("x",loc.x);
        location.put("y",loc.y);
        location.put("z",loc.z);
        msg.put("location",location);
        return msg;
    }

    public static Map<String,Object> location(int numUAV){
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(HighlevelCommLink.Keywords.RECEIVERID,numUAV);
        mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,LOCATION);
        return mandatoryFields;
    }

    public static Location3DUTM processLocation(JSONObject msg){
        JSONObject locationJSON = msg.getJSONObject("location");
        double x = locationJSON.getInt("x");
        double y = locationJSON.getInt("y");
        double z = locationJSON.getInt("z");
        return new Location3DUTM(x,y,z);
    }

    public static JSONObject move(int numUAV, int receiver){
        JSONObject msg = new JSONObject();
        msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
        msg.put(HighlevelCommLink.Keywords.RECEIVERID,receiver);
        msg.put(HighlevelCommLink.Keywords.MESSAGEID,MOVE);
        return msg;
    }

    public static Map<String,Object> move(int numUAV){
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(HighlevelCommLink.Keywords.RECEIVERID,numUAV);
        mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,MOVE);
        return mandatoryFields;
    }

    public static JSONObject targetReached(int numUAV,int receiver){
        JSONObject msg = new JSONObject();
        msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
        msg.put(HighlevelCommLink.Keywords.RECEIVERID,receiver);
        msg.put(HighlevelCommLink.Keywords.MESSAGEID,TARGET_REACHED);
        return msg;
    }

    public static Map<String,Object> targetReached(int numUAV){
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(HighlevelCommLink.Keywords.RECEIVERID,numUAV);
        mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,TARGET_REACHED);
        return mandatoryFields;
    }

    public static JSONObject done(int numUAV){
        JSONObject msg = new JSONObject();
        msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
        msg.put(HighlevelCommLink.Keywords.MESSAGEID,DONE);
        return msg;
    }

    public static Map<String,Object> done(){
        Map<String,Object> mandatoryFields = new HashMap<>();
        mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,DONE);
        return mandatoryFields;
    }
}

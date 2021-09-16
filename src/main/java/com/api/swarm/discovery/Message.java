package com.api.swarm.discovery;

import com.api.communications.HighlevelCommLink;
import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.json.JSONObject;

import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;

public class Message {

    private final static int LOCATION  = 1;
    private final static int DONE = 2;


    public static JSONObject location(int numUAV,int receiver, Location3DUTM loc){
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

    public static Pair<Long,Location3DUTM> processLocation(JSONObject msg){
        Long senderId = (long) msg.getInt(HighlevelCommLink.Keywords.SENDERID);
        JSONObject locationJSON = msg.getJSONObject("location");
        double x = locationJSON.getInt("x");
        double y = locationJSON.getInt("y");
        double z = locationJSON.getInt("z");
        Location3DUTM location = new Location3DUTM(x,y,z);
        return new Pair<>(senderId,location);
    }
}

package com.protocols.muscop.logic;

import com.api.communications.HighlevelCommLink;
import es.upv.grc.mapper.Location3DUTM;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * List of messages sent.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

class Message {

	private static final int MISSION_DATA = 1;
	private static final int WAYPOINT_REACHED = 2;
	private static final int MOVE_TO_WAYPOINT = 3;
	private static final int LAND = 4;

	public static JSONObject missionData(int numUAV, int numUAVs, ArrayList<Location3DUTM> waypoints) {
		JSONObject msg = new JSONObject();
		msg.put(HighlevelCommLink.Keywords.MESSAGEID, MISSION_DATA);
		msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
		msg.put("numUAVs",numUAVs);
		JSONObject wps = new JSONObject();
		for(int i=0;i<waypoints.size();i++){
			JSONObject wp = new JSONObject();
			wp.put("x",waypoints.get(i).x);
			wp.put("y",waypoints.get(i).y);
			wp.put("z",waypoints.get(i).z);
			wps.put(String.valueOf(i),wp);
		}
		msg.put("waypoints",wps);
		return msg;
	}

	public static Map<String,Object> missionData(){
		Map<String,Object> mandatoryFields = new HashMap<>();
		mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,MISSION_DATA);
		mandatoryFields.put(HighlevelCommLink.Keywords.SENDERID,0);
		return mandatoryFields;
	}

	public static JSONObject waypointReached(int numUAV,int waypoint){
		JSONObject msg = new JSONObject();
		msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
		msg.put(HighlevelCommLink.Keywords.MESSAGEID, WAYPOINT_REACHED);
		msg.put("waypoint",waypoint);
		return msg;
	}

	public static Map<String,Object> waypointReached(int waypoint){
		Map<String,Object> mandatoryFields = new HashMap<>();
		mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,WAYPOINT_REACHED);
		mandatoryFields.put("waypoint",waypoint);
		return mandatoryFields;
	}

	public static JSONObject moveToWaypoint(int numUAV,int waypoint){
		JSONObject msg = new JSONObject();
		msg.put(HighlevelCommLink.Keywords.SENDERID,numUAV);
		msg.put(HighlevelCommLink.Keywords.MESSAGEID, MOVE_TO_WAYPOINT);
		msg.put("waypoint", waypoint);
		return msg;
	}

	public static Map<String,Object> moveToWaypoint(int waypoint){
		Map<String,Object> mandatoryFields = new HashMap<>();
		mandatoryFields.put(HighlevelCommLink.Keywords.MESSAGEID,MOVE_TO_WAYPOINT);
		mandatoryFields.put("waypoint",waypoint);
		return mandatoryFields;
	}
}



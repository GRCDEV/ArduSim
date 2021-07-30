package com.protocols.shakeup.pojo;

import com.api.swarm.assignement.AssignmentAlgorithm;
import com.api.swarm.formations.Formation;

public class Param {

	public static volatile double masterInitialLatitude = 39.482615; // (degrees) Latitude for simulations
	public static volatile double masterInitialLongitude = -0.34629; // (degrees) Longitude for simulations
	public static volatile double masterInitialYaw = 0.0; 			 // (rad) Initial heading of the master UAV for simulations
	public static volatile double altitude = 5;					 // (m) Relative altitude where the UAVs finish the take off process
	
	//Place the formations you want in this string
	public static Formation.Layout startLayout = Formation.Layout.LINEAR;
	public static Formation.Layout endLayout = Formation.Layout.CIRCLE;
	public static int minDistance = 10;
	public static String[] formations = {Formation.Layout.MATRIX.name()	};
	public static volatile TargetFormation[] flightFormations = null; // List of Flightformations 
	
	public static int NUMBER_OF_SECTORS = 3; 		 // number of sectors used to select the altitude
	public static int ALTITUDE_DIFF_SECTORS = 0;	 // (m) minimum difference in altitude between a section
	public static AssignmentAlgorithm.AssignmentAlgorithms TAKE_OFF_ALGORITHM = AssignmentAlgorithm.AssignmentAlgorithms.HEURISTIC;
	public static double ALTITUDE_MARGIN = 1;	 // (m) margin between intended altitude and current altitude before going to next state
	public static final double XY_MARGIN = 0.2;			 // (m) margine between intended xy position and current xy position before going to next state
	
	public static final long TIMEOUT = 250; 		// (ms) Waiting time in sending messages or reading threads
	public static final long SENDING_TIMEOUT = 200;	// (ms) Time between packets sent
	public static final long WAITING_TIME = 200;	// (ms) Time to wait to check for thread

	
	public static final short MESSAGE_ACK 	= 0;
	public static final short DATA 			= 1;
	public static final short MOVE_Z 		= 2;
	public static final short MOVE_XY 		= 3;
	public static final short MOVE_Z_INI 	= 4;
	public static final short LANDING 		= 5;

}

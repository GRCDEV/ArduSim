package com.protocols.vision.logic;


/** This class includes parameters specifically related to the communication with the flight controller.
 * <p>Developed by: Jamie Wubben, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class visionParam {

	// Parameters used to detect when a UAV reaches the last waypoint
	public static final double LAST_WP_THRESHOLD = 2.0; // (m) Maximum distance considered to assert that the UAV has reached the last waypoint
	public static final String PTYHON_SERVER_IP = "127.0.0.1";
	public enum status {LAND, DESCEND, MOVE, ROTATE, LOITER,RECOVER}

    //default place in the field
	//only used when the file location.ini is not found
	public static double ALTITUDE = 20.0; 
	public static double LATITUDE = 39.725319;
	public static double LONGITUDE = -0.733674;
		
}

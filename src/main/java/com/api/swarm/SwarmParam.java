package com.api.swarm;

import com.api.swarm.assignement.AssignmentAlgorithm;

public class SwarmParam {
    public static volatile String[] macAddresses = new String[] { "b8:27:eb:57:4c:0e", "b8:27:eb:02:19:5b" };// MACs of master (Hexacopter) with standard format
    public static volatile long[] macIDs = new long[] { 202481591602190L, 202481586018651L };// MACs of master with long format
    public static volatile int masterId = 0;

    public static double minimalTakeoffAltitude = 5;

    public static AssignmentAlgorithm.AssignmentAlgorithms assignmentAlgorithm;

    public static final int MSG_ASK_FOR_LOCATION = 1;
    public static final int MSG_LOCATION = 2;
    public static final int MSG_TAKEOFFLOC = 3;
    public static final int MSG_TAKEOFF = 4;
}

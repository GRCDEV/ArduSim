package com.api.swarm.takeoff;
import es.upv.grc.mapper.Location3DUTM;

import java.util.Map;

public abstract class TakeoffAlgorithm {

    public enum TakeoffAlgorithms{
        SEQUENTIAL,SEMI_SEQUENTIAL,SEMI_SIMULTANEOUS, SIMULTANEOUS
    }

    protected Map<Long, Location3DUTM> assignment;
    protected Location3DUTM targetLocation;
    protected int numUAV;
    public abstract void takeOff(int numUAV);

}

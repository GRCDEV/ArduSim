package com.api.swarm.takeoff;

import es.upv.grc.mapper.Location3DUTM;

import java.util.Map;

class semiSequential extends TakeoffAlgorithm {

    public semiSequential(Map<Long, Location3DUTM> assignment) {
        this.assignment = assignment;
    }

    @Override
    public void takeOff(int numUAV) {

    }
}

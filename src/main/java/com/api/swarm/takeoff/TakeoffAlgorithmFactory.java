package com.api.swarm.takeoff;

import es.upv.grc.mapper.Location3DUTM;

import java.util.Map;

public class TakeoffAlgorithmFactory {

    public static TakeoffAlgorithm newTakeoffAlgorithm(TakeoffAlgorithm.TakeoffAlgorithms algorithms,
                                                       Map<Long, Location3DUTM> assignment){
        switch (algorithms){
            case SEQUENTIAL:
                return new sequential(assignment);
            case SEMI_SEQUENTIAL:
                return new semiSequential(assignment);
            case SIMULTANEOUSLY:
                return new simultaneously(assignment);
            default:
                throw new IllegalArgumentException("take off algorithm not recognized");
        }
    }
}

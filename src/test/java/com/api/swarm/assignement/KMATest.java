package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

class KMATest {

    Map<Long, Location3DUTM> groundLocations = new HashMap<>();
    Map<Long, Location3DUTM> airLocations = new HashMap<>();
    KMA kma;
    @BeforeEach
    void setup(){
        setGroundLocations();
        setAirLocations();
        kma = new KMA(groundLocations,airLocations);
    }

    private void setGroundLocations() {
        groundLocations.put(0L, new Location3DUTM(728236,4373697,0));
        groundLocations.put(1L, new Location3DUTM(728246,4373697,0));
        groundLocations.put(2L, new Location3DUTM(728236,4373707,0));
        groundLocations.put(3L, new Location3DUTM(728226,4373697,0));
        groundLocations.put(4L, new Location3DUTM(728236,4373687,0));
    }

    private void setAirLocations() {
        airLocations.put(0L, new Location3DUTM(728216,4373697,10));
        airLocations.put(1L, new Location3DUTM(728226,4373697,10));
        airLocations.put(2L, new Location3DUTM(728236,4373697,10));
        airLocations.put(3L, new Location3DUTM(728246,4373697,10));
        airLocations.put(4L, new Location3DUTM(728256,4373697,10));
    }

    @Test
    void getAssignmentAlgorithm() {
        Assertions.assertEquals(AssignmentAlgorithm.AssignmentAlgorithms.KMA, kma.getAssignmentAlgorithm());
    }
}
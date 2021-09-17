package com.api.swarm.takeoff;

import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SemiSimultaneousTest {
    private static SemiSimultaneous d1,d2,d3,d4,d5;

    private static final Location3DUTM a0 = new Location3DUTM(728215.99,4373697.17,100);
    private static final Location3DUTM a1 = new Location3DUTM(728225.99,4373697.17,100);
    private static final Location3DUTM a2 = new Location3DUTM(728235.99,4373697.17,100);
    private static final Location3DUTM a3 = new Location3DUTM(728245.99,4373697.17,100);
    private static final Location3DUTM a4 = new Location3DUTM(728255.99,4373697.17,100);
    private static final Location3DUTM a5 = new Location3DUTM(728265.99,4373697.17,100);
    private static final Location3DUTM a6 = new Location3DUTM(728275.99,4373697.17,100);
    private static final Location3DUTM a7 = new Location3DUTM(728285.99,4373697.17,100);
    private static final Location3DUTM a8 = new Location3DUTM(728295.99,4373697.17,100);
    private static final Location3DUTM a9 = new Location3DUTM(728305.99,4373697.17,100);


    private static final Map<Long,Location3DUTM> assignment1 = new HashMap<>(){{
        put(0L,a0);put(1L,a1);put(2L,a2);put(3L,a3);put(4L,a4);
    }};

    private static final Map<Long,Location3DUTM> groundLocations1 = new HashMap<>(){{
        put(0L,new Location3DUTM(728215.99, 4373697.17,5));
        put(1L,new Location3DUTM(728225.99, 4373697.17,5));
        put(2L,new Location3DUTM(728235.99, 4373697.17,5));
        put(3L,new Location3DUTM(728245.99, 4373697.17,5));
        put(4L,new Location3DUTM(728255.99, 4373697.17,5));
    }};

    private static final Map<Long,Location3DUTM> assignment2 = new HashMap<>(){{
        put(0L,a0);put(1L,a3);put(2L,a1);put(3L,a2);put(4L,a4);
    }};


    private static final Map<Long,Location3DUTM> assignment3 = new HashMap<>(){{
        put(0L,a0);put(1L,a1);put(2L,a4);put(3L,a2);put(4L,a3);
    }};

    private static final Map<Long,Location3DUTM> assignment4 = new HashMap<>(){{
        put(0L,a0);put(1L,a1);put(2L,a2);put(3L,a4);put(4L,a3);
    }};

    private static final Map<Long,Location3DUTM> assignment5 = new HashMap<>(){{
        put(0L,a0);put(1L,a2);put(2L,a1);put(3L,a4);put(4L,a3);
        put(5L,a6);put(6L,a5);put(7L,a8);put(8L,a7);put(9L,a9);
    }};
    private static final Map<Long,Location3DUTM> groundLocations2 = new HashMap<>(){{
        putAll(groundLocations1);
        put(5L,new Location3DUTM(728265.99, 4373697.17,5));
        put(6L,new Location3DUTM(728275.99, 4373697.17,5));
        put(7L,new Location3DUTM(728285.99, 4373697.17,5));
        put(8L,new Location3DUTM(728295.99, 4373697.17,5));
        put(9L,new Location3DUTM(728305.99, 4373697.17,5));
    }};

    private static Stream<Arguments> CollisionDetectionTest(){
        d1 = new SemiSimultaneous(assignment1,groundLocations1);
        Set<Long> expected1 = new HashSet<>();
        d2 = new SemiSimultaneous(assignment2,groundLocations1);
        Set<Long> expected2 = new HashSet<>(){{add(1L);}};
        d3 = new SemiSimultaneous(assignment3,groundLocations1);
        Set<Long> expected3 = new HashSet<>(){{add(2L);}};
        d4 = new SemiSimultaneous(assignment4,groundLocations1);
        Set<Long> expected4 = new HashSet<>(){{add(3L);}};
        d5 = new SemiSimultaneous(assignment5,groundLocations2);
        Set<Long> expected5 = new HashSet<>(){{add(1L);add(3L);add(5L);add(7L);}};

        return Stream.of(
                Arguments.of(expected1, d1),
                Arguments.of(expected2, d2),
                Arguments.of(expected3, d3),
                Arguments.of(expected4, d4),
                Arguments.of(expected5, d5)
        );
    }

    @ParameterizedTest
    @MethodSource("CollisionDetectionTest")
    void detectCollision(Set<Long> expected, SemiSimultaneous d) {
        ArrayList<Quartet<Long,Location3DUTM, Long, Location3DUTM>> solution = d.detectCollision();
        Set<Long> sol = new HashSet<>();
        try{
            for (Quartet<Long, Location3DUTM, Long, Location3DUTM> objects : solution) {
                sol.add(objects.getValue0());
            }
            assertEquals(expected, sol);
        }catch (Exception e){
            assertEquals(expected,-1);
        }
    }
}
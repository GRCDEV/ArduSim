package com.api.swarm.assignement;

import es.upv.grc.mapper.Location3DUTM;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.api.swarm.assignement.AssignmentAlgorithm.AssignmentAlgorithms.*;
import static org.junit.jupiter.api.Assertions.*;

class AssignmentAlgorithmFactoryTest {

    /**
     * Test {@link AssignmentAlgorithmFactory#newAssignmentAlgorithm(AssignmentAlgorithm.AssignmentAlgorithms, Map, Map)}  }
     */
    @ParameterizedTest
    @EnumSource(AssignmentAlgorithm.AssignmentAlgorithms.class)
    void newFormation(AssignmentAlgorithm.AssignmentAlgorithms algo) {
        Map<Long, Location3DUTM> ground = new HashMap<>();
        Map<Long, Location3DUTM> air = new HashMap<>();
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,null,null));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,null));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,null,air));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,air));

        ground.put(1L,new Location3DUTM(1,1,1));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,null));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,air));
        air.put(1L,new Location3DUTM(1,1,1));
        ground.put(2L,new Location3DUTM(1,1,1));
        assertThrows(java.lang.IllegalArgumentException.class, () -> AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,air));

        air.put(2L,new Location3DUTM(1,1,1));
        AssignmentAlgorithm a = AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,air);
        assertNotNull(a);
        String algoName = a.getAssignmentAlgorithm().name();
        assertEquals(algo.name(),algoName);
    }

    /**
     * Test {@link AssignmentAlgorithm#calculateAssignment()}  }
     */
    @ParameterizedTest
    @EnumSource(AssignmentAlgorithm.AssignmentAlgorithms.class)
    void assignmentNotNull(AssignmentAlgorithm.AssignmentAlgorithms algo){
        Map<Long, Location3DUTM> ground = new HashMap<>();
        Map<Long, Location3DUTM> air = new HashMap<>();

        ground.put(0L,new Location3DUTM(728235.99,4373697.17,0.0));
        ground.put(1L,new Location3DUTM(728245.99,4373697.17,0.0));
        ground.put(2L,new Location3DUTM(728235.99,4373707.17,0.0));
        ground.put(3L,new Location3DUTM(728225.99,4373697.17,0.0));
        ground.put(4L,new Location3DUTM(728235.99,4373687.17,0.0));

        air.put(0L,new Location3DUTM(728235.99,4373697.17,10.0));
        air.put(1L,new Location3DUTM(728245.99,4373697.17,10.0));
        air.put(2L,new Location3DUTM(728235.99,4373707.17,10.0));
        air.put(3L,new Location3DUTM(728225.99,4373697.17,10.0));
        air.put(4L,new Location3DUTM(728235.99,4373687.17,10.0));
        AssignmentAlgorithm a = AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo,ground,air);
        a.calculateAssignment();
        assertNotNull(a.getAssignment());
    }

    private static Stream<Arguments> totalDistanceInput(){
        Map<Long, Location3DUTM> cross = Map.of(
                0L, new Location3DUTM(728236,4373697,0),
                1L, new Location3DUTM(728246,4373697,0),
                2L, new Location3DUTM(728236,4373707,0),
                3L, new Location3DUTM(728226,4373697,0),
                4L, new Location3DUTM(728236,4373687,0)
        );
        Map<Long, Location3DUTM> line = Map.of(
                0L, new Location3DUTM(728216,4373697,10),
                1L, new Location3DUTM(728226,4373697,10),
                2L, new Location3DUTM(728236,4373697,10),
                3L, new Location3DUTM(728246,4373697,10),
                4L, new Location3DUTM(728256,4373697,10)
        );

        Map<Long,Location3DUTM> circle = Map.of(
                0L, new Location3DUTM(728236,4373697,15),
                1L, new Location3DUTM(728251,4373697,15),
                2L, new Location3DUTM(728239,4373672,15),
                3L, new Location3DUTM(728209,4373699,15),
                4L, new Location3DUTM(728238,4373714,15)

        );

        return Stream.of(
                Arguments.of(cross, line),
                Arguments.of(line,cross),
                Arguments.of(cross,circle)
        );
    }

    @ParameterizedTest()
    @MethodSource("totalDistanceInput")
    void totalDistance(Map<Long,Location3DUTM> ground, Map<Long,Location3DUTM> air){
        Map<AssignmentAlgorithm.AssignmentAlgorithms,Float> totalDistanceMap = new HashMap<>();
        for(AssignmentAlgorithm.AssignmentAlgorithms algo: values()) {
            AssignmentAlgorithm a = AssignmentAlgorithmFactory.newAssignmentAlgorithm(algo, ground, air);
            a.calculateAssignment();
            totalDistanceMap.put(algo,a.getTotalDistanceSquared());
        }
        assertEquals(totalDistanceMap.get(KMA), totalDistanceMap.get(BRUTE_FORCE),"KMA and brute force should have the same total distance travelled");
        assertTrue(totalDistanceMap.get(KMA) <= totalDistanceMap.get(HEURISTIC), "The heurisitic cannot give a smaller total distance then the KMA");
        AssignmentAlgorithm random = AssignmentAlgorithmFactory.newAssignmentAlgorithm(RANDOM, ground, air);
        assertNotSame(totalDistanceMap.get(RANDOM),random.getTotalDistanceSquared(),"Using the random assignment twice should give a different answer");
    }
}
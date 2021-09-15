package com.api.swarm.formations;

import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.stream.Stream;

import static com.api.swarm.formations.Formation.Layout.LINEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@Link Linear}
 * @author Jamie Wubben
 */
class LinearTest {

    private final Formation formation = FormationFactory.newFormation(LINEAR);
    private static final double minDistance = 10;

    /**
     * Tests {@link Linear#getCenterIndex()}
     */
    @Test
    void testGetCenterIndex() {
        formation.init(6,minDistance);
        assertEquals(0,formation.getCenterIndex());

        formation.init(5,minDistance);
        assertEquals(0,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        //index is not tested to reuse formationPoints
        FormationPoint p1  = new FormationPoint(0, 0, 0,0);
        FormationPoint p2  = new FormationPoint(1, -minDistance, 0,0);
        FormationPoint p3  = new FormationPoint(2, minDistance, 0,0);
        FormationPoint p4  = new FormationPoint(2, -2*minDistance, 0,0);
        FormationPoint p5  = new FormationPoint(2, 2*minDistance, 0,0);


        return Stream.of(
                Arguments.of(1, new FormationPoint[]{p1}),
                Arguments.of(2, new FormationPoint[]{p1,p2}),
                Arguments.of(3, new FormationPoint[]{p1,p2,p3}),
                Arguments.of(4, new FormationPoint[]{p1,p2,p3,p4}),
                Arguments.of(5, new FormationPoint[]{p1,p2,p3,p4,p5})
        );
    }
    /**
     * Tests for {@link Linear#get3DUTMLocation(Location3DUTM, int)}
     * General Tests can be found in
     */
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void testCalculateFormation(int numUAVs, FormationPoint[] expected) {
        ArrayList<FormationPoint>  actual = formation.calculateFormation(numUAVs,minDistance);
        assertEquals(actual.size(), expected.length);
        for(int i = 0 ; i< actual.size();i++){
            assertEquals(expected[i].offsetX , actual.get(i).offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual.get(i).offsetY,"offsetY");
            assertEquals(expected[i].offsetZ , actual.get(i).offsetZ,"offsetZ");
        }
    }
}
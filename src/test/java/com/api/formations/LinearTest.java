package com.api.formations;

import es.upv.grc.mapper.Location2DUTM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.api.formations.Formation.Layout.LINEAR;
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
        // centerIndex = numUAVs/2 (integer division)

        formation.init(6,minDistance);
        assertEquals(3,formation.getCenterIndex());

        formation.init(5,minDistance);
        assertEquals(2,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        //index is not tested to reuse formationPoints
        FormationPoint p1  = new FormationPoint(0, 0, 0);
        FormationPoint p2  = new FormationPoint(1, -minDistance, 0);
        FormationPoint p3  = new FormationPoint(2, minDistance, 0);
        FormationPoint p4  = new FormationPoint(2, -2*minDistance, 0);
        FormationPoint p5  = new FormationPoint(2, 2*minDistance, 0);


        return Stream.of(
                Arguments.of(1, new FormationPoint[]{p1}),
                Arguments.of(2, new FormationPoint[]{p2,p1}),
                Arguments.of(3, new FormationPoint[]{p2,p1,p3}),
                Arguments.of(4, new FormationPoint[]{p4,p2,p1,p3}),
                Arguments.of(5, new FormationPoint[]{p4,p2,p1,p3,p5})
        );
    }
    /**
     * Tests for {@link Linear#get2DUTMLocation(Location2DUTM, int)}
     * General Tests can be found in
     */
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void testCalculateFormation(int numUAVs, FormationPoint[] expected) {
        FormationPoint[] actual = formation.calculateFormation(numUAVs,minDistance);
        assertEquals(actual.length, expected.length);
        for(int i = 0 ; i< actual.length;i++){
            assertEquals(expected[i].offsetX , actual[i].offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual[i].offsetY,"offsetY");
        }
    }
}
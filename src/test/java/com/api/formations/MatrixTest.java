package com.api.formations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.api.formations.Formation.Layout.MATRIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link Matrix}
 * @author Jamie Wubben
 */
class MatrixTest {

    private final Formation formation = FormationFactory.newFormation(MATRIX);
    private static final double minDistance = 10;

    /**
     * Test for {@link Matrix#getCenterIndex()}
     */
    @Test
    public void getCenterIndex() {
        assert formation != null;
        formation.init(5,minDistance);
        assertEquals(0,formation.getCenterIndex());
        formation.init(11,minDistance);
        assertEquals(0,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        FormationPoint p1  = new FormationPoint(0, 0, 0);
        FormationPoint p2  = new FormationPoint(1, minDistance, 0);
        FormationPoint p3  = new FormationPoint(2, 0,minDistance);
        FormationPoint p4  = new FormationPoint(3,-minDistance,0);
        FormationPoint p5  = new FormationPoint(4,0,-minDistance);

        FormationPoint p6  = new FormationPoint(5,minDistance,minDistance);
        FormationPoint p7  = new FormationPoint(6,-minDistance,minDistance);
        FormationPoint p8  = new FormationPoint(7,-minDistance,-minDistance);
        FormationPoint p9  = new FormationPoint(8, minDistance,-minDistance);

        FormationPoint p10 = new FormationPoint(9,2*minDistance,0);


        return Stream.of(
                Arguments.of(10, new FormationPoint[]{p1,p2,p3,p4,p5,p6,p7,p8,p9,p10})
        );
    }

    /**
     * Test for {@link Matrix#calculateFormation(int, double)}
     * @param numUAVs: number of UAVs
     * @param expected: expected array of formationPoints: comming form {@link #inputCalculateFormation()}
     */
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void calculateFormation(int numUAVs, FormationPoint[] expected) {
        assert formation != null;
        FormationPoint[] actual = formation.calculateFormation(numUAVs,minDistance);
        assertEquals(actual.length, expected.length);
        for(int i = 0 ; i< actual.length;i++){
            assertEquals(expected[i].offsetX , actual[i].offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual[i].offsetY,"offsetY");
        }
    }
}
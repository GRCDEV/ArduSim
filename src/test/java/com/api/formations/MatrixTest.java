package com.api.formations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.api.formations.Formation.Layout.MATRIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Matrix}
 * @author Jamie Wubben
 */
//TODO make the test correct and adjust the code
class MatrixTest {

    private final Formation formation = FormationFactory.newFormation(MATRIX);
    private static final double minDistance = 10;


    private static Stream<Arguments> inputGetCenterIndex(){
        //numUAVs, expectedCenterIndex
        return Stream.of(
                Arguments.of(1,0),
                Arguments.of(2,1),
                Arguments.of(3,1),
                Arguments.of(4,2),
                Arguments.of(5,1)
                );
    }
    @ParameterizedTest
    @MethodSource("inputGetCenterIndex")
    void getCenterIndex(int numUAVs, int expected) {
        formation.init(numUAVs,minDistance);
        assertEquals(expected,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        //index is not tested to reuse formationPoints
        FormationPoint p1  = new FormationPoint(0, 0, 0);
        FormationPoint p2  = new FormationPoint(1, minDistance, 0);
        FormationPoint p3  = new FormationPoint(2, 0,minDistance);
        FormationPoint p4  = new FormationPoint(3,-minDistance,0);
        FormationPoint p5  = new FormationPoint(4,0   ,-minDistance);
        FormationPoint p6  = new FormationPoint(5, minDistance, minDistance);
        FormationPoint p7  = new FormationPoint(6,-minDistance, minDistance);
        FormationPoint p8  = new FormationPoint(8,-minDistance,-minDistance);
        FormationPoint p9  = new FormationPoint(9, minDistance, minDistance);

        return Stream.of(
                Arguments.of(1, new FormationPoint[]{p1}),
                Arguments.of(2, new FormationPoint[]{p1,p2}),
                Arguments.of(3, new FormationPoint[]{p1,p2,p3})
                //Arguments.of(4, new FormationPoint[]{p4,p2,p3,p1})
                //Arguments.of(5, new FormationPoint[]{p1,p2,p5,p3,p4}),
                //Arguments.of(6, new FormationPoint[]{p1,p2,p5,p3,p4,p6}),
                //Arguments.of(7, new FormationPoint[]{p1,p2,p5,p3,p4,p6,p7}),
                //Arguments.of(8, new FormationPoint[]{p1,p2,p5,p3,p4,p6,p7,p8}),
                //Arguments.of(9, new FormationPoint[]{p1,p2,p5,p3,p4,p6,p7,p8,p9}),
        );
    }
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void calculateFormation(int numUAVs, FormationPoint[] expected) {
        assert formation != null;
        FormationPoint[] actual = formation.calculateFormation(numUAVs,minDistance);
        assertEquals(actual.length, expected.length);
        for(int i = 0 ; i< actual.length;i++){
            System.out.print("expected: " + expected[i].position + ";" + expected[i].offsetX + ";" + expected[i].offsetY + "\t");
            System.out.println("actual: " + actual[i].position + ";" + actual[i].offsetX + ";" + actual[i].offsetY);
            //assertEquals(expected[i].position, actual[i].position,"position");
            assertEquals(expected[i].offsetX , actual[i].offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual[i].offsetY,"offsetY");
        }

    }
}
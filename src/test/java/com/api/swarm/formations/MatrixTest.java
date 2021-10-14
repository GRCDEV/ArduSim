package com.api.swarm.formations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.stream.Stream;

import static com.api.swarm.formations.Formation.Layout.MATRIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        formation.init(5,minDistance);
        assertEquals(0,formation.getCenterIndex());
        formation.init(11,minDistance);
        assertEquals(0,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        FormationPoint p1  = new FormationPoint(0, 0, 0,0);
        FormationPoint p2  = new FormationPoint(1, -minDistance, 0,0);
        FormationPoint p3  = new FormationPoint(2, 0,-minDistance,0);
        FormationPoint p4  = new FormationPoint(3,0,minDistance,0);
        FormationPoint p5  = new FormationPoint(4,minDistance,0,0);

        FormationPoint p6  = new FormationPoint(5,minDistance,minDistance,0);
        FormationPoint p7  = new FormationPoint(6,-minDistance,-minDistance,0);
        FormationPoint p8  = new FormationPoint(7,-minDistance,minDistance,0);
        FormationPoint p9  = new FormationPoint(8, minDistance,-minDistance,0);

        FormationPoint p10 = new FormationPoint(9,0,-2*minDistance,0);


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
    void calculateFormation(int numUAVs, FormationPoint[] expected) { ;
        ArrayList<FormationPoint> actual = formation.calculateFormation(numUAVs,minDistance);
        assertTrue(actual.size() >= numUAVs);
        for(int i = 0 ; i< expected.length;i++){
            assertEquals(expected[i].offsetX , actual.get(i).offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual.get(i).offsetY,"offsetY");
            assertEquals(expected[i].offsetZ , actual.get(i).offsetZ,"offsetZ");
        }
    }

    @Test
    void printLocations(){
        ArrayList<FormationPoint> actual = formation.calculateFormation(11,10);
        for(FormationPoint p:actual){
            System.out.println(p.offsetX + "," + p.offsetY + ",50");
        }
    }

    @Test
    void checkMinDistance(){
        ArrayList<FormationPoint> actual = formation.calculateFormation(2000,minDistance);
        for(int i=0;i<actual.size();i++){
            for(int j=i+1;j< actual.size();j++){
                FormationPoint p1 = actual.get(i);
                FormationPoint p2 = actual.get(j);
                double x = p1.offsetX - p2.offsetX;
                double y = p1.offsetY-p2.offsetY;
                double dist = Math.sqrt((Math.pow(x,2) + Math.pow(y,2) ));
                assertTrue(dist >= minDistance);
            }
        }
    }

}
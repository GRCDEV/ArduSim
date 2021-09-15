package com.api.swarm.formations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.stream.Stream;

import static com.api.swarm.formations.Formation.Layout.RANDOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Random}
 * @author Jamie Wubben
 */
class RandomTest {

    private final Formation formation = FormationFactory.newFormation(RANDOM);

    /**
     * Test for {@link Random#getCenterIndex()}
     */
    @Test
    void getCenterIndex() {
        formation.init(12,35);
        assertEquals(0,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        return Stream.of(
                Arguments.of(10,10),
                Arguments.of(168,10),
                Arguments.of(37,18.9),
                Arguments.of(46,5.7)
        );
    }

    /**
     * Test for {@link Random#calculateFormation(int, double)}
     * Checks if minDistances is guaranteed
     * @param numUAVs: number of UAVs
     * @param minDistance: minDistance between UAVs
     */
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void calculateFormation(int numUAVs, double minDistance) {
        ArrayList<FormationPoint> points = formation.calculateFormation(numUAVs,minDistance);

        for(int i = 0;i< points.size();i++){
            double x_i = points.get(i).offsetX;
            double y_i = points.get(i).offsetY;
            double z_i = points.get(i).offsetY;

            for(int j = i+1;j<points.size();j++){
                double x_j = points.get(j).offsetX;
                double y_j = points.get(j).offsetY;
                double z_j = points.get(j).offsetZ;

                double distance = Math.sqrt(Math.pow(x_i-x_j,2) + Math.pow(y_i-y_j,2)+ Math.pow(z_i-z_j,2));
                assertTrue(distance >= minDistance,"minDistance not guaranteed." +
                        " Distance between UAV " + i + " and " + j + " = " + distance);
            }
        }

    }
}
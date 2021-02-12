package com.api.formations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.api.formations.Formation.Layout.CIRCLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link Circle}
 * @Author Jamie Wubben
 */
class CircleTest {

    private final Formation formation = FormationFactory.newFormation(CIRCLE);

    /**
     * Tests {@link Circle#getCenterIndex()}
     */
    @Test
    void getCenterIndex() {
        // centerIndex = 0
        assert formation != null;
        formation.init(10,10);
        assertEquals(0,formation.getCenterIndex());

        formation.init(5,10);
        assertEquals(0,formation.getCenterIndex());
    }

    private static Stream<Arguments> inputCalculateFormation(){
        FormationPoint p1 = new FormationPoint(0, 0, 0);
        FormationPoint p2 = new FormationPoint(1, 10, 0);
        FormationPoint p3 = new FormationPoint(2, -5,8.660254);
        FormationPoint p4 = new FormationPoint(3,-5,-8.660254);

        FormationPoint p5 = new FormationPoint(5,-10,0);
        FormationPoint p6 = new FormationPoint(6,0,10);
        FormationPoint p7 = new FormationPoint(7,0,-10);

        return Stream.of(
                Arguments.of(4, new FormationPoint[]{p1,p2,p3,p4}),
                Arguments.of(5, new FormationPoint[]{p1,p2,p6,p5,p7})
        );
    }

    /**
     * Tests{@link Circle#calculateFormation(int, double)}
     */
    @ParameterizedTest
    @MethodSource("inputCalculateFormation")
    void calculateFormation(int numUAVs, FormationPoint[] expected) {
        assert formation != null;
        FormationPoint[] actual = formation.calculateFormation(numUAVs,10);
        assertEquals(actual.length, expected.length);
        for(int i = 0 ; i< actual.length;i++){
            assertEquals(expected[i].offsetX , actual[i].offsetX,"offsetX");
            assertEquals(expected[i].offsetY , actual[i].offsetY,"offsetY");
        }
    }
}
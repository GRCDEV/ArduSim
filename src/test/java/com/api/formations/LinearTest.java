package com.api.formations;

import es.upv.grc.mapper.Location2DUTM;
import org.junit.jupiter.api.Test;

import static com.api.formations.Formation.Layout.LINEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@Link Linear}
 * @author Jamie Wubben
 */
class LinearTest {

    private final Formation formation = FormationFactory.newFormation(LINEAR);

    /**
     * Tests {@link Linear#getCenterIndex()}
     */
    @Test
    void testGetCenterIndex() {
        // centerIndex = numUAVs/2 (integer division)

        formation.init(6,10);
        assertEquals(3,formation.getCenterIndex());

        formation.init(5,10);
        assertEquals(2,formation.getCenterIndex());
    }

    /**
     * Specific Tests for {@link Linear#get2DUTMLocation(Location2DUTM, int)}
     * General Tests can be found in
     */
    @Test
    void testCalculateFormation() {
        formation.init(5,10);
        Location2DUTM centralLocation = new Location2DUTM(39.725064, -0.733661);

        testCenterUAVLocation(centralLocation);
        testFirstUAVLocation(centralLocation);
        testlastUAVLocation(centralLocation);
    }

    private void testCenterUAVLocation(Location2DUTM centralLocation) {
        Location2DUTM actual = formation.get2DUTMLocation(centralLocation,2);
        assertEquals(centralLocation,actual);
    }

    private void testFirstUAVLocation(Location2DUTM centralLocation) {
        Location2DUTM actual;
        double x = centralLocation.x - (2*10);
        double y = centralLocation.y;
        Location2DUTM expected = new Location2DUTM(x,y);
        actual = formation.get2DUTMLocation(centralLocation,0);
        assertEquals(expected,actual);
    }

    private void testlastUAVLocation(Location2DUTM centralLocation) {
        Location2DUTM actual;
        double x = centralLocation.x + (2*10);
        double y = centralLocation.y;
        Location2DUTM expected = new Location2DUTM(x,y);
        actual = formation.get2DUTMLocation(centralLocation,4);
        assertEquals(expected,actual);
    }
}
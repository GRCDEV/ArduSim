package com.api.swarm.formations;

import es.upv.grc.mapper.Location3DUTM;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Formation}
 * @Author Jamie Wubben
 */
class FormationTest {

    /**
     * Tests {@link Formation#getLayout()}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void getLayout(Formation.Layout layout) {
        Formation formation = FormationFactory.newFormation(layout);
        Formation.Layout l = formation.getLayout();
        assertEquals(l.name(), layout.name());
    }

    /**
     * Tests {@link Formation#init(int, double)}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void init(Formation.Layout layout) {
        Formation formation = FormationFactory.newFormation(layout);
        // Error case: invalid numUAVs
        assertThrows(java.lang.Error.class, () -> formation.init(0, 10));

        // Error case: invalid minDistance
        assertThrows(java.lang.Error.class, () -> formation.init(5, 0));

        // Error case: invalid numUAVs and minDistance
        assertThrows(java.lang.Error.class, () -> formation.init(0, 0));
    }

    /**
     * Tests {@link Formation#get3DUTMLocation(Location3DUTM, int)}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void get2DUTMLocation(Formation.Layout layout) {
        Location3DUTM centralLocation = new Location3DUTM(39.725064, -0.733661,0);
        Formation formation = FormationFactory.newFormation(layout);
        formation.init(5,10);

        // Error case: index out of bound (lower end)
        assertThrows(java.lang.Error.class, () -> formation.get3DUTMLocation(centralLocation, -1));
        // Error case: index out of bound (upper end)
        assertThrows(java.lang.Error.class, () -> formation.get3DUTMLocation(centralLocation, 5));
        // Error case: centerlocation is null
        assertThrows(java.lang.Error.class, () -> formation.get3DUTMLocation(null, 2));
        // Error case: centerlocation null and index out of bound
        assertThrows(java.lang.Error.class, () -> formation.get3DUTMLocation(null, 5));
        // correct case: UTMLocation is not null
        assertNotNull(formation.get3DUTMLocation(centralLocation,3));
    }

    /**
     * Tests {@link Formation#getNumUAVs()}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void getNumUAVs(Formation.Layout layout) {
        Formation formation = FormationFactory.newFormation(layout);
        // Error case: not initialized
        assertThrows(java.lang.Error.class, formation::getNumUAVs);

        // general case
        int expected = 6;
        formation.init(expected, 10);
        assertEquals(expected, formation.getNumUAVs());
    }
}
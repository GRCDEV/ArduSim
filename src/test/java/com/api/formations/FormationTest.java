package com.api.formations;

import es.upv.grc.mapper.Location2DUTM;
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
        assert formation != null;
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
        assertThrows(java.lang.Error.class, () -> {
            assert formation != null;
            formation.init(0, 10);
        });

        // Error case: invalid minDistance
        assertThrows(java.lang.Error.class, () -> {
            assert formation != null;
            formation.init(5, 0);
        });

        // Error case: invalid numUAVs and minDistance
        assertThrows(java.lang.Error.class, () -> {
            assert formation != null;
            formation.init(0, 0);
        });
    }

    /**
     * Tests {@link Formation#get2DUTMLocation(Location2DUTM, int)}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void get2DUTMLocation(Formation.Layout layout) {
        Location2DUTM centralLocation = new Location2DUTM(39.725064, -0.733661);
        Formation formation = FormationFactory.newFormation(layout);
        formation.init(5,10);

        // Error case: index out of bound (lower end)
        assertThrows(java.lang.Error.class, () -> formation.get2DUTMLocation(centralLocation, -1));
        // Error case: index out of bound (upper end)
        assertThrows(java.lang.Error.class, () -> formation.get2DUTMLocation(centralLocation, 5));
        // Error case: centerlocation is null
        assertThrows(java.lang.Error.class, () -> formation.get2DUTMLocation(null, 2));
        // Error case: centerlocation null and index out of bound
        assertThrows(java.lang.Error.class, () -> formation.get2DUTMLocation(null, 5));
        // correct case: UTMLocation is not null
        assertNotNull(formation.get2DUTMLocation(centralLocation,3));
    }

    /**
     * Tests {@link Formation#getNumUAVs()}
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void getNumUAVs(Formation.Layout layout) {
        Formation formation = FormationFactory.newFormation(layout);
        // Error case: not initialized
        assert formation != null;
        assertThrows(java.lang.Error.class, formation::getNumUAVs);

        // general case
        int expected = 6;
        formation.init(expected, 10);
        assertEquals(expected, formation.getNumUAVs());
    }
}
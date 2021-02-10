package test.main.api.formations;

import es.upv.grc.mapper.Location2DUTM;
import main.api.formations.Formation;
import main.api.formations.FormationFactory;
import org.junit.jupiter.api.Test;

import static main.api.formations.Formation.Layout.LINEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for {@link main.api.formations.Linear}
 * @Author Jamie Wubben
 */
public class Linear {

    private final Formation formation = FormationFactory.newFormation(LINEAR);

    /**
     * Tests {@link main.api.formations.Linear#getLayout()}
     */
    @Test
    public void testGetLayout(){
        assert formation != null;
        Formation.Layout l = formation.getLayout();
        assertEquals(l.name(), LINEAR.name());
    }

    /**
     * Tests {@link main.api.formations.Linear#get2DUTMLocation(Location2DUTM, int)}
     */
    @Test
    public void testGet2DUTMLocation(){
        formation.init(5,10);
        Location2DUTM centralLocation = new Location2DUTM(39.725064, -0.733661);

        // central UAV should be at the same place (0 indexed)
        Location2DUTM actual = formation.get2DUTMLocation(centralLocation,2);
        assertEquals(centralLocation,actual);

        // first UAV should be on: x - (2*10)
        double x = centralLocation.x - (2*10);
        double y = centralLocation.y;
        Location2DUTM expected = new Location2DUTM(x,y);
        actual = formation.get2DUTMLocation(centralLocation,0);
        assertEquals(expected,actual);

        // last UAV should be on: x + (2*10)
        x = centralLocation.x + (2*10);
        y = centralLocation.y;
        expected = new Location2DUTM(x,y);
        actual = formation.get2DUTMLocation(centralLocation,4);
        assertEquals(expected,actual);

        // index out of bound (lower end)
        actual = formation.get2DUTMLocation(centralLocation,-1);
        assertNull(actual);

        //index out of bound (upper end)
        actual = formation.get2DUTMLocation(centralLocation,5);
        assertNull(actual);
    }

    /**
     * Tests {@link main.api.formations.Linear#getCenterIndex()}
     */
    @Test
    public void testGetCenterIndex(){
        formation.init(6,10);
        // centerIndex = numUAVs/2 (integer division)

        int expected = 3;
        int actual = formation.getCenterIndex();
        assertEquals(expected,actual);

        formation.init(5,10);
        expected = 2;
        actual = formation.getCenterIndex();
        assertEquals(expected,actual);
    }

    /**
     * Tests {@link main.api.formations.Linear#getNumUAVs()}
     */
    @Test
    public void testGetNumUAVs(){
        int expected = 6;
        formation.init(expected,10);

        int actual = formation.getNumUAVs();
        assertEquals(expected,actual);
    }
}

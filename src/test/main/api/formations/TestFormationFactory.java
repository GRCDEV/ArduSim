package test.main.api.formations;

import main.api.formations.Formation;
import main.api.formations.FormationFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test Class for {@link main.api.formations.FormationFactory}
 */
public class TestFormationFactory {

    /**
     * Test {@link main.api.formations.FormationFactory#newFormation(Formation.Layout)} }
     */
    @Test
    public void testNewFormation(){
        for(Formation.Layout layout: Formation.Layout.values()){
            Formation f = FormationFactory.newFormation(layout);
            assertNotNull(f);
            String name = f.getLayout().name();
            assertEquals(name,layout.name());
        }

    }
}

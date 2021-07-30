package com.api.swarm.formations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link FormationFactory}
 * @Author Jamie Wubben
 */
class FormationFactoryTest {

    /**
     * Test {@link FormationFactory#newFormation(Formation.Layout)} }
     */
    @ParameterizedTest
    @EnumSource(Formation.Layout.class)
    void newFormation(Formation.Layout layout) {
        Formation f = FormationFactory.newFormation(layout);
        assertNotNull(f);
        String name = f.getLayout().name();
        assertEquals(name,layout.name());
    }
}
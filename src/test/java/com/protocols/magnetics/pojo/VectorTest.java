package com.protocols.magnetics.pojo;

import com.protocols.magnetics.gui.MagneticsSimProperties;
import es.upv.grc.mapper.Location3DUTM;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class VectorTest {

    Location3DUTM l1 = new Location3DUTM(1,1,1);
    Location3DUTM l2 = new Location3DUTM(3,3,3);
    Vector v1 = new Vector(l1,l2);
    Vector v2 = new Vector(l2,l1);

    @Test
    void create(){
        String resultingVector = v1.toString();
        String expected = "{2.0;2.0;2.0}";
        assertEquals(expected,resultingVector);
    }

    @Test
    void add(){
        v1.add(v2);
        String resultingVector = v1.toString();
        String expected = "{0.0;0.0;0.0}";
        assertEquals(expected,resultingVector);
    }

    @Test
    void staticAdd(){
        Vector vn = Vector.add(v1,v2);
        String resultingVector = vn.toString();
        String expected = "{0.0;0.0;0.0}";
        assertEquals(expected,resultingVector);
    }

    @Test
    void subtract(){
        v1.subtract(v2);
        String resultingVector = v1.toString();
        String expected = "{4.0;4.0;4.0}";
        assertEquals(expected,resultingVector);
    }

    @Test
    void scalar(){
        v1.scalarProduct(2);
        String resultingVector = v1.toString();
        String expected = "{4.0;4.0;4.0}";
        assertEquals(expected,resultingVector);
    }

    @Test
    void magnitude(){
        double magnitude = v2.magnitude();
        assertEquals(Math.sqrt(12),magnitude);
    }

    @Test
    void normalize(){
        v1.normalize();
        assertEquals(1,v1.magnitude());
    }
}
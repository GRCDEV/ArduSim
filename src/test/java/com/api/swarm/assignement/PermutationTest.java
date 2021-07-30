package com.api.swarm.assignement;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class PermutationTest {

    @Test
    void permutations(){
        Integer[] array = new Integer[]{1,2,3};
        Integer[][] expected = {{1,2,3},{1,3,2},{2,1,3},{2,3,1},{3,2,1},{3,1,2}};

        Permutation<Integer> p = new Permutation<>(array);

        assertEquals(BigInteger.valueOf(6),p.size());
        int i=0;
        boolean executed = false;
        Integer[] permutation = p.next();
        while(permutation != null){
            executed = true;
            for(int j=0;j<permutation.length;j++){
                assertEquals(expected[i][j],permutation[j]);
            }
            i++;
            permutation = p.next();
        }
        assertTrue(executed);
    }

}
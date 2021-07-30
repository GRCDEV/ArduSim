package com.api.swarm.assignement;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.stream.Stream;
import com.api.swarm.assignement.KMAAlgorithm.Element;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


class KMAAlgorithmTest {

    private static Stream<Arguments> getDataToTestSolve(){
        // examples that must work and give this and only this as a result
        // Small matrix that can be solved by hand
        KMAAlgorithm smallMatrix = new KMAAlgorithm(3);
        float[][] smallCostMatrix = {{25,40,35},{40,60,35},{20,40,25}};
        smallMatrix.fillWithCostmatrix(smallCostMatrix);
        Element[][] expectedResultSmallMatrix = {{null, Element.STAR, null},{null, null, Element.STAR},{Element.STAR, null, null}};

        // medium matrix
        KMAAlgorithm mediumMatrix = new KMAAlgorithm(6);
        float[][] mediumCostMatrix = {
                {5722.480469f, 29090.666016f, 28889.136719f, 39922.804688f, 14188.624023f, 44626.484375f},
                {5634.038574f, 32107.908203f, 26194.085938f, 42228.714844f, 13035.842773f, 48570.703125f},
                {6462.551758f, 33658.625000f, 23241.507812f, 42128.628906f, 13462.201172f, 52694.136719f},
                {7820.348145f, 33017.222656f, 21412.951172f, 39669.375000f, 15268.202148f, 55067.386719f},
                {9072.101562f, 30483.814453f, 21564.013672f, 36001.664062f, 17608.798828f, 54579.976562f},
                {9632.101562f, 27243.816406f, 23624.017578f, 32841.664062f, 19388.798828f, 51459.984375f},
        };
        mediumMatrix.fillWithCostmatrix(mediumCostMatrix);
        Element[][] expectedResultMediumMatrix = {
                {null,null,null,null,null,Element.STAR},
                {Element.STAR,null,null,null,null,null},
                {null,null,null,null,Element.STAR,null},
                {null,null,Element.STAR,null,null,null},
                {null,null,null,Element.STAR,null,null},
                {null,Element.STAR,null,null,null,null}
        };

        // Large matrix
        KMAAlgorithm largeMatrix = new KMAAlgorithm(10);
        float[][] largeCostMatrix = {
                {5722.480469f, 29090.666016f, 28889.136719f, 39922.804688f, 14188.624023f, 44626.484375f, 7267.435059f, 61925.132812f, 57436.230469f, 52831.816406f },
                {5634.038574f, 32107.908203f, 26194.085938f, 42228.714844f, 13035.842773f, 48570.703125f, 7464.423828f, 57552.042969f, 53035.984375f, 57181.007812f },
                {6462.551758f, 33658.625000f, 23241.507812f, 42128.628906f, 13462.201172f, 52694.136719f, 8677.384766f, 53132.007812f, 48998.242188f, 61615.683594f },
                {7820.348145f, 33017.222656f, 21412.951172f, 39669.375000f, 15268.202148f, 55067.386719f, 10338.758789f, 50733.210938f, 47212.308594f, 64060.816406f },
                {9072.101562f, 30483.814453f, 21564.013672f, 36001.664062f, 17608.798828f, 54579.976562f, 11671.172852f, 51478.074219f, 48513.847656f, 63372.300781f },
                {9632.101562f, 27243.816406f, 23624.017578f, 32841.664062f, 19388.798828f, 51459.984375f, 12051.172852f, 55018.078125f, 52293.839844f, 59872.300781f },
                {9238.318359f, 24813.253906f, 26629.054688f, 31667.972656f, 19775.322266f, 47167.269531f, 11300.952148f, 59696.808594f, 56783.605469f, 55198.503906f },
                {8075.007324f, 24329.421875f, 29173.042969f, 33029.769531f, 18587.507812f, 43710.460938f, 9771.551758f, 63325.031250f, 59882.316406f, 51537.832031f },
                {6686.494629f, 26018.699219f, 30065.621094f, 36289.859375f, 16381.148438f, 42707.027344f, 8178.589355f, 64205.070312f, 60140.062500f, 50603.152344f },
                {5722.480469f, 29090.666016f, 28889.136719f, 39922.804688f, 14188.624023f, 44626.484375f, 7267.435059f, 61925.132812f, 57436.230469f, 52831.816406f}
        };
        largeMatrix.fillWithCostmatrix(largeCostMatrix);
        Element[][] expectedResultLargeMatrix = {
                {null,null,null,null,null,null,Element.STAR,null,null,null},
                {null,null,null,null,Element.STAR,null,null,null,null,null},
                {null,null,null,null,null,null,null,null,Element.STAR,null},
                {null,null,null,null,null,null,null,Element.STAR,null,null},
                {null,null,Element.STAR,null,null,null,null,null,null,null},
                {null,null,null,Element.STAR,null,null,null,null,null,null},
                {null,Element.STAR,null,null,null,null,null,null,null,null},
                {null,null,null,null,null,null,null,null,null,Element.STAR},
                {null,null,null,null,null,Element.STAR,null,null,null,null},
                {Element.STAR,null,null,null,null,null,null,null,null,null}
        };

        return Stream.of(
                Arguments.of(smallMatrix,expectedResultSmallMatrix),
                Arguments.of(mediumMatrix,expectedResultMediumMatrix),
                Arguments.of(largeMatrix,expectedResultLargeMatrix)
        );
    }
    @ParameterizedTest
    @MethodSource("getDataToTestSolve")
    void solve(KMAAlgorithm m, Element[][] expected) {
        m.solve();
        Element[][] solution = m.getMask();
        for(int i=0;i<expected.length;i++) {
            assertArrayEquals(expected[i],solution[i]);
        }
    }

    private static Stream<Arguments> getDataToTestFillWithCostMatrix() {
        return Stream.of(
                Arguments.of(new float[][] {{1,2,3},{4,5,6}},false),
                Arguments.of(new float[][] {{1,2},{2,3},{4,5}},false),
                Arguments.of(new float[][] {{1,2,3,4},{-5,6,7,8},{1,2,3,4},{5,6,7,8}},false),
                Arguments.of(new float[][] {{1,2,3},{4,5,6},{7,8,9}},false),
                Arguments.of(new float[][] {{1,2,3,4},{5,6,7,8},{1,2,3,4},{5,6,7,8}},true)
        );
    }
    @ParameterizedTest
    @MethodSource("getDataToTestFillWithCostMatrix")
    void fillWithCostmatrix(float[][] costMatrix, boolean expected) {
        KMAAlgorithm m = new KMAAlgorithm(4);
        boolean actual = m.fillWithCostmatrix(costMatrix);
        assertEquals(expected,actual);
    }

    @Test
    void getAssignment() {
        KMAAlgorithm smallMatrix = new KMAAlgorithm(3);
        float[][] smallCostMatrix = {{25,40,35},{40,60,35},{20,40,25}};
        smallMatrix.fillWithCostmatrix(smallCostMatrix);
        smallMatrix.solve();
        ArrayList<Pair<Long,Long>> actual = smallMatrix.getAssignment();
        assertEquals(0, actual.get(0).getValue0());
        assertEquals(1, actual.get(0).getValue1());

        assertEquals(1, actual.get(1).getValue0());
        assertEquals(2, actual.get(1).getValue1());

        assertEquals(2, actual.get(2).getValue0());
        assertEquals(0, actual.get(2).getValue1());
    }
}
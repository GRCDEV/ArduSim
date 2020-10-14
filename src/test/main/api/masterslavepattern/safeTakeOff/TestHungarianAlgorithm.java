package test.main.api.masterslavepattern.safeTakeOff;


import main.api.masterslavepattern.safeTakeOff.HungarianAlgorithm;
import main.api.masterslavepattern.safeTakeOff.HungarianAlgorithm.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHungarianAlgorithm {

    private static Stream<Arguments> getDataToTestFillWithCostMatrix() {
        return Stream.of(
                Arguments.of(new double[][] {{1,2,3},{4,5,6}},false),
                Arguments.of(new double[][] {{1,2},{2,3},{4,5}},false),
                Arguments.of(new double[][] {{1,2,3,4},{-5,6,7,8},{1,2,3,4},{5,6,7,8}},false),
                Arguments.of(new double[][] {{1,2,3},{4,5,6},{7,8,9}},false),
                Arguments.of(new double[][] {{1,2,3,4},{5,6,7,8},{1,2,3,4},{5,6,7,8}},true)
        );
    }
    @ParameterizedTest
    @MethodSource("getDataToTestFillWithCostMatrix")
    public void testFillWithCostMatrix(double[][] costMatrix, boolean expected) {
        HungarianAlgorithm m = new HungarianAlgorithm(4);
        boolean actual = m.fillWithCostmatrix(costMatrix);
        assertEquals(expected,actual);
    }
    private static Stream<Arguments> getDataToTestSolve(){
        // examples that must work and give this and only this as a result
        // Small matrix that can be solved by hand
        HungarianAlgorithm smallMatrix = new HungarianAlgorithm(3);
        double[][] smallCostMatrix = {{25,40,35},{40,60,35},{20,40,25}};
        smallMatrix.fillWithCostmatrix(smallCostMatrix);
        Element[][] expectedResultSmallMatrix = {{null, Element.STAR, null},{null, null, Element.STAR},{Element.STAR, null, null}};

        // medium matrix
        HungarianAlgorithm mediumMatrix = new HungarianAlgorithm(6);
        double[][] mediumCostMatrix = {
                {5722.480469, 29090.666016, 28889.136719, 39922.804688, 14188.624023, 44626.484375},
                {5634.038574, 32107.908203, 26194.085938, 42228.714844, 13035.842773, 48570.703125},
                {6462.551758, 33658.625000, 23241.507812, 42128.628906, 13462.201172, 52694.136719},
                {7820.348145, 33017.222656, 21412.951172, 39669.375000, 15268.202148, 55067.386719},
                {9072.101562, 30483.814453, 21564.013672, 36001.664062, 17608.798828, 54579.976562},
                {9632.101562, 27243.816406, 23624.017578, 32841.664062, 19388.798828, 51459.984375},
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
        HungarianAlgorithm largeMatrix = new HungarianAlgorithm(10);
        double[][] largeCostMatrix = {
                {5722.480469, 29090.666016, 28889.136719, 39922.804688, 14188.624023, 44626.484375, 7267.435059, 61925.132812, 57436.230469, 52831.816406 },
                {5634.038574, 32107.908203, 26194.085938, 42228.714844, 13035.842773, 48570.703125, 7464.423828, 57552.042969, 53035.984375, 57181.007812 },
                {6462.551758, 33658.625000, 23241.507812, 42128.628906, 13462.201172, 52694.136719, 8677.384766, 53132.007812, 48998.242188, 61615.683594 },
                {7820.348145, 33017.222656, 21412.951172, 39669.375000, 15268.202148, 55067.386719, 10338.758789, 50733.210938, 47212.308594, 64060.816406 },
                {9072.101562, 30483.814453, 21564.013672, 36001.664062, 17608.798828, 54579.976562, 11671.172852, 51478.074219, 48513.847656, 63372.300781 },
                {9632.101562, 27243.816406, 23624.017578, 32841.664062, 19388.798828, 51459.984375, 12051.172852, 55018.078125, 52293.839844, 59872.300781 },
                {9238.318359, 24813.253906, 26629.054688, 31667.972656, 19775.322266, 47167.269531, 11300.952148, 59696.808594, 56783.605469, 55198.503906 },
                {8075.007324, 24329.421875, 29173.042969, 33029.769531, 18587.507812, 43710.460938, 9771.551758, 63325.031250, 59882.316406, 51537.832031 },
                {6686.494629, 26018.699219, 30065.621094, 36289.859375, 16381.148438, 42707.027344, 8178.589355, 64205.070312, 60140.062500, 50603.152344 },
                {5722.480469, 29090.666016, 28889.136719, 39922.804688, 14188.624023, 44626.484375, 7267.435059, 61925.132812, 57436.230469, 52831.816406}
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
    public void testSolve(HungarianAlgorithm m, Element[][] expected) {
        m.solve();
        Element[][] solution = m.getMask();
        for(int i=0;i<expected.length;i++) {
            assertArrayEquals(expected[i],solution[i]);
        }
    }
}

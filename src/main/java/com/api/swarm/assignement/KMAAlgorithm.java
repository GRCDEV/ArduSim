package com.api.swarm.assignement;

import org.javatuples.Pair;

import java.util.ArrayList;

public class KMAAlgorithm {

    int dimension;
    double[][] m;
    double[][] originalCostMatrix;
    public enum Element {STAR,PRIME}
    Element[][] mask;
    boolean[] coveredColumns;
    boolean[] coveredRows;

    public KMAAlgorithm(int dimension) {
        this.dimension = dimension;
        m = new double[dimension][dimension];

        originalCostMatrix = new double[dimension][dimension];
        mask = new Element[dimension][dimension];
        coveredColumns = new boolean[dimension];
        coveredRows = new boolean[dimension];
    }

    public void solve() {
        rowReduce();
        initMask();
        initializeCoverColumn();
        coverColumnsOfStarredZeroes();
    }
    private void rowReduce() {
        for(int row=0;row<dimension;row++) {
            double smallestElement = getSmallestElementRow(row);
            subtractElementFromRow(row,smallestElement);
        }
    }
    private void initMask() {
        ArrayList<Pair<Integer,Integer>> zeros = getAllZeros();
        for (Pair<Integer, Integer> p : zeros) {
            int row = p.getValue0();
            int column = p.getValue1();
            if (!existStarInRowOrColumn(row, column)) {
                mask[row][column] = Element.STAR;
            }
        }
    }
    private void initializeCoverColumn() {
        for(int column = 0;column<dimension;column++) {
            coveredColumns[column] = existStarInColumn(column);
        }
    }
    private void coverColumnsOfStarredZeroes() {
        initializeCoverColumn();
        if(getNumberOfColumnsCovered() != dimension){
            primeSomeUncoveredZero();
        }
    }
    private void primeSomeUncoveredZero() {
        Pair<Integer,Integer> z = findFirstNonCoveredZero();
        while(z != null) {
            int row = z.getValue0();
            int column = z.getValue1();
            mask[row][column] = Element.PRIME;
            if(!existStarInRow(row)) {
                incrementSetOfStarredZeroes(z);
                return;
            }else {
                Pair<Integer,Integer> p = findFirstStarInRow(row);
                row = p.getValue0();
                column = p.getValue1();
                coveredRows[row]= true;
                coveredColumns[column] = false;
                z = findFirstNonCoveredZero();
            }
        }
        makeMoreZeroes();
    }
    private void incrementSetOfStarredZeroes(Pair<Integer,Integer> z0) {
        ArrayList<Pair<Integer,Integer>> primesOfSeries = new ArrayList<>();
        ArrayList<Pair<Integer,Integer>> starsOfSeries = new ArrayList<>();

        constructAlternatingPrimesAndStaredZeros(z0, primesOfSeries, starsOfSeries);
        unstarEachStarredZeroInSeries(starsOfSeries);
        starAllPrimesInSeries(primesOfSeries);
        mask[z0.getValue0()][z0.getValue1()] = Element.STAR;
        removeAllPrimes();
        uncoverAll();
        coverColumnsOfStarredZeroes();
    }

    private void constructAlternatingPrimesAndStaredZeros(Pair<Integer,Integer> z0, ArrayList<Pair<Integer,Integer>> primesOfSeries,
                                                          ArrayList<Pair<Integer,Integer>> starsOfSeries) {
        int row = z0.getValue0();
        int column = z0.getValue1();
        while(existStarInColumn(column)) {
            Pair<Integer,Integer> star = findFirstStarInColumn(column);
            starsOfSeries.add(star);
            row = star.getValue0();
            Pair<Integer,Integer>prime = findFirstPrimeInRow(row);
            primesOfSeries.add(prime);
            column = prime.getValue1();
        }
    }

    private void makeMoreZeroes() {
        double h = getSmallestNonCoveredElement();
        addElementToCoveredRows(h);
        subtractElementFromUncoveredColumn(h);
        primeSomeUncoveredZero();
    }

    public boolean fillWithCostmatrix(float[][] costMatrix) {
        boolean isSquare = checkSquareMatrix(costMatrix);
        if(!isSquare) {
            System.err.println("costMatrix has to be square");
            return false;
        }

        int dimensionCostMatrix = costMatrix.length;
        if(this.dimension != dimensionCostMatrix) {
            System.err.println("costMatrix has to have the same dimension as the internal matrix");
            return false;}

        boolean isPositive = checkPositive(costMatrix);
        if(!isPositive) {
            System.err.println("costMatrix has to be positive");
            return false;}

        fillMatrix(costMatrix);
        return true;
    }
    private boolean checkSquareMatrix(float[][] costMatrix) {
        int rows = costMatrix.length;
        for (float[] matrix : costMatrix) {
            int columns = matrix.length;
            if (columns != rows) {
                return false;
            }
        }
        return true;
    }
    private boolean checkPositive(float[][] costMatrix) {
        for (float[] matrix : costMatrix) {
            for (float v : matrix) {
                if (v < 0) {
                    return false;
                }
            }
        }
        return true;
    }
    private void fillMatrix(float[][] costMatrix) {
        for(int i=0;i<dimension;i++) {
            for(int j=0;j<dimension;j++) {
                m[i][j] = costMatrix[i][j];
                originalCostMatrix[i][j] = costMatrix[i][j];
            }
        }
    }

    private boolean existStarInRowOrColumn(int row, int column) {
        return existStarInRow(row) || existStarInColumn(column);
    }
    private boolean existStarInColumn(int column) {
        for(int row = 0; row < dimension;row++) {
            if(mask[row][column] == Element.STAR) {
                return true;
            }
        }
        return false;
    }
    private boolean existStarInRow(int row) {
        for(int column = 0; column < dimension;column++) {
            if(mask[row][column] == Element.STAR) {
                return true;
            }
        }
        return false;
    }
    private Pair<Integer,Integer> findFirstStarInColumn(int column) {
        for(int row = 0; row<dimension;row++) {
            if(mask[row][column] == Element.STAR) {
                return new Pair<>(row,column);
            }
        }
        return null;
    }
    private Pair<Integer,Integer> findFirstStarInRow(int row) {
        for(int column = 0; column<dimension;column++) {
            if(mask[row][column] == Element.STAR) {
                return new Pair<>(row,column);
            }
        }
        return null;
    }


    private ArrayList<Pair<Integer,Integer>> getAllZeros() {
        ArrayList<Pair<Integer,Integer>> zeros = new ArrayList<>();
        for(int row = 0;row<dimension;row++) {
            for(int column = 0;column<dimension;column++) {
                if(m[row][column] == 0) {
                    zeros.add(new Pair<>(row,column));
                }
            }
        }
        return zeros;
    }
    private Pair<Integer,Integer> findFirstNonCoveredZero() {
        for(int row = 0;row<dimension;row++) {
            if(!coveredRows[row]) {
                for(int column = 0;column<dimension;column++) {
                    if(!coveredColumns[column]) {
                        if(m[row][column] == 0) {
                            return new Pair<>(row,column);
                        }
                    }
                }
            }
        }
        return null;
    }
    private void unstarEachStarredZeroInSeries(ArrayList<Pair<Integer,Integer>> starsOfSeries) {
        for (Pair<Integer, Integer> p : starsOfSeries) {
            mask[p.getValue0()][p.getValue1()] = null;
        }
    }

    private Pair<Integer,Integer> findFirstPrimeInRow(int row) {
        for(int column = 0; column< dimension; column++) {
            if(mask[row][column] == Element.PRIME) {
                return new Pair<>(row,column);
            }
        }
        System.err.println("there should always be a prime in the row");
        return null;
    }
    private void starAllPrimesInSeries(ArrayList<Pair<Integer,Integer>> primesOfSeries) {
        for (Pair<Integer, Integer> p : primesOfSeries) {
            mask[p.getValue0()][p.getValue1()] = Element.STAR;
        }
    }
    private void removeAllPrimes() {
        int row;
        int column;
        for(row = 0;row<dimension;row++) {
            for(column = 0; column<dimension;column++) {
                if(mask[row][column]==Element.PRIME) {
                    mask[row][column]=null;
                }
            }
        }
    }

    private double getSmallestElementRow(int row) {
        double smallestElement = Double.MAX_VALUE;
        for(int j=0;j<dimension;j++) {
            if(m[row][j] < smallestElement) {
                smallestElement = m[row][j];
            }
        }
        return smallestElement;
    }
    private double getSmallestNonCoveredElement() {
        double smallestElement = Double.MAX_VALUE;
        for(int row = 0;row<dimension;row++) {
            if(!coveredRows[row]) {
                for(int column = 0;column<dimension;column++) {
                    if(!coveredColumns[column]) {
                        if(m[row][column] < smallestElement && m[row][column] >=0) {
                            smallestElement = m[row][column];
                        }
                    }
                }
            }
        }
        return smallestElement;
    }
    private void subtractElementFromRow(int row, double element) {
        for(int column=0;column<dimension;column++) {
            m[row][column] -= element;
        }
    }
    private void subtractElementFromUncoveredColumn(double element) {
        for(int column = 0;column<dimension;column++) {
            if(!coveredColumns[column]) {
                for(int row=0;row<dimension;row++) {
                    m[row][column] -= element;
                }
            }
        }
    }
    private void addElementToCoveredRows(double element) {
        for(int row = 0;row<dimension;row++) {
            if(coveredRows[row]) {
                for(int column=0;column<dimension;column++) {
                    m[row][column] += element;
                }
            }
        }
    }

    private int getNumberOfColumnsCovered() {
        int nr = 0;
        for(int i = 0;i<dimension;i++) {
            if(coveredColumns[i]) {
                nr++;
            }
        }
        return nr;
    }
    private void uncoverAll() {
        coveredColumns = new boolean[dimension];
        coveredRows = new boolean[dimension];
    }

    public  Element[][] getMask() {
        return mask;
    }

    public ArrayList<Pair<Long,Long>> getAssignment(){
        ArrayList<Pair<Long,Long>> assignment = new ArrayList<>();
        for(int row = 0; row< dimension; row++){
            Pair<Integer,Integer> p = findFirstStarInRow(row);
            assert p != null;
            long groundId = p.getValue0();
            long airId = p.getValue1();
            Pair<Long,Long> longPair = new Pair<>(groundId,airId);
            assignment.add(longPair);
        }
        return assignment;
    }
}

package com.protocols.topography.logic;

import es.upv.grc.mapper.Location2DUTM;

import java.io.BufferedReader;
import java.io.FileReader;

public class DEM {

    private Location2DUTM origin;
    private int cellSize;
    private float NO_DATA_VALUE;
    private float[][] dem;

    public DEM(String pathToDEM) {

        //Read the DEM file
        try {
            BufferedReader br =new BufferedReader(new FileReader(pathToDEM));

            //Retrieve DEM parameters
            int nCols = Integer.parseInt(br.readLine().split("\\s+")[1]);
            int nRows = Integer.parseInt(br.readLine().split("\\s+")[1]);
            int originX = Integer.parseInt(br.readLine().split("\\s+")[1]);
            int originY = Integer.parseInt(br.readLine().split("\\s+")[1]);
            cellSize = Integer.parseInt(br.readLine().split("\\s+")[1]);
            NO_DATA_VALUE = Float.parseFloat(br.readLine().split("\\s+")[1]);
            dem = new float[nRows][nCols];
            origin = new Location2DUTM(originX, originY);

            //Fill dem[][] with the actual DEM
            int tab = nRows - 1;
            String line = br.readLine();
            while (line != null && !line.isBlank() && !line.isEmpty()) {

                //String[] to float[] algorithm
                String[] rowS = line.split("\\s+");
                float[] rowF = new float[rowS.length];

                for(int i = 0; i < rowS.length; i++) {
                    float f = Float.parseFloat(rowS[i]);
                    if(f == NO_DATA_VALUE) { f = 0; }
                    rowF[i] = f;
                }

                dem[tab] = rowF;
                tab--;
                line = br.readLine();
            }

            br.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * This method retrieves the real altitude of a given location
     *
     * @param location Location to retrieve the height
     *
     * @return The height in meters from the ground
     */
    public float getRealAltitude(Location2DUTM location) {

        return dem[(int) ((location.getY() - origin.getY())/ cellSize)][(int) ((location.getX() - origin.getX())/ cellSize)];
    }

    /**
     * This method retrieves the real altitude of a given location
     *
     * @param x X axis of UTM location
     * @param y Y axis of UTM location
     *
     * @return The height in meters from the ground
     */
    public float getRealAltitude(double x, double y) {

        return dem[(int) ((y - origin.getY())/ cellSize)][(int) ((x - origin.getX())/ cellSize)];
    }

    public Location2DUTM getOrigin() { return origin; }
    public int getCellSize() { return cellSize; }
    public float getNO_DATA_VALUE() { return NO_DATA_VALUE; }
    public float[][] getDem() { return dem; }
}

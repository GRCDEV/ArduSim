package com.api.formations;

import es.upv.grc.mapper.Location2DUTM;

/**
 * Parent class for all Formations
 * Tests for this class can be found
 * @Author Jamie Wubben
 */
public abstract class Formation {

    /**
     * All possible layouts
     */
    public enum Layout {
        LINEAR,MATRIX,CIRCLE,RANDOM
    }

    /**
     * Array that gives the offset for each UAV in the formation
     * This does not give the (real) UTM location only the offset
     */
    protected FormationPoint[] positions;

    /**
     * @return A specifc layout from the Enum Layout
     */
    public abstract Layout getLayout();

    /**
     * Used to initialize the formation. Has to be used before the other methods
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: minimal (guaranteed) distance between the UAVs
     */
    public void init(int numUAVs, double minDistance){
        if(numUAVs < 1 | minDistance <= 0){
            throw new Error("Input parameters invalid");
        }
        this.positions = calculateFormation(numUAVs, minDistance);
    }

    /**
     * calculates the (real) 2DUTM location of the UAV in a formation
     * @param centerLocation: 2DUTM coordinates of the center of the formation
     * @param index: index of the UAV (in {@link #positions}, 0-based
     * @return 2DUTM location
     */
    public Location2DUTM get2DUTMLocation(Location2DUTM centerLocation, int index){
        if(index < 0 | index >= positions.length | centerLocation == null){
            throw new Error("input parameters invalid");
        }
        double x = centerLocation.x + positions[index].offsetX;
        double y = centerLocation.y + positions[index].offsetY;
        return new Location2DUTM(x,y);
    }

    /**
     * @return index of the UAV in the center of the formation
     */
    public abstract int getCenterIndex();

    /**
     * @return number of UAVs in the formation
     */
    public int getNumUAVs(){
        if(positions == null){
            throw new Error("Formation is not initialized");
        }
        return positions.length;
    }

    /**
     * used to calculate the offsets of the UAVs in the formation w.r.t the center UAV
     * Each subclass has to implement it's own specific behaviour
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: minimum guaranteed distance between the UAVs
     * @return array of formationPoints which has the offset (in doubles not real coordinates) w.r.t the center
     */
    protected abstract FormationPoint[] calculateFormation(int numUAVs, double minDistance);
}

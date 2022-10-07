package com.api.swarm.formations;
import es.upv.grc.mapper.Location3DUTM;

import java.util.ArrayList;

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
        LINEAR,MATRIX,CIRCLE, CIRCLE2, RANDOM
    }

    /**
     * Array that gives the offset for each UAV in the formation
     * This does not give the (real) UTM location only the offset
     */
    protected ArrayList<FormationPoint> positions;

    /**
     * Relative altitude of the formation (in case of 3D formation altitude of centerUAV)
     */
    protected double altitude;

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
        if(numUAVs < 1 | minDistance <= 0 | altitude < 0){
            throw new Error("Input parameters invalid");
        }
        this.positions = calculateFormation(numUAVs, minDistance);
    }

    /**
     * calculates the (real) 3DUTM location of the UAV in a formation
     * @param centerLocation: 3DUTM coordinates of the center of the formation
     * @param index: index of the UAV (in {@link #positions}, 0-based
     * @return 3DUTM location
     */
    public Location3DUTM get3DUTMLocation(Location3DUTM centerLocation, int index){
        if(index < 0 | index >= positions.size() | centerLocation == null){
            throw new Error("input parameters invalid");
        }
        double x = centerLocation.x + positions.get(index).offsetX;
        double y = centerLocation.y + positions.get(index).offsetY;
        double z = centerLocation.z + positions.get(index).offsetZ;
        return new Location3DUTM(x,y,z);
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
        return positions.size();
    }

    /**
     * used to calculate the offsets of the UAVs in the formation w.r.t the center UAV
     * Each subclass has to implement it's own specific behaviour
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: minimum guaranteed distance between the UAVs
     * @return array of formationPoints which has the offset (in doubles not real coordinates) w.r.t the center
     */
    protected abstract ArrayList<FormationPoint> calculateFormation(int numUAVs, double minDistance);
}

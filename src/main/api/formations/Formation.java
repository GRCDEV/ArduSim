package main.api.formations;

import es.upv.grc.mapper.Location2DUTM;

/**
 * Abstract class for all Formations
 * @Author Jamie Wubben
 */
public abstract class Formation {

    /**
     * All possible layouts
     */
    public enum Layout {
        LINEAR,REGULAR_MATRIX,COMPACT_MATRIX,CIRCLE,COMPACT_MESH,RANDOM,SPLITUP
    }

    /**
     * Array that gives the offset for each UAV in the formation
     * This does not give the (real) UTM location only the offset
     */
    protected FormationPoint[] positions;

    /**
     * @return Specifc layout
     */
    public abstract Layout getLayout();

    /**
     * Used to calculate the formation. Has to be used before the other parameters
     * @param numUAVs: number of UAVs in the formation
     * @param minDistance: minimal (guaranteed) distance between the UAVs
     */
    public abstract void init(int numUAVs, double minDistance);

    /**
     * calculates the (real) 2DUTM location of the UAV in a formation
     * @param centerLocation: 2DUTM coordinates of the center of the formation
     * @param index: index of the UAV (in {@link #positions}, 0-based
     * @return 2DUTM location
     */
    public abstract Location2DUTM get2DUTMLocation(Location2DUTM centerLocation, int index);

    /**
     * @return index of the UAV in the center of the formation
     */
    public abstract int getCenterIndex();

    /**
     * @return number of UAVs in the formation
     */
    public abstract int getNumUAVs();
}

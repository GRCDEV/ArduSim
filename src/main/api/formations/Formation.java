package main.api.formations;

import es.upv.grc.mapper.Location2DUTM;

public abstract class Formation {

    public enum Layout {
        LINEAR,REGULAR_MATRIX,COMPACT_MATRIX,CIRCLE,COMPACT_MESH,RANDOM,SPLITUP
    };
    protected FormationPoint[] positions;
    public abstract Layout getLayout();
    public abstract void init(int numUAVs, double minDistance);
    public abstract Location2DUTM get2DUTMLocation(Location2DUTM centerLocation, int index);
    public abstract int getCenterIndex();
    public abstract int getNumUAVs();
}

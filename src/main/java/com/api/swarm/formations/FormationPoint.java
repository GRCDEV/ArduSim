package com.api.swarm.formations;

class FormationPoint {


    public int position;
    public double offsetX;
    public double offsetY;
    public double offsetZ;

    /**
     * Build an object for a point in the formation.
     * @param position Position of the UAV in the formation.
     * @param offsetX Offset of the UAV in x axes from the center UAV.
     * @param offsetY Offset of the UAV in y axes from the center UAV.
     */
    public FormationPoint(int position, double offsetX, double offsetY, double offsetZ) {
        this.position = position;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public String toString(){
        return position + ":{" + offsetX + ";" + offsetY + ";" + offsetZ + "}";
    }
}

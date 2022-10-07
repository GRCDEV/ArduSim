package com.api.swarm.formations;

/**
 * Class to obtain a specific Formation.
 * Exists as part of the the Factory design pattern.
 */
public class FormationFactory {

    /**
     * Static method to get a formation. Should be the only entrance to obtain a formation.
     * @param formation: the enum of the formation
     * @return a formation in case the enum was valid, null otherwise
     */
    public static Formation newFormation(Formation.Layout formation){
        switch (formation){
            case LINEAR:
                return new Linear();
            case MATRIX:
                return new Matrix();
            case CIRCLE:
                return new Circle();
            case RANDOM:
                return new Random();
            case CIRCLE2:
                return new Circle2();
            default:
                throw new IllegalStateException("Unexpected value: " + formation);
        }
    }
}

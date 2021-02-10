package main.api.formations;

/**
 * Class to obtain a specific Formation.
 * Exists as part of the the Factory design pattern.
 * See tests in {@link test.main.api.formations.TestFormationFactory}
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
            default:
                return null;
        }
    }
}

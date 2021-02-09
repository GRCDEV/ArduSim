package main.api.formations;

public class FormationFactory {

    public static Formation newFormation(Formation.Layout f){
        switch (f){
            case LINEAR:
                return new Linear();
            default:
                return  null;
        }
    }
}

package muscop.logic;

import api.API;
import api.pojo.location.Waypoint;
import es.upv.grc.mapper.Location3DUTM;
import main.ArduSimTools;
import main.Text;
import main.api.FlightFormationTools;
import main.api.MissionHelper;
import main.api.formations.FlightFormation;
import main.api.masterslavepattern.safeTakeOff.TakeOffAlgorithm;
import org.javatuples.Pair;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MuscopSimProperties {

    // GUI parameters
    public static File missionFile;
    public static FlightFormation.Formation groundFormation;
    public static int numberOfClusters;
    public static double groundMinDistance;
    public static TakeOffAlgorithm takeOffStrategy;
    public static FlightFormation.Formation flyingFormation;
    public static double flyingMinDistance;
    public static double landingMinDistance;

    // Timeouts
    public static int RECEIVING_TIMEOUT;			// (ms) The port is unlocked after this time when receiving messages
    public static long SENDING_TIMEOUT;			// (ms) Time between packets sent
    public static long LAND_CHECK_TIMEOUT;		// (ms) Between checks if the UAV has landed
    public static long STATE_CHANGE_TIMEOUT; 	// (ms) Waiting time in sending messages or reading threads
    public static long MISSION_TIMEOUT;		// (ms) Timeout to wait after the mission is received and the master UAV changes its state
    public static long TTL;					// (ms) Time to life for a UAV.
    public static int RECEIVETIMEOUT;			// (ms) Timeout for link.receiveMessage()
    // Maximum number of waypoints that fit in a datagram.
    public static int MAX_WAYPOINTS;	// main.api.CommLink.DATAGRAM_MAX_LENGTH - 2 - 2 - 3x8xn >= 0

    // (rad) Master UAV yaw or heading (first mission segment orientation in simulation).
    public static volatile double formationYaw;
    // Mission sent from master to slaves (center mission).
    public static AtomicReference<Location3DUTM[]> missionSent = new AtomicReference<>();// Array containing the mission sent to the slaves

    public static AtomicInteger[] state;


    public boolean storeParameters(Properties guiParams, ResourceBundle fileParams){
        // First check if there are parameters set in the file who are not accessed by the gui
        Properties parameters = new Properties();
        // the file always consist of all the parameters but sometimes the value could be different because it is set in the GUI
        for(String key :fileParams.keySet()){
            if(guiParams.containsKey(key)){
                String guiValue = guiParams.getProperty(key);
                parameters.setProperty(key,guiValue);
            }else{
                String fileValue = fileParams.getString(key);
                parameters.setProperty(key,fileValue);
            }
        }
        Iterator<Object> itr = parameters.keySet().iterator();

        // get all the fields in this class
        Field[] variables = this.getClass().getFields();
        Map<String,Field> variablesDict = new HashMap<>();
        for(Field var:variables){variablesDict.put(var.getName(),var);}

        // loop through all the parameters in the file
        while(itr.hasNext()){
            String key = itr.next().toString();
            String value = parameters.getProperty(key);
            if(!variablesDict.containsKey(key)){
                continue;
            }
            Field var = variablesDict.get(key);
            // set the value of the variable
            try {
                String type = var.getType().toString();
                if(type.equals("int")){
                    var.setInt(this,Integer.parseInt(value));
                }else if(type.equals("double")){
                    var.setDouble(this,Double.parseDouble(value));
                }else if(type.equals("long")){
                    var.setLong(this,Long.parseLong(value));
                }else if(type.contains("java.lang.String")){
                    var.set(this,value);
                }else if(type.contains("java.io.File")) {
                    var.set(this, new File(value));
                }else if(type.contains("FlightFormation")){
                    var.set(this,FlightFormation.Formation.getFormation(value));
                }else if(type.contains("TakeOffAlgorithm")){
                    var.set(this,TakeOffAlgorithm.getAlgorithm(value));
                }else{
                    ArduSimTools.warnGlobal(Text.LOADING_ERROR, Text.ERROR_STORE_PARAMETERS + type);
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        if(specificCheckVariables()){
            setSimulationParameters();
            return true;
        }else{
            return false;
        }
    }
    private boolean specificCheckVariables(){
        if(groundMinDistance<0){return false;}
        if(flyingMinDistance<0){return false;}
        if(landingMinDistance<0){return false;}
        if(!missionFile.exists()){return false;}
        return true;}

    private void setSimulationParameters(){
        File[] fileArray = {missionFile};
        storeMissionFile(fileArray);
        FlightFormationTools f = API.getFlightFormationTools();
        f.setGroundFormation(groundFormation.getName(),groundMinDistance);
        API.getCopter(0).getSafeTakeOffHelper().setTakeOffAlgorithm(takeOffStrategy.getName());
        f.setFlyingFormation(flyingFormation.getName(),flyingMinDistance);
        f.setLandingFormationMinimumDistance(landingMinDistance);
    }

    public void storeMissionFile(File[] selection) {
        // TODO check why paco used a file array instead of a single file
        final Pair<String, List<Waypoint>[]> missions = API.getGUI(0).loadMissions(selection);
        MissionHelper missionHelper = API.getCopter(0).getMissionHelper();
        if (missions == null) {
            missionHelper.setMissionsLoaded(null);
        } else {
            int numUAVs = API.getArduSim().getNumUAVs();
            // The master is assigned the first mission in the list
            List<Waypoint>[] missionsFinal = new ArrayList[numUAVs];
            // The master UAV is always in the position 0 of arrays
            missionsFinal[0] = missions.getValue1()[0];
            missionHelper.setMissionsLoaded(missionsFinal);
        }
    }
}

package com.protocols.topography.logic;

import com.api.API;
import com.api.copter.Copter;
import com.api.pojo.FlightMode;
import com.api.pojo.location.Waypoint;
import com.protocols.topography.gui.TopographySimProperties;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class DroneThread extends Thread{

    private final int numUAV;
    private final DEM dem;
    private final int demCellSize;
    private final CyclicBarrier barrier;
    private final Copter copter;
    private final double plannedSpeed;
    private final float initialTerrainLevel;
    private final double lookAheadDistance;

    DroneThread(int numUAV, DEM dem, CyclicBarrier barrier){
        this.numUAV = numUAV;
        this.dem = dem;
        this.demCellSize = dem.getCellSize();
        this.barrier = barrier;
        this.copter = API.getCopter(numUAV);
        this.plannedSpeed = copter.getPlannedSpeed();
        this.initialTerrainLevel = dem.getRealAltitude(copter.getLocationUTM());
        this.lookAheadDistance = demCellSize*TopographySimProperties.lookAhead;

    }
    @Override
    public void run(){
        copter.setFlightMode(FlightMode.GUIDED);
        List<Waypoint> wps = copter.getMissionHelper().getMissionsLoaded()[0];
        for(int index = 2; index < wps.size(); index++) {
            Waypoint wp = getWaypoint(wps, index);
            try {
                goToWp(wp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //waitAtWp();
        }
        API.getGUI((int) copter.getID()).logUAV("Done");
    }

    private Waypoint getWaypoint(List<Waypoint> wps, int index) {
        Waypoint wp = wps.get(index);
        API.getGUI((int) copter.getID()).logUAV("Moving to WP: " + (index -1));
        wp.setAltitude(10); //distance between ground and UAV
        return wp;
    }

    private void goToWp(Waypoint wp) throws IOException {
        double distance, old_error=0;
        double totalError=0;
        double maxError=0;
        long n=0;

        float kp = TopographySimProperties.kp+numUAV;
        float kd = TopographySimProperties.kd;

        do{
            if(copter.getAltitudeRelative() < 0.1){
                break;
            }
            Location3DUTM lookAhead = getLookAhead(wp);
            Location3DUTM uav3d = new Location3DUTM(copter.getLocationUTM(), initialTerrainLevel + copter.getAltitudeRelative());

            double desiredAltitude = lookAhead.z;
            double uavAltitude = uav3d.z;
            double error = desiredAltitude - uavAltitude;

            if(error>maxError){
                maxError = error;
            }
            n++;

            //double groundLevel = desiredAltitude-wp.getAltitude();
            //double error2 = uavAltitude - dem.getRealAltitude(copter.getLocationUTM()) -10;
            //fb.write( groundLevel+ "," + desiredAltitude + "," + uavAltitude + "," + error2 +"\n");

            double diff = error-old_error;
            old_error = error;
            totalError +=error;
            double sum = error*kp + diff*kd;


            double moveX = lookAhead.x - uav3d.x;
            double moveY = lookAhead.y - uav3d.y;
            double max = Math.max(moveX,moveY);
            moveX = moveX/max;
            moveY = moveY/max;

            copter.moveTo(moveY*plannedSpeed, moveX*plannedSpeed,-sum);

            API.getArduSim().sleep(200);
            distance = wp.getUTM().distance(copter.getLocationUTM());
        }while(distance > 50);

        FileWriter fb = new FileWriter("parametersOptimalization.csv",true);
        //fb.write("kp,kd,lookAhead,totalError,avgError,maxError\n"); 20m
        fb.write(kp+","+kd+","+lookAheadDistance+","+ totalError+","+totalError/n+","+maxError+"\n");
        fb.flush();
        fb.close();
        copter.moveTo(0, 0, 0);
        copter.land();
    }


    private Location3DUTM getLookAhead(Waypoint wp) {
        Location2DUTM wp2D = wp.getUTM();
        Vector2D copter2D = new Vector2D(copter.getLocationUTM().x, copter.getLocationUTM().y);
        Vector2D dir = new Vector2D(wp2D.x - copter2D.x, wp2D.y - copter2D.y);
        dir.normalize();
        Vector2D lookAheadPoint = copter2D.getAdded(dir);

        while(lookAheadPoint.distance(copter2D) < lookAheadDistance){
            lookAheadPoint.add(dir);
        }

        float alt = dem.getRealAltitude(lookAheadPoint.x, lookAheadPoint.y);
        return new Location3DUTM(lookAheadPoint.x,lookAheadPoint.y,alt + wp.getAltitude());
    }

    private void waitAtWp() {
        copter.moveTo(0, 0, 0);
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
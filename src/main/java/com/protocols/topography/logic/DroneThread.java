package com.protocols.topography.logic;

import com.api.API;
import com.api.copter.Copter;
import com.api.pojo.FlightMode;
import com.api.pojo.location.Waypoint;
import com.protocols.topography.gui.TopographySimProperties;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class DroneThread extends Thread{

    private final int numUAV;
    private final DEM dem;
    private final int demCellSize;
    private final CyclicBarrier barrier;
    private final Copter copter;
    private final float initialTerrainLevel;
    private final double lookAheadDistance;
    public static final double desiredAltitude = 10;
    private long startTimeFlight;

    DroneThread(int numUAV, DEM dem, CyclicBarrier barrier){
        this.numUAV = numUAV;
        this.dem = dem;
        this.demCellSize = dem.getCellSize();
        this.barrier = barrier;
        this.copter = API.getCopter(numUAV);
        this.initialTerrainLevel = dem.getRealAltitude(copter.getLocationUTM());
        this.lookAheadDistance = demCellSize*TopographySimProperties.lookAhead;
    }
    @Override
    public void run(){
        copter.setFlightMode(FlightMode.GUIDED);
        impulseResonse();
        /*
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
         */
        API.getGUI((int) copter.getID()).logUAV("Done");
    }

    private void impulseResonse() {
        try {
            FileWriter fb = new FileWriter("/home/jamiewubben/Downloads/pidExperiment/new.csv");
            fb.write("time,altitude,setpoint\n");

            API.getArduSim().sleep(3000);
            long start = System.currentTimeMillis();
            PID pid = new PID(TopographySimProperties.kp,TopographySimProperties.kd,0,0.2);

            double setpoint = 10;
            double now;
            double maxAltitude = 0, startRiseTime =0,endRiseTime=0;
            do{
                now = (System.currentTimeMillis() - start)/1000f;

                double altitude = copter.getAltitude();
                if(altitude > maxAltitude){
                    maxAltitude = altitude;
                }

                if(percentage(altitude) > 0.1 && startRiseTime==0){
                    startRiseTime=now;
                }else if(percentage(altitude) > 0.9 && endRiseTime==0 ){
                    endRiseTime= now;
                }


                double error = setpoint - altitude;

                fb.write(now + "," + percentage(altitude) + "," + percentage(setpoint) + "\n");


                double z_speed = pid.calculateOutput(error);
                copter.moveTo(0,0,-z_speed);

                API.getArduSim().sleep(200);

                if(now > 5){
                    setpoint = 20;
                }

            }while(now<25);
            fb.flush();
            fb.close();
            System.out.println("overshoot = " + percentage(maxAltitude));
            System.out.println("riseTime = " + (endRiseTime - startRiseTime));
        }catch(IOException e){
            e.printStackTrace();
        }
        copter.land();
    }

    private double percentage(double input){
        double max = 20;
        double min = 10;
        return (input-min)/(max-min);
    }

    private Waypoint getWaypoint(List<Waypoint> wps, int index) {
        Waypoint wp = wps.get(index);
        API.getGUI((int) copter.getID()).logUAV("Moving to WP: " + (index -1));
        wp.setAltitude(desiredAltitude); //distance between ground and UAV
        return wp;
    }

    private void goToWp(Waypoint wp) throws IOException {
        PID pid = new PID(TopographySimProperties.kp,TopographySimProperties.kd,0.0,200);
        String filename = "/home/jamiewubben/Downloads/pidExperiment/rural/retest.csv";
        FileWriter fb = new FileWriter(filename);
        fb.write("time,groundlevel,realAltitude,diff,error,batteryW,hspeed,zspeed\n");
        startTimeFlight = System.currentTimeMillis();
        do{
            double error = getError(wp);
            double zSpeed = pid.calculateOutput(error);
            if(zSpeed > 4.9){
                moveOnlyVertical(zSpeed);
            }else{
                moveTowardsTarget(wp, zSpeed);
            }
            log(fb);
            API.getArduSim().sleep(200);
        }while(wp.getUTM().distance(copter.getLocationUTM()) > 50);
        fb.close();
        copter.moveTo(0, 0, 0);
        copter.land();
    }

    private void log(FileWriter fb) throws IOException {
        double groundLevel = dem.getRealAltitude(copter.getLocationUTM());
        double droneAltitude = initialTerrainLevel + copter.getAltitudeRelative();
        double diff = droneAltitude - groundLevel;
        double error = diff - desiredAltitude;
        long time = System.currentTimeMillis() - startTimeFlight;
        double watts = UAVParam.uavCurrentStatus[numUAV].getVoltage() * UAVParam.uavCurrentStatus[numUAV].getCurrent();
        fb.write(time + "," + groundLevel + "," + droneAltitude+ "," + diff + "," + error + ","
                + watts + "," + copter.getHorizontalSpeed() + "," + copter.getSpeedComponents()[2] + "\n");
        fb.flush();
    }

    private void moveTowardsTarget(Waypoint wp, double zSpeed) {

        double xDiff = wp.getUTM().x- copter.getLocationUTM().x;
        double yDiff = wp.getUTM().y - copter.getLocationUTM().y;
        double length = Math.sqrt(Math.pow(xDiff,2) + Math.pow(yDiff,2));

        final double MAX_SPEED = copter.getPlannedSpeed();
        double xSpeed = MAX_SPEED * xDiff/length;
        double ySpeed = MAX_SPEED * yDiff/length;
        copter.moveTo(ySpeed,xSpeed,-zSpeed);
    }

    private void moveOnlyVertical(double zSpeed) {
        copter.moveTo(0, 0,-zSpeed);
    }

    private double getError(Waypoint wp) {
        Location3DUTM lookAhead = getLookAhead(wp);
        Location3DUTM uav3d = new Location3DUTM(copter.getLocationUTM(), initialTerrainLevel + copter.getAltitudeRelative());
        double desiredAltitude = lookAhead.z;
        double uavAltitude = uav3d.z;
        return desiredAltitude - uavAltitude;
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
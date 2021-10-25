package com.protocols.topography.logic;

import com.api.API;
import com.api.copter.Copter;
import com.api.pojo.FlightMode;
import com.api.pojo.location.Waypoint;
import com.api.swarm.formations.Formation;
import com.sun.javafx.geom.Vec3d;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class DroneThread extends Thread{

    private final int numUAV;
    private final DEM dem;
    private final int demCellSize;
    private final CyclicBarrier barrier;
    private final Copter copter;
    private final Formation formation;
    private final double plannedSpeed;

    private Vec3d vec = new Vec3d();

    private final int prevision = 50;

    DroneThread(int nunUAV, DEM dem, CyclicBarrier barrier){
        this.numUAV = nunUAV;
        this.dem = dem;
        this.demCellSize = dem.getCellSize();
        this.barrier = barrier;
        this.copter = API.getCopter(numUAV);
        this.formation = UAVParam.groundFormation.get();
        this.plannedSpeed = copter.getPlannedSpeed();
    }
    @Override
    public void run(){
        copter.setFlightMode(FlightMode.GUIDED);
        completeMission();
    }

    private void completeMission() {
        List<Waypoint> wps = copter.getMissionHelper().getMissionsLoaded()[0];
        goToWps(wps);
        copter.moveTo(0, 0, 0);
    }

    private void goToWps(List<Waypoint> wps) {
        for(int index = 2; index < wps.size(); index++) {
            Waypoint wp = getWaypoint(wps, index);
            goToWp(wp);
            waitAtWp();
        }
    }

    private Waypoint getWaypoint(List<Waypoint> wps, int index) {
        Waypoint wp = wps.get(index);
        API.getGUI((int) copter.getID()).logUAV("Moving to WP: " + (index -1));
        if(wp.getAltitude() < 5) {
            System.out.println("for safety reasons the wp altitude is raised up to 10 meters.");
            wp.setAltitude(10);
        }
        return wp;
    }

    private void goToWp(Waypoint wp) {
        //Waypoint UTM coordinates
        Location2DUTM waypointUTM = formation.get3DUTMLocation(new Location3DUTM(wp.getUTM(),0), numUAV);
        double waypointZ = wp.getAltitude() + dem.getRealAltitude(waypointUTM);

        boolean waypointReached = false;
        while (!waypointReached) {
            waypointReached = moveToWp(wp, waypointUTM, waypointZ);
        }
    }

    private boolean moveToWp(Waypoint wp, Location2DUTM waypointUTM, double waypointZ) {
        Location2DUTM copterUTM = copter.getLocationUTM();
        Location3DUTM copter3DUTM = new Location3DUTM(copter.getLocationUTM(), copter.getAltitude());
        double distance2D = Math.abs(waypointUTM.distance(copterUTM));
        double distance3D = new Location3DUTM(waypointUTM, waypointZ).distance3D(copter3DUTM);
        float maxFutureAlt = getMaxFutureAlt(distance2D, copterUTM);
        double precision = Math.max(distance3D * 0.01, 5);

        applyDirectionVector(waypointUTM);
        setZ(wp, maxFutureAlt);
        applySlowingFactor(distance2D,precision,wp,maxFutureAlt);
        applyEmergency(copterUTM,maxFutureAlt);
        move();

        API.getArduSim().sleep((long) Math.min(1000 * demCellSize / plannedSpeed, 500));
        return isWayPointReached(distance3D, precision);
    }

    private void applyEmergency(Location2DUTM copterUTM, double maxFutureAlt){
        double groundAlt = dem.getRealAltitude(copterUTM);
        double relativeAltitude = copter.getAltitude() - groundAlt;
        if(relativeAltitude < 9 || maxFutureAlt - copter.getAltitude() > prevision) {
            vec.x = 0;
            vec.y = 0;
            vec.z = -1 * plannedSpeed;
        }
    }

    private void applySlowingFactor(double distance2D, double precision, Waypoint wp, double maxFutureAlt){
        double slowFactor = getSlowFactor(distance2D, precision);
        double z = vec.z;
        vec.x = slowFactor * plannedSpeed * vec.x;
        vec.y = slowFactor * plannedSpeed * vec.y;
        vec.z = Math.min(plannedSpeed, Math.abs(wp.getAltitude() + maxFutureAlt - copter.getAltitude())) * z;
    }
    private void setZ(Waypoint wp, float maxFutureAlt) {
        if((wp.getAltitude() + maxFutureAlt) <= copter.getAltitude()) {
            vec.z = 0.5;
        } else {
            vec.z = -1;
        }
    }

    private float getMaxFutureAlt(double distance2D, Location2DUTM copterUTM) {
        float maxFutureAlt = 0;
        for(int i = 0; i <= prevision / demCellSize; i++) {
            boolean isBeforeWp = i * demCellSize <= distance2D;
            if(isBeforeWp) {
                if (getHeightFromDem(copterUTM, i) > maxFutureAlt) {
                    maxFutureAlt = getHeightFromDem(copterUTM, i);
                }
            }
        }
        return maxFutureAlt;
    }

    private float getHeightFromDem(Location2DUTM copterUTM, int vectorSteps) {
        double projectionX = ((copterUTM.x - dem.getOrigin().getX()) / demCellSize) + vec.x * vectorSteps;
        double projectionY = ((copterUTM.y - dem.getOrigin().getY()) / demCellSize) + vec.y * vectorSteps;
        return dem.getDem()[(int) projectionY][(int) projectionX];
    }

    private void applyDirectionVector(Location2DUTM waypointUTM){
        double dx = waypointUTM.getX() - copter.getLocationUTM().getX();
        double dy = waypointUTM.getY() - copter.getLocationUTM().getY();
        double max = Math.max(Math.abs(dx), Math.abs(dy));

        vec.x = dx/max;
        vec.y = dy/max;
    }

    private void move(){
        copter.moveTo(vec.y,vec.x,vec.z);
    }

    private boolean isWayPointReached(double distance3D, double precision) {
        return !(distance3D > precision);
    }

    private double getSlowFactor(double distance2D, double precision) {
        double slowFactor = 1;
        if(precision <= plannedSpeed *7.5 && distance2D <= plannedSpeed *7.5) {
            slowFactor = distance2D /(plannedSpeed *7.5);
        }
        return slowFactor;
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
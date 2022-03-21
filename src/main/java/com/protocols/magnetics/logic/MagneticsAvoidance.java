package com.protocols.magnetics.logic;

import com.api.API;
import com.api.copter.Copter;
import com.api.copter.TakeOff;
import com.api.copter.TakeOffListener;
import com.api.pojo.location.Waypoint;
import com.protocols.magnetics.gui.MagneticsSimProperties;
import com.protocols.magnetics.pojo.Vector;
import com.uavController.UAVParam;
import es.upv.grc.mapper.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


class MagneticsAvoidance extends Thread{

    Queue<Location3DUTM> waypoints;
    Copter copter;
    int numUAV;
    int numUAVs;
    Communication communication;
    double minDistance = Double.MAX_VALUE;
    DrawVectors drawer;

    public MagneticsAvoidance(int numUAV){
        this.numUAV = numUAV;
        this.numUAVs = API.getArduSim().getNumUAVs();
        this.copter = API.getCopter(numUAV);
        setWaypoints(numUAV);
        communication = new Communication(numUAV);
        drawer = new DrawVectors(copter);
    }

    private void setWaypoints(int numUAV) {
        waypoints = new LinkedList<>();
        List<Waypoint>[] missions = copter.getMissionHelper().getMissionsLoaded();
        //first point is africa
        for(int i = 1; i<missions[numUAV].size(); i++){
            waypoints.add(new Location3DUTM(missions[numUAV].get(i).getUTM(), MagneticsSimProperties.altitude));
        }
    }

    private Location3DUTM getCopterLocation() {
        return new Location3DUTM(copter.getLocationUTM(),copter.getAltitude());
    }

    @Override
    public void run(){
        takeoff();
        communication.start();
        long start = System.currentTimeMillis();
        while (waypoints.size() > 0) {
            while (!waypointReached()) {
                Vector attraction = getAttractionVector();
                Vector totalRepulsion = calculateRepulsion();
                Vector resulting = Vector.add(attraction,totalRepulsion);
                resulting = reduceToMaxSpeed(resulting);
                moveUAV(resulting);
                logMinDistance();
                drawer.update(attraction,totalRepulsion,resulting);
                API.getArduSim().sleep(200);
            }
            waypoints.poll();
        }
        long protocolTime = System.currentTimeMillis() - start;
        communication.stopCommunication();
        saveData(protocolTime);
        land();
    }

    private Vector calculateRepulsion() {
        Vector totalRepulsion = new Vector();
        for(Location3DUTM obstacle:communication.getObstacles()) {
            Vector repulsion = getRepulsionVector(obstacle);
            totalRepulsion = Vector.add(totalRepulsion, repulsion);
        }
        return totalRepulsion;
    }

    private void takeoff() {
        TakeOff takeOff = copter.takeOff(MagneticsSimProperties.altitude, new TakeOffListener() {
            @Override
            public void onCompleteActionPerformed() {

            }

            @Override
            public void onFailure() {

            }
        });
        takeOff.start();
        try {
            takeOff.join();
        } catch (InterruptedException e1) {}
        waypoints.poll();
    }

    private boolean waypointReached(){
        return getCopterLocation().distance3D(waypoints.peek()) < 5;
    }

    private Vector getAttractionVector() {
        Vector attraction = new Vector(getCopterLocation(), waypoints.peek());
        attraction.normalize();
        double scalar = 0;

        // to create the takeover case, UAV 1 will always take over.
        double speed = MagneticsSimProperties.maxspeed;
        /*
        if(MagneticsSimProperties.missionFile.get(0).getName().contains("takeover.kml")){
            if(numUAV == 0){
                speed = 5;
            }else {
                speed = 10;
            }
        }
         */

        if(getCopterLocation().distance3D(waypoints.peek()) > 50) {
            scalar = speed;
        }else{
            scalar = MagneticsSimProperties.maxspeed/2;
        }
        attraction.scalarProduct(scalar);
        return attraction;
    }

    private Vector getRepulsionVector(Location3DUTM obstacle) {
        if(obstacle != null) {
            Location3DUTM uav = getCopterLocation();
            Vector repulsionDirection = new Vector(obstacle, uav);
            return changeMagnitude(repulsionDirection, obstacle.distance3D(uav));
        }
        return null;
    }

    private Vector changeMagnitude(Vector repulsion, double distance) {
        repulsion.z = 0;
        repulsion.normalize();
        applyMagnitudeFunction(repulsion, distance);
        return repulsion;
    }

    private void applyMagnitudeFunction(Vector repulsion, double distance) {
        distance = Math.max(MagneticsSimProperties.frd,distance);
        Expression expression = new ExpressionBuilder(MagneticsSimProperties.repulsionMagnitude)
                .variables("x","frd","a")
                .build()
                .setVariable("x", distance)
                .setVariable("frd",MagneticsSimProperties.frd)
                .setVariable("a",MagneticsSimProperties.a);
        double scalar = expression.evaluate();
        if(scalar < 0){
            scalar = 0;
        }
        repulsion.scalarProduct(scalar);
    }

    private Vector reduceToMaxSpeed(Vector v_) {
        Vector v = new Vector(v_);
        if(v.magnitude() > MagneticsSimProperties.maxspeed){
            v.normalize();
            v.scalarProduct(MagneticsSimProperties.maxspeed);
        }
        return v;
    }

    private void moveUAV(Vector resulting) {
        copter.moveTo(resulting.y, resulting.x, -resulting.z);
    }

    private void logMinDistance() {
        for(int i=0;i<numUAVs;i++) {
            if(numUAV == i){continue;}
            double distance = UAVParam.distances[numUAV][i].get();
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
    }

    private void saveData(long protocolTime) {

        String protocol = MagneticsSimProperties.missionFile.get(0).toString();
        protocol = protocol.substring(protocol.lastIndexOf("/") + 1);
        protocol = protocol.substring(0, protocol.length() - 4);
        String repulsion = createStringRepulsionFunction();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(protocol, true));
            writer.append(String.valueOf(numUAV))
                    .append(",")
                    .append(String.valueOf(API.getArduSim().getNumUAVs())) //protocol
                    .append(",")
                    .append(repulsion)
                    .append(",")
                    .append(String.valueOf(minDistance))
                    .append(",")
                    .append(String.valueOf(protocolTime))
                    .append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createStringRepulsionFunction() {
        String s = MagneticsSimProperties.repulsionMagnitude;
        String a = String.valueOf(MagneticsSimProperties.a);
        String frd = String.valueOf(MagneticsSimProperties.frd);
        s = s.replace("a",a);
        s = s.replace("frd",frd);
        return s;
    }

    private void land() {
        copter.land();
    }

}

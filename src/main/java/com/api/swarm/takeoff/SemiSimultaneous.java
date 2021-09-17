package com.api.swarm.takeoff;

import com.sun.javafx.geom.Vec3d;
import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.util.*;

public class SemiSimultaneous extends TakeoffAlgorithm{

    private final Map<Long, Pair<Location3DUTM,Location3DUTM>> placement;
    private Map<Long, Vec3d> vec_nor;
    private final boolean isMaster;

    public SemiSimultaneous(Map<Long, Location3DUTM> assignment, Map<Long, Location3DUTM> groundLocations) {
        assert(assignment.size() == groundLocations.size());
        this.assignment = assignment;
        this.isMaster = assignment != null;
        this.placement = new HashMap<>();
        if(isMaster) { setPlacement(groundLocations); }
    }

    private void setPlacement(Map<Long, Location3DUTM> groundLocations) {
        for(Map.Entry<Long,Location3DUTM> e:assignment.entrySet()){
            long uavId = e.getKey();
            Location3DUTM ground = groundLocations.get(uavId);
            Location3DUTM air = e.getValue();
            placement.put(uavId,new Pair<>(ground,air));
        }
    }


    @Override
    public void takeOff(int numUAV) {
        this.numUAV = numUAV;
        if(isMaster) {
            vec_nor = getNormalizedVectors();
            ArrayList<Quartet<Long, Location3DUTM, Long, Location3DUTM>> collisionList = detectCollision();
        }
    }

    private Map<Long, Vec3d> getNormalizedVectors() {
        Map<Long, Vec3d> vectors = new HashMap<>();
        for(Map.Entry<Long,Pair<Location3DUTM,Location3DUTM>> e:placement.entrySet()){
            Location3DUTM ground = e.getValue().getValue0();
            Location3DUTM air = e.getValue().getValue1();
            Location3DUTM l = subtract(air,ground);
            Vec3d v = new Vec3d(l.x,l.y,l.z);
            v.normalize();
            vectors.put(e.getKey(),v);
        }
        return vectors;
    }

    public ArrayList<Quartet<Long, Location3DUTM, Long, Location3DUTM>> detectCollision() {
        vec_nor = getNormalizedVectors();


        ArrayList<Quartet<Long, Location3DUTM, Long, Location3DUTM>> collisionList = new ArrayList<>();
        Set<Long> uncheckedUAV = new HashSet<>(placement.keySet());

        for(Long uavId: placement.keySet()){
            uncheckedUAV.remove(uavId);
            Location3DUTM air = placement.get(uavId).getValue1();
            Location3DUTM position = placement.get(uavId).getValue0();
            Vec3d directionVector = vec_nor.get(uavId);

            while(position.distance3D(air) > 1){
                position = displace(position,directionVector);
                Optional<Pair<Long,Location3DUTM>> collision = searchForFirstCollisionWithOtherUAVs(position,uncheckedUAV);
                if(collision.isPresent()){
                    long collidedUav = collision.get().getValue0();
                    Location3DUTM collisionLocation = collision.get().getValue1();
                    collisionList.add(new Quartet<>(uavId,position,collidedUav,collisionLocation));
                    break;
                }
            }
        }

        return collisionList;
    }

    private Optional<Pair<Long,Location3DUTM>> searchForFirstCollisionWithOtherUAVs(Location3DUTM position, Set<Long>uavIds) {
        for(Long uavId: uavIds){
            Optional<Location3DUTM> collision = searchFirstCollision(position,uavId);
            if(collision.isPresent()){
                return Optional.of(new Pair<>(uavId,collision.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<Location3DUTM> searchFirstCollision(Location3DUTM fixedPoint, Long uavId) {
        Location3DUTM air = placement.get(uavId).getValue1();
        Location3DUTM position = placement.get(uavId).getValue0();
        Vec3d directionVector = vec_nor.get(uavId);

        while(position.distance3D(air) > 1){
            position = displace(position,directionVector);
            if(position.distance3D(fixedPoint) <= 5){
                return Optional.of(position);
            }
        }
        return Optional.empty();
    }

    private Location3DUTM displace(Location3DUTM position, Vec3d directionVector) {
        return new Location3DUTM(position.x+directionVector.x, position.y+directionVector.y,position.z+directionVector.z);
    }


    private Location3DUTM subtract(Location3DUTM air, Location3DUTM ground){
        return new Location3DUTM(air.x - ground.x, air.y - ground.y, air.z - ground.z);
    }
}

package com.protocols.magnetics.pojo;

import es.upv.grc.mapper.Location3DUTM;


public class Vector {

    public double x,y,z;

    public Vector(Location3DUTM l1, Location3DUTM l2) {
        x = l2.x - l1.x;
        y = l2.y - l1.y;
        z = l2.z - l1.z;
    }

    public Vector(Vector v){
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector(){
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    private Vector(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "{"+ x + ";" + y + ";" + z + "}";
    }

    public void add(Vector v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    public static Vector add(Vector v1, Vector v2){
        if(v1 == null){return v2;}
        if(v2 == null){return v1;}
        return new Vector(v1.x+v2.x,v1.y+v2.y,v1.z+v2.z);
    }

    public void subtract(Vector v){
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    public void scalarProduct(double s){
        x *= s;
        y *= s;
        z *= s;
    }

    public double magnitude(){
        return Math.sqrt(x*x + y*y + z*z);
    }

    public void normalize(){
        double m = magnitude();
        x /= m;
        y /= m;
        z /= m;

    }
}

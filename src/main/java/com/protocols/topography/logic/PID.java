package com.protocols.topography.logic;

class PID {

    private final double MAX_SPEED =5;

    private final double kp,kd,ki,iterationTime;

    private double previousError=0;
    private double integral_prior=0;
    private boolean integralActive = false;


    public PID(double kp, double kd, double ki, double iterationTime){
        this.kp = kp;
        this.kd = kd;
        this.ki = ki;
        this.iterationTime = iterationTime;
    }

    public double calculateOutput(double error){
        double derivative = (error-previousError)/iterationTime;
        double integral = calculateIntegral(error);

        double output = kp*error + ki*integral + kd*derivative;

        setIntegralActive(error);
        accumulateIntegral(integral);
        previousError = error;

        return clamp(output);
    }

    private void accumulateIntegral(double integral) {
        if(integralActive){
            integral_prior += integral;
        }else{
            integral_prior = 0.0;
        }
    }

    private void setIntegralActive(double error) {
        if(Math.abs(previousError-error) < 0.05){
            integralActive = true;
        }else{
            integralActive = false;
        }
    }

    private double calculateIntegral(double error) {
        if(integralActive) {
            return integral_prior + error * iterationTime;
        }else{
            return 0.0;
        }
    }

    private double clamp(double output) {
        if(output > MAX_SPEED){
            return MAX_SPEED;
        }else if(output < -MAX_SPEED){
            return -MAX_SPEED;
        }else{
            return output;
        }
    }

}

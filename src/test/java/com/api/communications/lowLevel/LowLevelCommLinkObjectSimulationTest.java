package com.api.communications.lowLevel;

import com.api.ArduSim;
import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.setup.Param;
import com.uavController.UAVParam;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LowLevelCommLinkObjectSimulationTest {

    private static Stream<Arguments> inputInit(){
        return Stream.of(
                Arguments.of(0,100,true),
                Arguments.of(10,0,true),
                Arguments.of(0,0,true),
                Arguments.of(5,10,false)
        );
    }

    /**
     * Tests {@link CommLinkObjectSimulation#init(int, boolean, boolean, int)} input parameters
     * @param numUAV: number of UAVs.
     * @param bufferSize: size of buffer in int.
     * @param expectCatch: whether or not it is expected that an error is thrown.
     */
    @ParameterizedTest
    @MethodSource("inputInit")
    void init(int numUAV,int bufferSize, boolean expectCatch) {
        try{
            CommLinkObjectSimulation.init(numUAV,true,true,bufferSize);
        }catch (Error e){
            assertTrue(expectCatch);
        }
    }

    private static Stream<Arguments> inputInitCollisionAndCommunication(){
        return Stream.of(
                Arguments.of(ArduSim.MULTICOPTER,true),
                Arguments.of(ArduSim.SIMULATOR_GUI,false),
                Arguments.of(ArduSim.PCCOMPANION,true),
                Arguments.of(ArduSim.SIMULATOR_CLI,false)
        );
    }

    /**
     * Tests {@link CommLinkObjectSimulation#init(int, boolean, boolean, int)} creating objects
     * @param role: function of Ardusim {Multicopter, Simulator_GUI, PCcompanion, Simulator_CLI}
     * @param expectNull: whether or not the parameters are suppose to be null.
     */
    @ParameterizedTest
    @MethodSource("inputInitCollisionAndCommunication")
    void initCollisionAndCommunication(int role,boolean expectNull){
        // Reset from previous tests
        Param.role = role;
        UAVParam.distances = null;
        CommLinkObjectSimulation.isInRange = null;

        int numUAVs = 5;
        CommLinkObjectSimulation.init(numUAVs,true,true,100);
        if(expectNull){
            assertNull(UAVParam.distances);
            assertNull(CommLinkObjectSimulation.isInRange);
        }else {
            assertEquals(UAVParam.distances.length, numUAVs);
            assertEquals(CommLinkObjectSimulation.isInRange.length, numUAVs);
        }
    }

}
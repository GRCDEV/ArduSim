package com.api.swarm.discovery;

import com.api.ArduSim;
import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.api.communications.RangeCalculusThread;
import com.api.communications.WirelessModel;
import com.setup.Param;
import com.setup.sim.logic.DistanceCalculusThread;
import com.uavController.UAVCurrentData;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2D;
import es.upv.grc.mapper.Location3DUTM;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class BasicDiscoverTest {

    final Location2D[] startingLocations = new Location2D[]{
            new Location2D(-0.34700,39.48070),
            new Location2D(-0.34600,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34700,39.48070),
            new Location2D(-0.34600,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34500,39.48070),
            new Location2D(-0.34700,39.48070),
            new Location2D(-0.34600,39.48070)
    };
    final int numUAVs = startingLocations.length;

    DistanceCalculusThread distanceCalculusThread;
    RangeCalculusThread rangeCalculusThread;

    private void startArdusim() {
        distanceCalculusThread = new DistanceCalculusThread();
        rangeCalculusThread = new RangeCalculusThread();
        Param.role = ArduSim.SIMULATOR_CLI;
        Param.selectedWirelessModel = WirelessModel.NONE;
        Param.numUAVs = numUAVs;
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
        Param.verboseLogging = true;
        UAVParam.uavCurrentData = new UAVCurrentData[Param.numUAVs];
        CommLinkObjectSimulation.init(numUAVs,true,true,163840);

        double[] speed = new double[]{2,2,2};
        for(int numUAV=0;numUAV<numUAVs;numUAV++){
            UAVParam.uavCurrentData[numUAV] = new UAVCurrentData();
            UAVParam.uavCurrentData[numUAV].update(0, startingLocations[numUAV], 0, 0, speed, 0, 0);
        }
        distanceCalculusThread.start();
        rangeCalculusThread.start();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @BeforeEach
    void init(){
        startArdusim();
    }

    @Test
    void start(){
        Discover master = new BasicDiscover(0);
        Thread masterThread = new Thread(master::start);
        masterThread.start();
        startSlaves();
        try {
            masterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map<Long, Location3DUTM> discovered = master.getUAVsDiscovered();
        Assertions.assertEquals(numUAVs, discovered.size());
    }

    private void startSlaves() {
        List<Integer> UAVs = new ArrayList<>();
        for (int numUAV = 1; numUAV < numUAVs; numUAV++) {
            UAVs.add(numUAV);
        }
        UAVs.forEach(numUAV -> new Thread(() -> {
            BasicDiscover d = new BasicDiscover(numUAV);
            d.start();
        }).start());
    }

    @AfterEach
    void tearDown(){
        Param.simStatus = Param.SimulatorState.SHUTTING_DOWN;
        try {
            distanceCalculusThread.join();
            rangeCalculusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
package com.api.communications;

import com.api.ArduSim;
import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.setup.Param;
import com.setup.sim.logic.DistanceCalculusThread;
import com.uavController.UAVCurrentData;
import com.uavController.UAVParam;
import es.upv.grc.mapper.Location2D;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import com.api.communications.HighlevelCommLink.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HighlevelCommLinkTest {

    List<JSONObject> msgsToSend = new ArrayList<>();
    List<HighlevelCommLink> commLinks = new ArrayList<>();
    final int numUAVs = 3;
    DistanceCalculusThread distanceCalculusThread = new DistanceCalculusThread();
    RangeCalculusThread rangeCalculusThread = new RangeCalculusThread();

    //message data
    final static int MSG_ID = 31;
    final static int SENDER_ID = 0;
    final static double DATA = 25.6;

    @BeforeAll
    void init(){
        startArdusim();
        startHighLevelCommLink();
        makeMessage();
    }

    private void startArdusim() {
        Param.role = ArduSim.SIMULATOR_CLI;
        Param.selectedWirelessModel = WirelessModel.NONE;
        Param.numUAVs = numUAVs;
        Param.simStatus = Param.SimulatorState.STARTING_UAVS;
        UAVParam.uavCurrentData = new UAVCurrentData[Param.numUAVs];
        CommLinkObjectSimulation.init(numUAVs,true,true,163840);

        Location2D loc = new Location2D(-0.7336316654085351,39.7251146686473);
        double[] speed = new double[]{2,2,2};
        for(int numUAV=0;numUAV<numUAVs;numUAV++){
            UAVParam.uavCurrentData[numUAV] = new UAVCurrentData();
            UAVParam.uavCurrentData[numUAV].update(0, loc, 0, 0, speed, 0, 0);
        }

        distanceCalculusThread.start();
        rangeCalculusThread.start();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startHighLevelCommLink() {
        for(int numUAV=0;numUAV<numUAVs;numUAV++){
            commLinks.add(new HighlevelCommLink(numUAV));
        }
    }

    private void makeMessage() {
        for(int i=0;i<numUAVs;i++) {
            JSONObject msgToSend = new JSONObject();
            msgToSend.put(Keywords.MESSAGEID, MSG_ID);
            msgToSend.put(Keywords.SENDERID, i);
            msgToSend.put("data", DATA);
            msgsToSend.add(msgToSend);
        }
    }

    @Test
    void sendAndReceive(){
        sendAndReceive(0,1);
        sendAndReceive(1,0);
    }

    private void sendAndReceive(int senderId, int receiverId) {
        commLinks.get(senderId).sendJSON(msgsToSend.get(0));
        JSONObject message = null;
        long timeStamp = System.currentTimeMillis();
        long executingTime = 0;
        while(message == null && executingTime<500){
            executingTime = System.currentTimeMillis() - timeStamp;
            message = commLinks.get(receiverId).receiveMessage();
        }
        assertNotNull(message);
        assertEquals(MSG_ID,message.getInt(Keywords.MESSAGEID));
        assertEquals(SENDER_ID,message.getInt(Keywords.SENDERID));
        assertEquals(DATA,message.getDouble("data"));
    }

    @Test
    void multipleSendersOneReceiver(){
        HighlevelCommLink sender1 = commLinks.get(0);
        HighlevelCommLink sender2 = commLinks.get(1);
        HighlevelCommLink receiver = commLinks.get(2);

        boolean receivedMessageSender1 = false;
        boolean receivedMessageSender2 = false;
        long timestamp = System.currentTimeMillis();
        while(!(receivedMessageSender1 && receivedMessageSender2)){
            sender1.sendJSON(msgsToSend.get(0));
            sender2.sendJSON(msgsToSend.get(1));
            JSONObject receivedMsg = receiver.receiveMessage();
            if(receivedMsg != null){
                int senderId = receivedMsg.getInt(Keywords.SENDERID);
                if(senderId == 0){
                    receivedMessageSender1 = true;
                }else if(senderId == 1){
                    receivedMessageSender2 = true;
                }
            }
            assert System.currentTimeMillis() - timestamp <= 3000;
        }
    }

    @Test
    @Timeout(value = 500, unit = TimeUnit.MILLISECONDS)
    void receiveWithMandatoryParameters(){
        caseWrongParameters();
        caseRightParameters();
    }

    private void caseRightParameters() {
        commLinks.get(0).sendJSON(msgsToSend.get(0));
        Map<String,Object> mandatoryParameters = new HashMap<>();
        mandatoryParameters.put(Keywords.MESSAGEID,MSG_ID);
        mandatoryParameters.put(Keywords.SENDERID,SENDER_ID);
        JSONObject message = commLinks.get(1).receiveMessage(mandatoryParameters);
        assertNotNull(message);
        assertEquals(MSG_ID,message.getInt(Keywords.MESSAGEID));
        assertEquals(SENDER_ID,message.getInt(Keywords.SENDERID));
    }

    private void caseWrongParameters() {
        commLinks.get(0).sendJSON(msgsToSend.get(0));
        Map<String,Object> mandatoryParameters = new HashMap<>();
        mandatoryParameters.put(Keywords.MESSAGEID,MSG_ID+1);
        mandatoryParameters.put(Keywords.SENDERID,SENDER_ID);
        JSONObject message = commLinks.get(1).receiveMessage(mandatoryParameters);
        assertNull(message);

    }

    @Test
    void sendUntilACKs(){
        Thread sender = new Thread(() -> {
            Set<Integer> ackIds = new HashSet<>();
            ackIds.add(1);
            ackIds.add(2);
            commLinks.get(0).sendJSONUntilACKsReceived(msgsToSend.get(0),ackIds);
        });

        List<Integer> listeners = new ArrayList<>();
        for(int i=1;i<numUAVs;i++){
            listeners.add(i);
        }

        listeners.forEach((numUAV) -> new Thread(() -> {
            Map<String,Object> mandatoryFields = new HashMap<>();
            mandatoryFields.put(Keywords.MESSAGEID,MSG_ID);
            commLinks.get(numUAV).receiveMessageReplyACK(mandatoryFields,5);
        }).start());

        sender.start();

        try {
            sender.join(2000);
        } catch (InterruptedException ignored) { }
        assertFalse(sender.isAlive(),"Sender could not be joined, increase nrOfRepliesOfReceiver");
    }

    @Test
    void sendUntilACK(){
        Thread sender0 = new Thread(() -> commLinks.get(0).sendJSONUntilACKReceived(msgsToSend.get(0),2, 1));
        Thread sender1 = new Thread(() -> commLinks.get(1).sendJSONUntilACKReceived(msgsToSend.get(1),2, 2));

        new Thread(() -> {
            Map<String,Object> mandatoryFields = new HashMap<>();
            mandatoryFields.put(Keywords.MESSAGEID,MSG_ID);
            commLinks.get(2).receiveMessageReplyACK(mandatoryFields,5);
        }).start();

        sender0.start();
        sender1.start();

        try {
            sender0.join(2000);
            sender1.join(2000);
        } catch (InterruptedException ignored) { }
        assertFalse(sender0.isAlive(),"Sender could not be joined, increase nrOfRepliesOfReceiver");
    }

    @AfterAll
    void tearDown() {
        Param.simStatus = Param.SimulatorState.SHUTTING_DOWN;
        try {
            distanceCalculusThread.join();
            rangeCalculusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        commLinks = null;
    }
}
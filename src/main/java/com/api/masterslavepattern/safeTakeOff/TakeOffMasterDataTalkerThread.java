package com.api.masterslavepattern.safeTakeOff;

import com.api.API;
import com.api.communications.CommLink;
import com.api.formations.Formation;
import com.esotericsoftware.kryo.io.Output;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import com.api.ArduSim;
import com.api.GUI;
import com.api.masterslavepattern.InternalCommLink;
import com.api.masterslavepattern.MSMessageID;
import com.api.masterslavepattern.MSParam;
import com.api.masterslavepattern.MSText;
import com.uavController.UAVParam;
import org.javatuples.Quartet;

import java.util.*;

public class TakeOffMasterDataTalkerThread extends Thread{

    TakeOffMasterDataListenerThread listener;
    int numUAVs;
    Long selfID;
    private InternalCommLink commLink;
    private byte[] outBuffer;
    private Output output;
    private ArduSim arduSim;
    private GUI gui;

    // parameters given by ListenerThread after calculating the match
    private Quartet<Integer, Long, Location2DUTM, Double>[] match;
    private long centerUAVID;
    private Location2DUTM centerUAVLocationUTM;
    private long[] ids;
    private double targetAltitude;
    private Formation flightFormation;
    private double formationYaw;
    private boolean exclude;
    private Set<Long> UAVSet = new HashSet<>();

    public TakeOffMasterDataTalkerThread(TakeOffMasterDataListenerThread listener,int numUAVs, Long selfID){
        this.listener = listener;
        this.numUAVs = numUAVs;
        this.selfID = selfID;
        this.commLink = InternalCommLink.getCommLink(selfID.intValue());
        this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        this.output = new Output(outBuffer);
        this.arduSim = API.getArduSim();
        this.gui = API.getGUI(Math.toIntExact(selfID));
        for(long i = 0; i< numUAVs; i++){
            UAVSet.add(i);
        }
    }

    @Override
    public void run(){
        sendExcludeToMissingSlaves();
        sendTakeOffDataToSlaves();
        sendLeaderOrderToSlaves();
    }

    private void sendLeaderOrderToSlaves() {
        byte[][] ms = createMessageLider(ids);
        while(listener.getDataState() == MSParam.STATE_SENDING_LIDERORDER) {
            for (byte[] m : ms) {
                long start = System.currentTimeMillis();
                commLink.sendBroadcastMessage(m);
                long waitTime = MSParam.SENDING_PERIOD - (System.currentTimeMillis() - start);
                if (waitTime > 0) {
                    arduSim.sleep(waitTime);
                }
            }
        }
    }

    private byte[][] createMessageLider(long[] ids) {
        boolean fragment = numUAVs > Math.floorDiv(CommLink.DATAGRAM_MAX_LENGTH - 2, 8); //183
        byte[][] ms;
        if (fragment) {
            ms = createMessagesLider_order_frag(ids);
        } else {
            ms = createMessageLider_order(ids);
        }
        return ms;
    }

    private byte[][] createMessageLider_order(long[] ids) {
        byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        byte[][] ms = new byte[1][numUAVs];
        Output output = new Output(outBuffer);
        output.reset();
        output.writeShort(MSMessageID.LIDER_ORDER);
        for (long id : ids) {
            output.writeLong(id);
        }
        output.flush();
        ms[0] = Arrays.copyOf(outBuffer, output.position());
        return ms;
    }

    private byte[][] createMessagesLider_order_frag(long[] ids) {
        byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        Output output = new Output(outBuffer);
        int maxSize = Math.floorDiv(CommLink.DATAGRAM_MAX_LENGTH - Short.BYTES - Integer.BYTES*2, 8);	//183 UAVs
        int numberOfFrags = (int) Math.ceil(numUAVs / (maxSize * 1.0));
        byte[][] ms = new byte[numberOfFrags][maxSize];

        int idsPosition = 0;
        for(int fragment = 0;fragment< numberOfFrags;fragment++){
            output.reset();
            output.writeShort(MSMessageID.LIDER_ORDER_FRAG);
            output.writeInt(maxSize);
            output.writeInt(fragment);
            for(int i = 0;i<maxSize;i++){
                if(idsPosition >= ids.length){
                    break;
                }
                output.writeLong(ids[idsPosition]);
                idsPosition++;
            }
            output.flush();
            ms[fragment] = Arrays.copyOf(outBuffer,output.position());
        }
        return ms;
    }

    private void sendTakeOffDataToSlaves() {
        Map<Long,byte[]> messages = createMessageTake_off_data();
        while(listener.getDataState() == MSParam.STATE_SENDING_TAKEOFFDATA){
            Set<Long> missingIDs = getMissingIds();
            long start = System.currentTimeMillis();
            for(long missingID : missingIDs){
                byte[] message = messages.get(missingID);
                if(message != null) {
                    commLink.sendBroadcastMessage(message);
                }
            }
            //TODO check: this could be a problem for a lot of UAVs
            long waitTime = MSParam.SENDING_PERIOD - (System.currentTimeMillis() - start);
            if(waitTime > 0) {
                arduSim.sleep(waitTime);
            }
        }
    }

    private Map<Long,byte[]> createMessageTake_off_data() {
        Map<Long,byte[]> messages = new HashMap<>((int)Math.ceil(numUAVs / 0.75) + 1);
        byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        Output output = new Output(outBuffer);
        long curID, prevID, nextID;
        // i represents the position in the takeoff sequence
        int formationPos;
        long pID;
        long nID;
        Location2DGeo targetLocation = null;
        SafeTakeOffContext data = null;
        double takeOffAltitudeStep1 = calculateTakeOffAltitudeStep1();

        for (int i = 0; i < numUAVs; i++) {
            curID = match[i].getValue1();
            if (i == 0) {
                prevID = SafeTakeOffContext.BROADCAST_MAC_ID;
            } else {
                prevID = match[i - 1].getValue1();
            }
            if (i == numUAVs - 1) {
                nextID = SafeTakeOffContext.BROADCAST_MAC_ID;
            } else {
                nextID = match[i + 1].getValue1();
            }

            // Master UAV
            formationPos = match[i].getValue0();
            System.out.println("formationpos set " + formationPos);
            if (curID == selfID) {
                pID = prevID;
                nID = nextID;
                try {
                    targetLocation = flightFormation.get2DUTMLocation(centerUAVLocationUTM,formationPos).getGeo();
                } catch (LocationNotReadyException e) {
                    e.printStackTrace();
                    gui.exit(e.getMessage());
                }
                UAVParam.groundFormation.get().init(numUAVs,UAVParam.landDistanceBetweenUAV);
                data = new SafeTakeOffContext(pID, nID, targetLocation, takeOffAltitudeStep1, targetAltitude, exclude, ids, centerUAVLocationUTM,
                        numUAVs, flightFormation, UAVParam.groundFormation.get(), formationPos, formationYaw);
            } else {
                output.reset();
                output.writeShort(MSMessageID.TAKE_OFF_DATA);
                output.writeLong(curID);
                output.writeLong(centerUAVID);
                output.writeDouble(centerUAVLocationUTM.x);
                output.writeDouble(centerUAVLocationUTM.y);
                output.writeLong(prevID);
                output.writeLong(nextID);
                output.writeInt(numUAVs);
                output.writeString(flightFormation.getLayout().name());
                output.writeDouble(UAVParam.airDistanceBetweenUAV);
                //output.writeDouble(UAVParam.landDistanceBetweenUAV);
                System.out.println("sending formationPos " + formationPos);
                output.writeInt(formationPos);
                output.writeDouble(formationYaw);
                output.writeDouble(takeOffAltitudeStep1);
                output.writeDouble(targetAltitude);
                output.flush();
                messages.put(curID, Arrays.copyOf(outBuffer, output.position()));
            }
        }
        if (targetLocation == null) {
            gui.exit(MSText.MASTER_ID_NOT_FOUND);
        }
        listener.setResult(data);
        return messages;
    }

    private double calculateTakeOffAltitudeStep1() {
        double takeOffAltitudeStep1;
        if (this.targetAltitude <= 5.0) {
            takeOffAltitudeStep1 = 2.0;
        } else if (this.targetAltitude >= 10.0) {
            takeOffAltitudeStep1 = 5.0;
        } else {
            takeOffAltitudeStep1 = this.targetAltitude / 2;
        }
        return takeOffAltitudeStep1;
    }

    private void sendExcludeToMissingSlaves() {
        while(listener.getDataState() == MSParam.STATE_EXCLUDE){
            long start = System.currentTimeMillis();
            Set<Long> missingIDs = getMissingIds();
            //TODO can be better by putting more ids in one message but then there is a change of bufferOverFlow
            for (Long missingID : missingIDs) {
                output.reset();
                output.writeShort(MSMessageID.EXCLUDE); //message type
                output.writeLong(missingID); //ids missing
                output.flush();
                byte[] message = Arrays.copyOf(outBuffer, output.position());
                commLink.sendBroadcastMessage(message);
            }
            long waitTime = MSParam.SENDING_PERIOD - (System.currentTimeMillis() - start);
            if(waitTime > 0) {
                arduSim.sleep(waitTime);
            }
        }
    }

    private Set<Long> getMissingIds() {
        Map<Long, Boolean> excludedMap = listener.getAckSet();
        Set<Long> missingIDs = new HashSet<>();
        for (long i = 0; i < numUAVs; i++) {
            if (excludedMap.get(i) == null && i != selfID) {
                missingIDs.add(i);
            }
        }
        return missingIDs;
    }

    public void setDataParameters(Quartet<Integer, Long, Location2DUTM, Double>[] match, long centerUAVID,
                                  Location2DUTM centerUAVLocationUTM, long[] ids, double targetAltitude,
                                  Formation flightFormation, double formationYaw, boolean exclude) {
        //TODO update this to many arguments
        this.match = match;
        this.centerUAVID = centerUAVID;
        this.centerUAVLocationUTM = centerUAVLocationUTM;
        this.ids = ids;
        this.targetAltitude = targetAltitude;
        this.flightFormation = flightFormation;
        this.formationYaw = formationYaw;
        this.exclude = exclude;
    }
}

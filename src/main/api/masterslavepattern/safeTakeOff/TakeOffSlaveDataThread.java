package main.api.masterslavepattern.safeTakeOff;

import api.API;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.Formation;
import main.api.formations.FormationFactory;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TakeOffSlaveDataThread extends Thread{

    private final int numUAV;
    private int numUAVs;
    private final boolean exclude;
    private final AtomicReference<SafeTakeOffContext> result;
    private Location2DUTM centerUAVLocation = null;
    private Formation flightFormation, landFormation;
    private int formationPos;
    private double formationYaw, altitudeStep1, altitudeStep2;
    private long[] masterOrder;
    private long prevID, nextID;

    private final InternalCommLink commLink;
    private byte[] inBuffer;
    private final Input input;
    private final byte[] outBuffer;
    private final Output output;
    private final GUI gui;

    public TakeOffSlaveDataThread(int numUAV, boolean exclude, AtomicReference<SafeTakeOffContext>  result){
        this.numUAV = numUAV;
        this.exclude = exclude;
        this.result = result;
        this.gui = API.getGUI(numUAV);

        this.commLink = InternalCommLink.getCommLink(numUAV);
        this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        this.input = new Input(inBuffer);
        this.outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
        this.output = new Output(outBuffer);
    }

    @Override
    public void run(){
        AtomicInteger state = new AtomicInteger(MSParam.STATE_EXCLUDE);
        gui.logVerboseUAV(MSText.SLAVE_LISTENER_WAITING_DATA);
        respondToMasterExcludeMessage(state);
        respondToMasterTakeoffDataMessage(state);
        respondToMasterLeaderOrderMessage(state);
        setSafeTakeOffContext(state);
        gui.logVerboseUAV(MSText.SLAVE_LISTENER_TAKE_OFF_DATA_END);
    }

    private void setSafeTakeOffContext(AtomicInteger state) {
        Location2DGeo target = null;
        try {
            target = flightFormation.get2DUTMLocation(centerUAVLocation,formationPos).getGeo();
        } catch (LocationNotReadyException e) {
            e.printStackTrace();
            gui.exit(e.getMessage());
        }
        System.out.println(target);
        SafeTakeOffContext data = new SafeTakeOffContext(prevID, nextID, target, altitudeStep1, altitudeStep2, exclude,
                this.masterOrder, centerUAVLocation, numUAVs, flightFormation, landFormation, formationPos, formationYaw);
        this.result.set(data);
        state.set(MSParam.STATE_SENDING_FINISHED);
    }

    private void respondToMasterLeaderOrderMessage(AtomicInteger state) {
        boolean sendAckOnce = false;
        long lastTimeReceivedData = System.currentTimeMillis();
        while(state.get() == MSParam.STATE_SENDING_LIDERORDER){
            inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
            if (inBuffer != null) {
                input.setBuffer(inBuffer);
                short type = input.readShort();
                if (type == MSMessageID.LIDER_ORDER) {
                    lastTimeReceivedData = System.currentTimeMillis();
                    for (int i = 0; i < numUAVs; i++) {
                        this.masterOrder[i] = input.readLong();
                    }
                    sendLeaderOrderAck();
                    sendAckOnce = true;
                }else if(type == MSMessageID.LIDER_ORDER_FRAG){
                    int maxSize = input.readInt();
                    int fragment = input.readInt();
                    for(int i = 0;i<maxSize;i++){
                        int position = fragment*maxSize + i;
                        if(position >= this.masterOrder.length){
                            break;
                        }
                        long value = input.readLong();
                        this.masterOrder[position] = value;
                    }
                    boolean allPositionsSet = true;
                    for (long value : masterOrder) {
                        if (value == -1) {
                            allPositionsSet = false;
                            break;
                        }
                    }
                    if(allPositionsSet){
                        sendLeaderOrderAck();
                        sendAckOnce = true;
                    }
                }
            }
            if(sendAckOnce && (System.currentTimeMillis() - lastTimeReceivedData) > MSParam.TAKE_OFF_DATA_TIMEOUT){
                break;
            }
        }
    }

    private void sendLeaderOrderAck() {
        gui.logVerboseUAV("leader order received sending ack to master");
        output.reset();
        output.writeShort(MSMessageID.LIDER_ORDER_ACK);
        output.writeLong(numUAV);
        output.flush();
        byte[] message = Arrays.copyOf(outBuffer, output.position());
        commLink.sendBroadcastMessage(message);
    }

    private void respondToMasterTakeoffDataMessage(AtomicInteger state) {
        while(state.get() == MSParam.STATE_SENDING_TAKEOFFDATA){
            inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
            if (inBuffer != null) {
                input.setBuffer(inBuffer);
                short type = input.readShort();
                if (type == MSMessageID.TAKE_OFF_DATA) {
                    if (input.readLong() == numUAV) {
                        input.readLong();	// The center UAV ID is no longer used
                        centerUAVLocation = new Location2DUTM(input.readDouble(), input.readDouble());
                        prevID = input.readLong();
                        nextID = input.readLong();
                        numUAVs = input.readInt();
                        this.masterOrder = new long[numUAVs];
                        Arrays.fill(this.masterOrder,-1);
                        String formationName = input.readString();
                        flightFormation = FormationFactory.newFormation(Formation.Layout.valueOf(formationName));
                        flightFormation.init(numUAVs,input.readDouble());
                        formationPos = input.readInt();
                        formationYaw = input.readDouble();
                        altitudeStep1 = input.readDouble();
                        altitudeStep2 = input.readDouble();
                        sendDataTakeOffAckMessage();
                    }
                } else if(type == MSMessageID.LIDER_ORDER || type == MSMessageID.LIDER_ORDER_FRAG){
                    state.set(MSParam.STATE_SENDING_LIDERORDER);
                }
            }
        }
    }

    private void sendDataTakeOffAckMessage() {
        gui.logVerboseUAV(MSText.SLAVE_TALKER_SENDING_TAKE_OFF_DATA_ACK);
        output.reset();
        output.writeShort(MSMessageID.TAKE_OFF_DATA_ACK);
        output.writeLong(numUAV);
        output.flush();
        byte[] message = Arrays.copyOf(outBuffer,output.position());
        commLink.sendBroadcastMessage(message);
    }

    private void respondToMasterExcludeMessage(AtomicInteger state) {
        while (state.get() == MSParam.STATE_EXCLUDE) {
            inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
            if (inBuffer != null) {
                input.setBuffer(inBuffer);
                short type = input.readShort();
                if(type == MSMessageID.EXCLUDE){
                    long id = input.readLong();
                    if(id == numUAV){
                        sendExcludeMessage();
                    }
                }
                if(type == MSMessageID.TAKE_OFF_DATA){
                    state.set(MSParam.STATE_SENDING_TAKEOFFDATA);
                }
            }
        }
    }

    private void sendExcludeMessage() {
        gui.logVerboseUAV(MSText.SLAVE_TALKER_SENDING_EXCLUDE);
        output.reset();
        output.writeShort(MSMessageID.EXCLUDE);
        output.writeLong(numUAV);
        output.writeBoolean(exclude);
        output.flush();
        byte[] message = Arrays.copyOf(outBuffer, output.position());
        commLink.sendBroadcastMessage(message);
    }
}

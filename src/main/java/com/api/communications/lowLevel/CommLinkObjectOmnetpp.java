package com.api.communications.lowLevel;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Text;
import org.json.JSONObject;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;


public class CommLinkObjectOmnetpp implements InterfaceCommLinkObject{
    private DatagramSocket sendSocket;
    private DatagramSocket listeningSocket;
    private DatagramPacket sendPacket;
    private final MessageQueue[] mBuffer;

    public CommLinkObjectOmnetpp(String ip, int port){
        int numUAVs = API.getArduSim().getNumUAVs();
        mBuffer = new MessageQueue[numUAVs];
        for (int i = 0; i < numUAVs; i++) {
            mBuffer[i] = new MessageQueue();
        }

        try {
            sendSocket = new DatagramSocket();
            sendPacket = new DatagramPacket(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH],
                    LowLevelCommLink.DATAGRAM_MAX_LENGTH,
                    InetAddress.getByName(ip),
                    port);

            listeningSocket =  new DatagramSocket(1000+port);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            ArduSimTools.closeAll(Text.THREAD_START_ERROR);
        }
        listenToOmnetpp().start();
    }

    @Override
    public void sendBroadcastMessage(int numUAV, byte[] message) {
        sendPacket.setData(message);
        try {
            sendSocket.send(sendPacket);
        } catch (IOException e) {
            API.getGUI(numUAV).logUAV(Text.MESSAGE_ERROR);
        }
    }

    @Override
    public byte[] receiveMessage(int numUAV, int socketTimeout) {
        Message m = mBuffer[numUAV].pollFirst();
        if(m != null){
            return m.message;
        }else{
            return null;
        }
    }

    private Thread listenToOmnetpp(){
        return new Thread(() -> {
            while(true){
                JSONObject msg = getMessage();
                msg.remove("OMNETPP_TYPE");

                int sender = msg.getInt("senderID");
                int receiver = msg.getInt("receiverID");
                byte[] message = msg.toString().getBytes(StandardCharsets.UTF_8);
                Message sendingMessage = new Message(sender,System.nanoTime(), message);
                mBuffer[receiver].offerLast(sendingMessage);
            }
        });
    }

    private JSONObject getMessage() {
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            listeningSocket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            return new JSONObject(received);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

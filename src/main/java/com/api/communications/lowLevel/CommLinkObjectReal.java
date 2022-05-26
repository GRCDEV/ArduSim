package com.api.communications.lowLevel;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Text;

import java.io.IOException;
import java.net.*;

class CommLinkObjectReal implements InterfaceCommLinkObject {

    /**
     * Port used for the sockets
     */
    private final int port;
    /**
     * Socket used to send
     */
    private DatagramSocket sendSocket;
    /**
     * Packet that will be send
     */
    private DatagramPacket sendPacket;
    /**
     * Socket to receive
     */
    private DatagramSocket receiveSocket;
    /**
     * Packet that will be received
     */
    private DatagramPacket receivePacket;
    /**
     * For the statistics: Total number of packages send
     */
    private int totalPackagesSend;
    /**
     * For the statistics: Total number of packages received
     */
    private int totalPackagesReceived;

    private final String ip;
    /**
     * Constructor for CommLinkObject used only for real UAVs
     * @param port: port used for communication
     */
    public CommLinkObjectReal(String ip, int port, boolean broadcast) {
        this.port = port;
        this.ip = ip;

        if(broadcast){
            initForAdHocUse(ip,port);
        }else{
            initForUDPServerUse(ip,port);
        }
    }

    private void initForAdHocUse(String ip, int port) {
        try {
        sendSocket = new DatagramSocket();
        sendPacket = new DatagramPacket(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH],
                LowLevelCommLink.DATAGRAM_MAX_LENGTH,
                InetAddress.getByName(ip),
                port);
        receiveSocket = new DatagramSocket(port);
        sendSocket.setBroadcast(true);
        receiveSocket.setBroadcast(true);
        receivePacket = new DatagramPacket(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH], LowLevelCommLink.DATAGRAM_MAX_LENGTH);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            ArduSimTools.closeAll(Text.THREAD_START_ERROR);
        }
    }

    private void initForUDPServerUse(String ip, int port) {
        try {
            sendSocket = new DatagramSocket();
            sendPacket = new DatagramPacket(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH],
                    LowLevelCommLink.DATAGRAM_MAX_LENGTH,
                    InetAddress.getByName(ip),
                    port);
            receiveSocket = new DatagramSocket(port+1);
            receivePacket = new DatagramPacket(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH], LowLevelCommLink.DATAGRAM_MAX_LENGTH);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            ArduSimTools.closeAll(Text.THREAD_START_ERROR);
        }
    }

    /**
     * Method used to send a broadcast message in the ether.
     * @param numUAV: identifier of the UAV sending the message
     * @param message: Message to be send
     */
    @Override
    public void sendBroadcastMessage(int numUAV, byte[] message) {
        sendPacket.setData(message);
        try {
            sendSocket.send(sendPacket);
            totalPackagesSend++;
        } catch (IOException e) {
            API.getGUI(numUAV).logUAV(Text.MESSAGE_ERROR);
        }
    }
    /**
     * Method used to receive a message
     * @param numUAV: not used (only for simulated UAVs)
     * @param socketTimeout: Timeout (ms) to wait for a message
     * @return The messages (in bytes)
     */
    @Override
    public byte[] receiveMessage(int numUAV,int socketTimeout) {
        receivePacket.setData(new byte[LowLevelCommLink.DATAGRAM_MAX_LENGTH], 0, LowLevelCommLink.DATAGRAM_MAX_LENGTH);
        try {
            if (socketTimeout > 0) {
                receiveSocket.setSoTimeout(socketTimeout);
            }
            receiveSocket.receive(receivePacket);
            totalPackagesReceived++;
            return receivePacket.getData();
        } catch (IOException e) { return null; }
    }
    /**
     * @return A string with all information about this communication
     */
    @Override
    public String toString() {
        return  "\n\t" + Text.BROADCAST_IP + " " + ip +
                "\n\t" + Text.BROADCAST_PORT + " " + port +
                "\n\t" + Text.TOT_SENT_PACKETS + " " + totalPackagesSend +
                "\n\t" + Text.TOT_PROCESSED + " " + totalPackagesReceived;
    }
}

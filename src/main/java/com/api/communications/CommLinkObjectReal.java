package com.api.communications;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Text;
import com.uavController.UAVParam;

import java.io.IOException;
import java.net.*;

public class CommLinkObjectReal implements InterfaceCommLinkObject {

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
    /**
     * Constructor for CommLinkObject used only for real UAVs
     * @param port: port used for communication
     */
    public CommLinkObjectReal(int port) {
        this.port = port;
        try {
            sendSocket = new DatagramSocket();
            sendSocket.setBroadcast(true);
            sendPacket = new DatagramPacket(new byte[CommLink.DATAGRAM_MAX_LENGTH],
                    CommLink.DATAGRAM_MAX_LENGTH,
                    InetAddress.getByName(UAVParam.broadcastIP),
                    port);
            receiveSocket = new DatagramSocket(port);
            receiveSocket.setBroadcast(true);
            receivePacket = new DatagramPacket(new byte[CommLink.DATAGRAM_MAX_LENGTH], CommLink.DATAGRAM_MAX_LENGTH);
        } catch (SocketException | UnknownHostException e) {
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
        receivePacket.setData(new byte[CommLink.DATAGRAM_MAX_LENGTH], 0, CommLink.DATAGRAM_MAX_LENGTH);
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
        return  "\n\t" + Text.BROADCAST_IP + " " + UAVParam.broadcastIP +
                "\n\t" + Text.BROADCAST_PORT + " " + port +
                "\n\t" + Text.TOT_SENT_PACKETS + " " + totalPackagesSend +
                "\n\t" + Text.TOT_PROCESSED + " " + totalPackagesReceived;
    }
}

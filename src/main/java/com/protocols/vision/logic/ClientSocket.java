package com.protocols.vision.logic;

import com.api.API;
import com.api.GUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocket {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final GUI gui;
    
    /**
     * Opens a new TCP socket (as a client)
     * @param ip: ip adress of the socket
     * @param port: port number of the socket
     */
	public ClientSocket(String ip,int port) {
		this.gui = API.getGUI(0);
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
        	gui.warn("ERROR", "could not set up a connection with python.");
        }
	}
	
	/**
	 * Sends a message with the use of the open socket and reads the responds
	 * @param message
	 * @return respons
	 */
	public String sendMessage(String message) {
        out.println(message);
        String respons = null;
        try {
            respons = in.readLine();
        } catch (IOException e) {
            gui.warn("ERROR", "could not send message.");
            return "NACK";
        }
        return respons;
    }

	/**
	 * closes the connection
	 */
	public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * checks if socket is connected
	 * @return true if client is connected
	 */
	public Boolean isConnected() {
		return clientSocket.isConnected();
	}
}

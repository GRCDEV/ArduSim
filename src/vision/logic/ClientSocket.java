package vision.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import api.API;
import main.api.GUI;

public class ClientSocket {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private GUI gui;
    
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
	public String sendMessage(String msg) {
        out.println(msg);
        String resp = null;
        try {
            resp = in.readLine();
        } catch (IOException e) {
            gui.warn("ERROR", "could not send message.");
            return "NACK";
        }
        return resp;
    }

	public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public Boolean isConnected() {
		return clientSocket.isConnected();
	}
}

package swarmprot.logic;

import java.net.SocketException;
import java.net.UnknownHostException;

import api.SwarmHelper;
import main.Param;
import uavController.UAVParam;

public class SwarmProtHelper {
	public static void initializeProtocolDataStructures() {
		
		
		SwarmProtParam.masterHeading = 0.0;
		UAVParam.newLocation = new float[Param.numUAVs][2];

	}

	public static void startSwarmThreads() throws SocketException, UnknownHostException {
			//cuando sea real se tendra que hacer un hilo de lectura y escritura en cada dron
			for (int i = 0; i < Param.numUAVs; i++) {
				(new Listener(i)).start();
				(new Talker(i)).start();
			}
			SwarmHelper.log(SwarmProtText.ENABLING);
	}
}

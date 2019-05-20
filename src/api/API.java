package api;

import java.util.concurrent.atomic.AtomicReferenceArray;

import main.Param;
import main.communications.CommLink;
import uavController.UAVParam;

public class API {
	
	private static volatile AtomicReferenceArray<CommLink> publicCommLink = null;
	private static final Object lock = new Object();
	
	private API() {}
	
	/**
	 * Get the communication link needed for UAV-to-UAV communications.
	 * @param numUAV This specific UAV position in the data arrays (see documentation).
	 * @return Communication link used by the UAV to communicate with the other UAVs.
	 */
	public static CommLink getCommLink(int numUAV) {
		
		synchronized (lock) {
			if (API.publicCommLink == null) {
				API.publicCommLink = new AtomicReferenceArray<CommLink>(Param.numUAVs);
				for (int i = 0; i < Param.numUAVs; i++) {
					API.publicCommLink.set(i, new CommLink(i, Param.numUAVs, UAVParam.broadcastPort));
				}
			}
		}
		
		return API.publicCommLink.get(numUAV);
	}
	
	
	
	
	
}

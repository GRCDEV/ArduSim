package main.api.masterslavepattern;

import api.API;
import es.upv.grc.mapper.Location2DUTM;
import main.Param;
import main.api.ArduSim;
import main.api.masterslavepattern.discovery.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Object used to coordinate UAVs with the master-slave pattern.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MasterSlaveHelper {

	private int numUAV;
	private ArduSim ardusim;
	
	@SuppressWarnings("unused")
	private MasterSlaveHelper() {}
	
	public MasterSlaveHelper(int numUAV) {
		this.numUAV = numUAV;
		this.ardusim = API.getArduSim();
	}
	
	/**
	 * Check if this UAV is master in the Master-Slave pattern. A UAV is master if the MAC address of the network adapter is included in the list loaded from ardusim.ini file.
	 * @return true if this UAV is master.
	 */
	public boolean isMaster() {
		boolean b = false;
		int role = Param.role;
		if (role == ArduSim.MULTICOPTER) {
			/* You get the id = f(MAC addresses) for real drone */
			long thisUAVID = Param.id[0];
			for (int i = 0; i < MSParam.macIDs.length; i++) {
				if (thisUAVID == MSParam.macIDs[i]) {
					return true;
				}
			}
		} else if (role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			if (numUAV == MSParam.MASTER_POSITION) {
				return true;
			}
		}
		return b;
	}
	
	/**
	 * Join as slave UAV to the process needed to make the master UAV notice the presence of the slaves, before starting any protocol based on the master-slave pattern. Blocking method.
	 */
	public void DiscoverMaster() {
		
		AtomicBoolean finished = new AtomicBoolean();
		
		DiscoverSlaveListener listener = new DiscoverSlaveListener(numUAV);
		listener.start();
		DiscoverSlaveTalker talker = new DiscoverSlaveTalker(numUAV, finished);
		talker.start();
		try {
			listener.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finished.set(true);
	}
	
	/**
	 * Join as master UAV to the process needed to make it UAV notice the presence of the slaves, before starting any protocol based on the master-slave pattern. Blocking method.
	 * @param progressListener listener needed by the master UAV to define when to stop the process.
	 * @return Map with the slaves ID as key, and their current location as values.
	 */
	public Map<Long, Location2DUTM> DiscoverSlaves(DiscoveryProgressListener progressListener) {
		
		AtomicBoolean finished = new AtomicBoolean();
		ConcurrentHashMap<Long, Location2DUTM> discovered = new ConcurrentHashMap<>();//TODO cambiar a atomicreference de hashmap
		
		DiscoverMasterListener listener = new DiscoverMasterListener(numUAV, discovered, progressListener);
		listener.start();
		DiscoverMasterTalker talker = new DiscoverMasterTalker(numUAV, finished);
		talker.start();
		try {
			listener.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		finished.set(true);
		ardusim.sleep(MSParam.DISCOVERY_TIMEOUT);
		return discovered;
	}
	
}

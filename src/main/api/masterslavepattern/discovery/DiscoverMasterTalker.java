package main.api.masterslavepattern.discovery;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import main.Text;
import main.api.ArduSim;
import main.api.communications.CommLink;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;

/**
 * Thread used by the master UAV to detect slaves.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class DiscoverMasterTalker extends Thread {
	
	private AtomicBoolean finished;
	private ArduSim ardusim;
	private InternalCommLink commLink;
	
	@SuppressWarnings("unused")
	private DiscoverMasterTalker() {}
	
	public DiscoverMasterTalker(int numUAV, AtomicBoolean finished) {
		super(Text.DISCOVERY_MASTER_TALKER + numUAV);
		this.finished = finished;
		this.ardusim = API.getArduSim();
		this.commLink = InternalCommLink.getCommLink(numUAV);
	}

	@Override
	public void run() {
		
		byte[] outBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		Output output = new Output(outBuffer);
		output.writeShort(MSMessageID.DISCOVERING);
		output.flush();
		byte[] message = Arrays.copyOf(outBuffer, output.position());
		output.close();
		
		long cicleTime = System.currentTimeMillis();
		long waitingTime;
		while (!finished.get()) {
			commLink.sendBroadcastMessage(message);
			
			cicleTime = cicleTime + MSParam.SENDING_PERIOD;
			waitingTime = cicleTime - System.currentTimeMillis();
			if (waitingTime > 0) {
				ardusim.sleep(waitingTime);
			}
		}
	}

}

package main.api.masterslavepattern.safeTakeOff;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.esotericsoftware.kryo.io.Input;

import api.API;
import es.upv.grc.mapper.Location2DGeo;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.LocationNotReadyException;
import main.Text;
import main.api.GUI;
import main.api.communications.CommLink;
import main.api.formations.FlightFormation;
import main.api.formations.FlightFormation.Formation;
import main.api.masterslavepattern.InternalCommLink;
import main.api.masterslavepattern.MSMessageID;
import main.api.masterslavepattern.MSParam;
import main.api.masterslavepattern.MSText;

/**
 * Thread used by a slave to coordinate the safe take off.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class TakeOffSlaveDataListenerThread extends Thread {

	private int numUAV;
	private long selfID;
	private Location2DUTM centerUAVLocation = null;
	private int numUAVs;
	private Formation formation;
	private FlightFormation flightFormation, landFormation;
	private int formationPos;
	private double formationYaw, altitudeStep1, altitudeStep2;
	private boolean exclude;
	private long[] masterOrder;
	private AtomicReference<SafeTakeOffContext> result;
	
	private long prevID, nextID;
	
	private InternalCommLink commLink;
	private byte[] inBuffer;
	private Input input;
	private GUI gui;
	
	@SuppressWarnings("unused")
	private TakeOffSlaveDataListenerThread() {}
	
	public TakeOffSlaveDataListenerThread(int numUAV, boolean exclude, AtomicReference<SafeTakeOffContext> result) {
		super(Text.SAFE_TAKE_OFF_SLAVE_CONTEXT_LISTENER + numUAV);
		this.numUAV = numUAV;
		this.selfID = API.getCopter(numUAV).getID();
		this.exclude = exclude;
		this.result = result;
		this.commLink = InternalCommLink.getCommLink(numUAV);
		this.inBuffer = new byte[CommLink.DATAGRAM_MAX_LENGTH];
		this.input = new Input(inBuffer);
		this.gui = API.getGUI(numUAV);
	}
	
	@Override
	public void run() {
		
		AtomicInteger state = new AtomicInteger(MSParam.STATE_EXCLUDE);
		new TakeOffSlaveDataTalkerThread(numUAV, exclude, state).start();
		
		gui.logVerboseUAV(MSText.SLAVE_LISTENER_WAITING_DATA);
		long lastReceivedData = 0;
		boolean takeOffDataReceived = false;
		long[][] auxMasterOrder = null;
		int max = CommLink.DATAGRAM_MAX_LENGTH;
		int maxSize = 0;
		int numFrags = 0;
		int fragsReceived = 0;
		short frag;
		
		while (state.get() == MSParam.STATE_EXCLUDE) {
			inBuffer = commLink.receiveMessage();
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.TAKE_OFF_DATA) {
					if (input.readLong() == selfID) {
						lastReceivedData = System.currentTimeMillis();
						input.readLong();	// The center UAV ID is no longer used
						centerUAVLocation = new Location2DUTM(input.readDouble(), input.readDouble());
						prevID = input.readLong();
						nextID = input.readLong();
						numUAVs = input.readInt();
						this.masterOrder = new long[numUAVs];
						formation = Formation.getFormation(input.readShort());
						flightFormation = FlightFormation.getFormation(formation, numUAVs, input.readDouble());
						landFormation = FlightFormation.getFormation(formation, numUAVs, input.readDouble());
						formationPos = input.readInt();
						formationYaw = input.readDouble();
						altitudeStep1 = input.readDouble();
						altitudeStep2 = input.readDouble();
						
						takeOffDataReceived = true;
					}
				}
				
				if (takeOffDataReceived && type == MSMessageID.LIDER_ORDER) {
					for (int i = 0; i < numUAVs; i++) {
						this.masterOrder[i] = input.readLong();
					}
					
					state.set(MSParam.STATE_SENDING_DATA);
				}
				
				if (takeOffDataReceived && type == MSMessageID.LIDER_ORDER_FRAG) {
					if (auxMasterOrder == null) {
						maxSize = Math.floorDiv(max - 2 - 2, 8);
						numFrags = (int) Math.ceil(numUAVs / (maxSize * 1.0));
						auxMasterOrder = new long[numFrags][];
					}
					frag = input.readShort();
					if (auxMasterOrder[frag] == null) {
						int subSize = Math.min(maxSize, numUAVs - frag * maxSize);
						auxMasterOrder[frag] = new long[subSize];
						for (int i = 0; i < subSize; i++) {
							auxMasterOrder[frag][i] = input.readLong();
						}
						fragsReceived++;
					}
					
					if (fragsReceived == numFrags) {
						int k = 0;
						for (int i = 0; i < auxMasterOrder.length; i++) {
							for (int j = 0; j < auxMasterOrder[i].length; j++) {
								this.masterOrder[k] = auxMasterOrder[i][j];
								k++;
							}
						}
						
						state.set(MSParam.STATE_SENDING_DATA);
					}
				}
			}
		}
		
		Location2DGeo target = null;
		try {
			target = flightFormation.getLocation(formationPos, centerUAVLocation, formationYaw).getGeo();
		} catch (LocationNotReadyException e) {
			e.printStackTrace();
			gui.exit(e.getMessage());
		}
		SafeTakeOffContext data = new SafeTakeOffContext(prevID, nextID, target, altitudeStep1, altitudeStep2, exclude,
				this.masterOrder, centerUAVLocation, numUAVs, flightFormation, landFormation, formationPos, formationYaw);
		this.result.set(data);
		
		gui.logVerboseUAV(MSText.SLAVE_LISTENER_WAITING_DATA_TIMEOUT);
		while (state.get() == MSParam.STATE_SENDING_DATA) {
			inBuffer = commLink.receiveMessage(MSParam.RECEIVING_TIMEOUT);
			if (inBuffer != null) {
				input.setBuffer(inBuffer);
				short type = input.readShort();
				
				if (type == MSMessageID.TAKE_OFF_DATA) {
					lastReceivedData = System.currentTimeMillis();
				}
			}
			
			if (System.currentTimeMillis() - lastReceivedData > MSParam.TAKE_OFF_DATA_TIMEOUT) {
				state.set(MSParam.STATE_SENDING_FINISHED);
			}
		}
		
		gui.logVerboseUAV(MSText.SLAVE_LISTENER_TAKE_OFF_DATA_END);
	}

}

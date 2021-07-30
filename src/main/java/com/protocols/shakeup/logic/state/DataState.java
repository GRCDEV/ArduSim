package com.protocols.shakeup.logic.state;

import com.esotericsoftware.kryo.io.Input;
import es.upv.grc.mapper.Location2DUTM;
import es.upv.grc.mapper.Location3DUTM;
import org.javatuples.Quartet;
import com.protocols.shakeup.logic.ShakeupListenerThread;
import com.protocols.shakeup.pojo.Param;
import com.protocols.shakeup.pojo.TargetFormation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class DataState extends State{

	private final ArrayList<Quartet<Long, Double, Double, Double>> messageInfo;
	private Location3DUTM targetLocation;
	private final Map<Long,Location2DUTM> UAVLocations2D;
	
	public DataState(int selfId, boolean isMaster, Map<Long, Location2DUTM> UAVsDetected) {
		super(selfId, isMaster);
		if(isMaster) {super.numUAVs = UAVsDetected.size();}
		super.stateNr = Param.DATA;
		
		gui.logUAV("Start state " + stateNr);
		gui.updateProtocolState("" + stateNr);
		
		this.UAVLocations2D = UAVsDetected;
		this.messageInfo = new ArrayList<Quartet<Long, Double, Double, Double>>();
	}

	@Override
	public void executeOnce() {
		if(isMaster) {
			TargetFormation targetFormation = Param.flightFormations[ShakeupListenerThread.getFormationIndex()];
			UAVLocations2D.put((long) selfId, copter.getLocationUTM());
			
			// gui.log("new flight formation: " + targetFormation.getFlightFormation().getFormationName());
			
			// calculate the next position of all the UAVs with the use of the safeTakeOff algorithm
			// set to takeoff algorithm to RANDOM or to SIMPLIFIED
			// TakeOffMasterDataListenerThread.selectedAlgorithm = Param.TAKE_OFF_ALGORITHM;
			// MatchCalculusThread mCT = new MatchCalculusThread(UAVLocations2D, targetFormation.getFlightFormation(),targetFormation.getHeading(), (long)selfId);
			// mCT.start();
			// while (mCT.isAlive()) {ardusim.sleep(Param.WAITING_TIME);}
			//Quartet<Integer, Long, Location2DUTM, Double>[] calculateData = mCT.getResult();
			Quartet<Integer, Long, Location2DUTM, Double>[] calculateData = null;
			
			for (int i = 0; i < calculateData.length; i++) {
				long id = calculateData[i].getValue1();
				Location2DUTM startLoc = UAVLocations2D.get(id);
				Location2DUTM targetLoc = calculateData[i].getValue2();
				//TODO check because there is something wrong witht he calculation of the angles
				// calculate the angle between start and target location
				double diffx = targetLoc.x - startLoc.x;
				double diffy = targetLoc.y - startLoc.y;
				double angle = Math.atan2(diffy, diffx);
				if(angle < 0 ) {angle += 2*Math.PI;} //clip it between 0 and 2*PI
				
				// check in which sector the UAV belongs
				double sectorWidth = 2*Math.PI /Param.NUMBER_OF_SECTORS;
				int sector = 0;
				sector = i%Param.NUMBER_OF_SECTORS;
				for(int j =0 ; j< Param.NUMBER_OF_SECTORS;j++) {
					double min = 0 + j*sectorWidth;
					double max = sectorWidth + j*sectorWidth;
					if(angle >= min && angle< max){
						sector = j;
						break;
					}
				}
				System.out.println("angle: " + Math.toDegrees(angle) + " Sector " + sector);
				// select the flying altitude of the UAV
				// an array is made with all the flying distances
				// to ensure that the gap between to adjacent sectors is as big as possible* the distance is alternated
				// * => not sure if this is the best solution but it is quick and has decent results
				int[] altitudes = new int[Param.NUMBER_OF_SECTORS];
				for(int j=0;j<Param.NUMBER_OF_SECTORS;j++) {altitudes[j] = j* Param.ALTITUDE_DIFF_SECTORS;}
				double z;
				if(sector %2 == 0) {z = altitudes[sector];}
				else {z = altitudes[Param.NUMBER_OF_SECTORS-(sector+1)];}
				
				// add info to message info
				double x = targetLoc.x;
				double y = targetLoc.y;
				messageInfo.add(new Quartet<Long, Double, Double, Double>(id,x,y,z));
				
				// The master will never receive it`s own message so therefore set his own targetLocation here
				if(id == selfId) {targetLocation = new Location3DUTM(x,y,z);}
			}
		}
	}

	@Override
	public void executeContinously() {}

	@Override
	public State transit(Boolean transit) {
		if(!isMaster && targetLocation != null) {super.send_ack = true;}
		if(transit) {
			if(isMaster) {return new MoveZState(selfId, isMaster, targetLocation, numUAVs);}
			else if(!isMaster && super.send_ack == true) {return new MoveZState(selfId,isMaster, targetLocation, numUAVs);}
		}
		return this;
	}
	
	@Override
	public byte[][] createMessage(){
		if(isMaster) {
			// The master sends in which state he is and gives the target location
			byte[][] message = new byte[numUAVs][];
			for(int i = 0 ;i< messageInfo.size();i++) {
				Quartet<Long, Double, Double, Double> q = messageInfo.get(i);
				output.reset();
				output.writeShort(stateNr);
				output.writeLong(q.getValue0()); // id
				output.writeDouble(q.getValue1()); // x
				output.writeDouble(q.getValue2()); // y
				output.writeDouble(q.getValue3()); // z
				message[i] = Arrays.copyOf(outBuffer, output.position());
			}
			return message;
		}else {
			if(send_ack) {
				// The slaves reply with an ack if they have received the state of the master
				output.reset();
				output.writeShort(Param.MESSAGE_ACK);
				output.writeShort(stateNr);
				output.writeLong(selfId);
				output.flush();
				byte[][] message = new byte[1][];
				message[0] = Arrays.copyOf(outBuffer, output.position());
				return message;
			}
		}
		return null;
	}

	
	@Override
	public void inspect(Input message) {
		long id = message.readLong();
		if(id == selfId) {
			targetLocation = new Location3DUTM(message.readDouble(),message.readDouble(),message.readDouble());
		}
	}

}

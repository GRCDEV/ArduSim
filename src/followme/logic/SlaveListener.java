package followme.logic;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Input;

import api.API;
import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.logic.SimParam;
import uavController.UAVParam;

public class SlaveListener extends Thread {

	private int idSlave;
	private int posFormacion;
	private boolean takeOff;
	private boolean masterReady;

	public SlaveListener(int idSlave) {
		this.idSlave = idSlave;
		this.takeOff = false;
		this.masterReady = false;
	}

	@Override
	public void run() {

		while (Param.simStatus != SimulatorState.READY_FOR_TEST) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		while (FollowMeParam.uavs[idSlave] != FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			recibirMessage(FollowMeParam.MsgTakeOff);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_MASTER) {
			recibirMessage(FollowMeParam.MsgCoordenadas);
			if (masterReady) {
				FollowMeParam.uavs[idSlave] = FollowMeState.FOLLOW;
				SwarmHelper.setSwarmState(idSlave, FollowMeParam.uavs[idSlave].getName());
			}
		}
		
		while (FollowMeParam.uavs[idSlave] == FollowMeState.LANDING_FOLLOWERS) {
			try {
				Param.controllers[idSlave].msgTarget(
						FollowMeParam.TypemsgTargetCoordinates,
						UAVParam.uavCurrentData[idSlave].getGeoLocation().getY(),
						UAVParam.uavCurrentData[idSlave].getGeoLocation().getX(),
						0.0, 
						1.0, 
						false, 
						1.0, 
						1.0, 
						1.0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void recibirMessage(int type) {

		String msg = null;
		byte[] message = null;
		Input in = new Input();
		message = API.receiveMessage(idSlave);

		in.setBuffer(message);
		int idSender = in.readInt();
		int typeMsg = in.readInt();
		if (type == typeMsg || (type == FollowMeParam.MsgCoordenadas && typeMsg == FollowMeParam.MsgLanding)) {
			switch (typeMsg) {
			case FollowMeParam.MsgTakeOff:
				if(!takeOff) {
					int size = in.readInt();
					int[] posiciones = in.readInts(size);
					for (int i = 0; i < posiciones.length; i++) {
						if (posiciones[i] == idSlave) {
							this.posFormacion = i;
							break;
						}
					}
					msg = "Master indica el despegue y la posicion en la formacion sera pos:" + this.posFormacion;
					takeOffIndividual(idSlave, FollowMeParam.AlturaInitFollowers);
					takeOff = true;	
				}
				break;
				
			case FollowMeParam.MsgCoordenadas:

				double lat, lon, heading, z, speedX, speedY, speedZ;
				lat = in.readDouble();
				lon = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();
				if (this.masterReady) {
					Point2D geoActual = UAVParam.uavCurrentData[idSlave].getGeoLocation();
					Resultado res = Formacion.getPosition(FollowMeParam.FormacionLineaHorizontal,
							new GeoCoordinates(lat, lon), heading,
							new GeoCoordinates(geoActual.getY(), geoActual.getX()), this.posFormacion,
							new Triplet<Double, Double, Double>(speedX, speedY, speedZ));
					try {
						Param.controllers[idSlave].msgTarget(FollowMeParam.TypemsgTargetCoordinates,
								res.getGeo().latitude, res.getGeo().longitude, z, 1.0, false, speedX, speedY, speedZ);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					this.masterReady = true;
				}
				break;
				
			case FollowMeParam.MsgLanding:
				
				FollowMeParam.uavs[idSlave] = FollowMeState.LANDING_FOLLOWERS;
				SwarmHelper.setSwarmState(idSlave, FollowMeParam.uavs[idSlave].getName());
				break;
				
			default:
				
				break;
			}
			System.out.println("Recibidos" + idSlave + "<--" + idSender + ": " + msg);
		}
		System.out.println("SlaveListener "+idSlave+" msg ignorado "+typeMsg);
		

	}

	public static void takeOffIndividual(int numUAV, double altitude) {
		// Taking Off to first altitude step
		if (!API.setMode(numUAV, UAVParam.Mode.GUIDED) || !API.armEngines(numUAV) || !API.doTakeOff(numUAV, altitude)) {
			GUIHelper.exit(Text.TAKE_OFF_ERROR_1 + " " + Param.id[numUAV]);
		}

		// The application must wait until all UAVs reach the planned altitude
		while (UAVParam.uavCurrentData[numUAV].getZRelative() < 0.95 * altitude) {
			if (Param.VERBOSE_LOGGING) {
				MissionHelper.log(SimParam.prefix[numUAV] + Text.ALTITUDE_TEXT + " = "
						+ String.format("%.2f", UAVParam.uavCurrentData[numUAV].getZ()) + " " + Text.METERS);
			}

			GUIHelper.waiting(UAVParam.ALTITUDE_WAIT);
		}
	}
}

package followme.logic;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.javatuples.Triplet;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;
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

		while (!Tools.isSetupFinished()) {
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
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			}
		}
		
		while (FollowMeParam.uavs[idSlave] == FollowMeState.LANDING_FOLLOWERS) {
			try {
				Point2D.Double geo = Copter.getGeoLocation(idSlave);
				Copter.getController(idSlave).msgTarget(
						FollowMeParam.TypemsgTargetCoordinates,
						geo.getY(),
						geo.getX(),
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
		message = Copter.receiveMessage(idSlave);

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
					Copter.takeOff(idSlave, FollowMeParam.AlturaInitFollowers);
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
					Point2D geoActual = Copter.getGeoLocation(idSlave);
					Resultado res = Formacion.getPosition(FollowMeParam.FormacionLineaHorizontal,
							new GeoCoordinates(lat, lon), heading,
							new GeoCoordinates(geoActual.getY(), geoActual.getX()), this.posFormacion,
							new Triplet<Double, Double, Double>(speedX, speedY, speedZ));
					try {
						Copter.getController(idSlave).msgTarget(FollowMeParam.TypemsgTargetCoordinates,
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
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
				break;
				
			default:
				
				break;
			}
			System.out.println("Recibidos" + idSlave + "<--" + idSender + ": " + msg);
		}
		System.out.println("SlaveListener "+idSlave+" msg ignorado "+typeMsg);
		

	}
}

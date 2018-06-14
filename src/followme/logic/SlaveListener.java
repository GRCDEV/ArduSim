package followme.logic;

import java.awt.geom.Point2D;
import java.io.IOException;

import com.esotericsoftware.kryo.io.Input;

import api.Copter;
import api.GUI;
import api.Tools;
import api.pojo.FlightMode;
import api.pojo.GeoCoordinates;
import followme.logic.FollowMeParam.FollowMeState;

public class SlaveListener extends Thread {

	private int numUAV;
	private long idSlave;
	private int idMaster;
	
	private int[] idsFormacion;
	
	private boolean takeOff;
	private boolean landing;

	public SlaveListener(int numUAV) {
		this.numUAV = numUAV;
		this.idSlave = Tools.getIdFromPos(numUAV);
		this.takeOff = false;
		this.landing = false;

	}

	@Override
	public void run() {

		GUI.log("SlaveListener " + numUAV + " run");
		
		while (!Tools.areUAVsReadyForSetup()) {
			Tools.waiting(100);
		}
		
		FollowMeParam.uavs[numUAV] = FollowMeState.SEND_ID;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		
		while (!Tools.isSetupInProgress()) {
			Tools.waiting(200);
		}
		
		FollowMeParam.uavs[numUAV] = FollowMeState.WAIT_TAKE_OFF_SLAVE;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		
		// Recibir MsgTakeOff
		byte[] message;
		Input in = new Input();
		int idSender, typeRecibido;
		boolean received = false;
		Point2D.Double offset = null;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (typeRecibido == FollowMeParam.MsgTakeOff) {
				idMaster = idSender;
				int size = in.readInt();
				idsFormacion = in.readInts(size);
				// Precálculo de la formación
				int posFormacion = -1;
				for (int i = 0; i < idsFormacion.length; i++) {
					if (idsFormacion[i] == idSlave) {
						posFormacion = i;
					}
				}// TODO acción si no lo encuentra
				
				switch (FollowMeParam.FormacionUsada) {
				case FollowMeParam.FormacionLinea:
					offset = Formacion.getOffsetLineal(posFormacion);
					break;
				case FollowMeParam.FormacionMatriz:
					offset = Formacion.getOffsetMatrix(posFormacion, size);
					break;
				case FollowMeParam.FormacionCircular:
//					res = getPositionCircular(geoMaster, headingMaster, posFormacion);
					break;
				default:
					break;
				}
				//TODO comprobar que offset != null
				FollowMeParam.uavs[numUAV] = FollowMeState.TAKE_OFF;
				GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
				Copter.takeOff(numUAV, FollowMeParam.AlturaInitFollowers);
				received = true;
			}
//			else {
//				GUI.log("SlaveListener "+idSlave+" Msg no enviado por MASTER, enviado por: "+idSender+" Msg tipo: "+FollowMeParam.getTypeMessage(typeRecibido));
//			}
		}
		
		FollowMeParam.uavs[numUAV] = FollowMeState.WAIT_MASTER;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		
		received = false;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (idSender == idMaster && typeRecibido == FollowMeParam.MsgCoordenadas) {
				double x, y, heading, z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
//				speedX = in.readDouble();
//				speedY = in.readDouble();
//				speedZ = in.readDouble();
				
				double incX, incY;
				if (true) {
					incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
					incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);
				} 
//				else {
//
//					// Mejorar los movimientos del dron 2 y 4 en posiciones impares
//					incY = -offset.y * Math.cos(heading - Math.PI)
//							- offset.x * Math.sin(heading - Math.PI);
//					incX = offset.x * Math.cos(heading - Math.PI)
//							+ -offset.y * Math.sin(heading - Math.PI);
//
//				}
				
				received = true;
			}
		}
		
		FollowMeParam.uavs[numUAV] = FollowMeState.FOLLOW;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		
		received = false;
		while (!received) {
			message = Copter.receiveMessage(numUAV); /// Espera bloqueante
			in.setBuffer(message);
			idSender = in.readInt();
			typeRecibido = in.readInt();
			if (idSender == idMaster && typeRecibido == FollowMeParam.MsgCoordenadas) {
				double x, y, heading, z, speedX, speedY, speedZ;
				x = in.readDouble();
				y = in.readDouble();
				heading = in.readDouble();
				z = in.readDouble();
				speedX = in.readDouble();
				speedY = in.readDouble();
				speedZ = in.readDouble();
				
				double incX, incY;
				if (true) {
					incY = offset.y * Math.cos(heading) - offset.x * Math.sin(heading);
					incX = offset.x * Math.cos(heading) + offset.y * Math.sin(heading);
				} 
//				else {
//
//					// Mejorar los movimientos del dron 2 y 4 en posiciones impares
//					incY = -offset.y * Math.cos(heading - Math.PI)
//							- offset.x * Math.sin(heading - Math.PI);
//					incX = offset.x * Math.cos(heading - Math.PI)
//							+ -offset.y * Math.sin(heading - Math.PI);
//
//				}
				
				x+=incX;
				y+=incY;
				
				GeoCoordinates geo = Tools.UTMToGeo(x, y);
				try {
					Copter.getController(numUAV).msgTarget(FollowMeParam.TypemsgTargetCoordinates, 
							geo.latitude, 
							geo.longitude, 
							z, 
							1.0, 
							false, 
							speedX, speedY, speedZ);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}else if (idSender == idMaster && typeRecibido == FollowMeParam.MsgLanding){
				
				Copter.setFlightMode(numUAV, FlightMode.LAND_ARMED);

				received = true;
			}
		}
		
		FollowMeParam.uavs[numUAV] = FollowMeState.LANDING_FOLLOWERS;
		GUI.updateprotocolState(numUAV, FollowMeParam.uavs[numUAV].getName());
		
		
		
		
		
		
		/*

		while (FollowMeParam.uavs[idSlave] == FollowMeState.TAKE_OFF) {// Cambio de estado en SlaveTalker
			Tools.waiting(1000);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_MASTER) {
			Input in = new Input();
			byte[] message = Copter.receiveMessage(idSlave); /// Espera bloqueante
			in.setBuffer(message);
			int idSender = in.readInt();
			if (idSender == FollowMeParam.posMaster) {
				int typeMsg = in.readInt();
				if (typeMsg == FollowMeParam.MsgCoordenadas) {
					double lat, lon, heading, z, speedX, speedY, speedZ;
					lat = in.readDouble();
					lon = in.readDouble();
					heading = in.readDouble();
					z = in.readDouble();
					speedX = in.readDouble();
					speedY = in.readDouble();
					speedZ = in.readDouble();
					GUI.log("SlaveListener " + idSlave + " Recibe coordenadas ");

				}
			}

		}
		*/
		GUI.log("SlaveListener " + idSlave + " Finaliza");

	}

	/*public void run1() {

		while (FollowMeParam.uavs[idSlave] != FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			Tools.waiting(1000);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_TAKE_OFF_SLAVE) {
			// recibirMessage(FollowMeParam.MsgTakeOff);
			if (!takeOff) {
				byte[] message = null;
				Input in = new Input();
				message = Copter.receiveMessage(idSlave); /// Espera bloqueante
				in.setBuffer(message);
				int idSender = in.readInt();
				if (idSender == FollowMeParam.posMaster) {
					int typeRecibido = in.readInt();
					if (typeRecibido == FollowMeParam.MsgTakeOff) {

						takeOff = true;

						// if (bufferMsg.get() != message) {
						// //bufferMsg.set(message);
						// }
					}
				}
			}
			if (takeOff) {
				FollowMeParam.uavs[idSlave] = FollowMeState.TAKE_OFF;
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
				Copter.takeOff(idSlave, FollowMeParam.AlturaInitFollowers);
			}
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.TAKE_OFF) {
			Tools.waiting(500);
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.WAIT_MASTER) {
			// recibirMessage(FollowMeParam.MsgCoordenadas);
			byte[] message = null;
			Input in = new Input();
			message = Copter.receiveMessage(idSlave); /// Espera bloqueante
			in.setBuffer(message);
			int idSender = in.readInt();
			if (idSender == FollowMeParam.posMaster) {
				int typeMsg = in.readInt();
				if (typeMsg == FollowMeParam.MsgCoordenadas) {
					// bufferMsg.set(message);
					double lat, lon, heading, z, speedX, speedY, speedZ;
					lat = in.readDouble();
					lon = in.readDouble();
					heading = in.readDouble();
					z = in.readDouble();
					speedX = in.readDouble();
					speedY = in.readDouble();
					speedZ = in.readDouble();

					try {
						Copter.getController(idSlave).msgTarget(FollowMeParam.TypemsgTargetCoordinates,
								// res.getGeo().latitude,
								// res.getGeo().longitude,
								lat, lon, z, 1.0, false, speedX, speedY, speedZ);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else if (typeMsg == FollowMeParam.MsgLanding) {
					FollowMeParam.uavs[idSlave] = FollowMeState.LANDING_FOLLOWERS;
					GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());

				} else {
					System.out.println("SlaveListener " + idSlave + " Meensaje type no adecuado:" + typeMsg);
				}
			}

		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.FOLLOW) {
			// recibirMessage(FollowMeParam.MsgCoordenadas);
			byte[] message = null;
			Input in = new Input();
			message = Copter.receiveMessage(idSlave); /// Espera bloqueante
			in.setBuffer(message);
			int idSender = in.readInt();
			if (idSender == FollowMeParam.posMaster) {
				int typeMsg = in.readInt();
				if (typeMsg == FollowMeParam.MsgCoordenadas) {
					if (bufferMsg.get() != message) {
						bufferMsg.set(message);
					}
				} else {
					System.out.println("Mensaje type no adecuado:" + typeMsg);
				}
			}

			// Tools.waiting(10);
			// Thread.yield();
		}

		while (FollowMeParam.uavs[idSlave] == FollowMeState.LANDING_FOLLOWERS) {
			if (!landing) {
				byte[] message = null;
				Input in = new Input();
				message = Copter.receiveMessage(idSlave); /// Espera bloqueante
				in.setBuffer(message);
				int idSender = in.readInt();
				if (idSender == FollowMeParam.posMaster) {
					int typeRecibido = in.readInt();
					if (typeRecibido == FollowMeParam.MsgLanding) {
						// if(bufferMsg.get() != message) {
						// bufferMsg.set(message);
						// }
						Copter.setFlightMode(idSlave, FlightMode.LAND_ARMED);
						Tools.waiting(100);
						landing = true;
					}
				}
			}

			FlightMode flyMode = Copter.getFlightMode(idSlave);
			if (flyMode == FlightMode.LAND) {
				FollowMeParam.uavs[idSlave] = FollowMeState.FINISH;
				GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
			} else if (flyMode != FlightMode.LAND_ARMED) {
				Point2D.Double geo = Copter.getGeoLocation(idSlave);
				Copter.setFlightMode(idSlave, FlightMode.LAND_ARMED);
			}
			Tools.waiting(500);
		}

	}

	private void recibirMessage(int typeEsperado) {

		String msg = null;
		boolean err = false;
		byte[] message = null;
		Input in = new Input();
		message = Copter.receiveMessage(idSlave);
		in.setBuffer(message);
		int idSender = in.readInt();
		if (idSender == FollowMeParam.posMaster) {
			int typeRecibido = in.readInt();
			if (typeEsperado == typeRecibido
					|| (typeEsperado == FollowMeParam.MsgCoordenadas && typeRecibido == FollowMeParam.MsgLanding)) {
				switch (typeRecibido) {
				case FollowMeParam.MsgTakeOff:
					if (!takeOff) {
						bufferMsg.set(message);
						numMaxUAV.set(in.readInt());
						int[] posiciones = in.readInts(numMaxUAV.get());
						for (int i = 0; i < posiciones.length; i++) {
							if (posiciones[i] == idSlave) {
								this.posFormacion.set(i);
								break;
							}
						}
						msg = "Master indica el despegue y la posicion en la formacion sera pos:"
								+ this.posFormacion.get();
						takeOff = true;
					}
					break;

				case FollowMeParam.MsgCoordenadas:
					bufferMsg.set(message);

					break;

				case FollowMeParam.MsgLanding:
					bufferMsg.set(message);

					FollowMeParam.uavs[idSlave] = FollowMeState.LANDING_FOLLOWERS;
					GUI.updateprotocolState(idSlave, FollowMeParam.uavs[idSlave].getName());
					msg = "Aterrizar";

					break;

				default:

					break;
				}
				if (msg != null) {
					if (err) {
						System.err.println("SlaveListener" + idSlave + "<--" + idSender + ": " + msg);

					} else {
						System.out.println("SlaveListener" + idSlave + "<--" + idSender + ": " + msg);
					}
				}

			}

		}

	}*/
}
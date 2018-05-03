package followme.logic;

import java.util.Arrays;

import com.esotericsoftware.kryo.io.Output;

import api.API;
import api.SwarmHelper;
import uavController.UAVParam;
import uavController.UAVParam.Mode;

public class MasterThread extends Thread{

	
	
	@Override
	public void run() {
		byte[] buffer = new byte[UAVParam.DATAGRAM_MAX_LENGTH];
		Output out = new Output(buffer);
		
		int count = 0;
		SwarmHelper.log("Iniciando Master");
		while(true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int idUAV = 0;
			double latitud ;
			double longitud ;
			double z;
						
			latitud = UAVParam.uavCurrentData[0].getGeoLocation().getX();
			longitud = UAVParam.uavCurrentData[0].getGeoLocation().getY();
			z = UAVParam.uavCurrentData[0].getZ();
					
			out.writeInt(idUAV);
			out.writeDouble(latitud);
			out.writeDouble(longitud);
			out.writeDouble(z);
			out.flush();
			byte[] message = Arrays.copyOf(buffer, out.position());
			System.out.println("Enviado"+idUAV+"-->\t"+latitud+"\t"+longitud+"\t"+z);
			API.sendBroadcastMessage(0, message);
			out.clear();
		}
		
	}
	

}

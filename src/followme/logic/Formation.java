package followme.logic;

import api.GUI;
import api.pojo.UTMCoordinates;

public class Formation {

	private Formation() {
		// TODO Auto-generated constructor stub
	}
	/* API: Parametros introducidos, posicion en la formacion, numero de UAVs esclavos
	 * 		Devuelve el offset, de la posicion a ala que pertenece el identificador en una matriz de esclavos.
	 */
	public static UTMCoordinates getOffsetMatrix(int posFormation, int numSlaves) {
		
		//GUI.log("Position Formation:"+ String.valueOf(posFormation) + "-NumSlaves:" + String.valueOf(numSlaves) );
		int posMasterLineal = (numSlaves + 1) / 2;
		int l = (int)Math.ceil(Math.sqrt(numSlaves + 1));
		
		int rowsBeforeMaster = posMasterLineal / l;
		int columnsBeforeMaster = posMasterLineal - rowsBeforeMaster * l;
		
		int posMasterX = columnsBeforeMaster;
		int posMasterY = rowsBeforeMaster;
		
		if (posFormation >= posMasterLineal) {
			posFormation++;
		}
		
		int rowsBeforeSlave = posFormation / l;
		int columnsBeforeSlave = posFormation - rowsBeforeSlave * l;
		
		int posSlaveX = columnsBeforeSlave;
		int posSlaveY = rowsBeforeSlave;
		
		double distX = (posSlaveX - posMasterX) * FollowMeParam.DistanceLinearOffset;
		double distY = (posSlaveY - posMasterY) * FollowMeParam.DistanceLinearOffset;
		
		//Point2D.Double offset = null;

		GUI.log("Position Formation:"+ String.valueOf(posFormation) + "-NumSlaves:" + String.valueOf(numSlaves) +"Resultados: distX=" + String.valueOf(distX) + "distY=" +String.valueOf(distY) );
		return new UTMCoordinates(distX, distY);
	}
	
	/* API:	Parametros de entrada posicion en una linea
	 * 		Devuelve el offset, de la posicion que adopta dependiendo el numero indicado.
	 * 		Los UAV esclavos se reparten a los extremos del master 
	 */
	
	public static UTMCoordinates getOffsetLineal(int posFormation) {
		double vert = -FollowMeParam.DistanciaSeparacionVertical;
		double horiz = FollowMeParam.DistanceLinearOffset * ((int) (posFormation + 2) / 2);
		if (posFormation % 2 == 0) {
		
			GUI.log("Position Formation:"+ String.valueOf(posFormation) + "-NumSlaves:?" +" Resultados: distX=" + String.valueOf(horiz) + "distY=" +String.valueOf(vert) );
			return new UTMCoordinates(horiz, vert);
		} else {
			GUI.log("Position Formation:"+ String.valueOf(posFormation) + "-NumSlaves:?" + " Resultados: distX=" + String.valueOf(-horiz) + "distY=" +String.valueOf(vert) );
			return new UTMCoordinates(-horiz, vert);
		}
	}
	
	/* API: PArametros de entrada la posicin en el circulo alrededero del master, numero de esclavo al formar el circulo
	 * 		Devuelve el offset, de la posicion que el esclavo adopta para realizar un cirrculo alrededor del master
	 * */
								
	public static UTMCoordinates getOffsetCircular(int posFormation, int numSlaves){
											   /*FollowMeParam.DistanceRadio*/
		double angulo = 2*Math.PI / numSlaves;
		//double vert = Math.sin(angulo * posFormation)* FollowMeParam.DistanceRadio;
		//double horiz = Math.cos(angulo * posFormation) *FollowMeParam.DistanceRadio;
		
		double vert = Math.sin(angulo * posFormation)* 65.32814824;
		double horiz = Math.cos(angulo * posFormation) * 65.32814824;
		
		
		//System.out.println("Vertical_:" + vert + "-Horizontal valor" + horiz);
		//GUI.log("Position Formation:"+ String.valueOf(posFormation) + "-NumSlaves: " + String.valueOf(numSlaves) + " Resultados: distX=" + String.valueOf(horiz) + "distY=" +String.valueOf(vert) );
		return new UTMCoordinates(horiz, vert);
	}

}
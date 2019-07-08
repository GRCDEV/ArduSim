package catchme.logic;

import api.API;
import api.pojo.location.Location2DUTM;
import main.api.ArduSimNotReadyException;

public class StartExpWithDraws {
	public static void move(String direction) {
		Location2DUTM current = CatchMeParams.startingLocation.getUTMLocation();
		//modificar current
		switch (direction) {
		case "left":
			current.setLocation(current.x - 5, current.y);
			break;
		case "right":
			current.setLocation(current.x + 5, current.y);
			break;
		case "up":
			current.setLocation(current.x, current.y + 5);
			break;
		case "down":
			current.setLocation(current.x, current.y - 5);
			break;
		}
		
		try {
			CatchMeParams.startingLocation.updateUTM(current);
		} catch (ArduSimNotReadyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CatchMeParams.targetLocationPX.set(API.getGUI(0).locatePoint(current));
		System.out.println(direction);
	}
}

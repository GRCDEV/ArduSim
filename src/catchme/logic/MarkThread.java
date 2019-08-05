package catchme.logic;

import api.API;
import main.api.ArduSim;

public class MarkThread extends Thread {
	
	private Mark mark;
	
	@SuppressWarnings("unused")
	private MarkThread() {}
	
	public MarkThread(Mark mark) {
		this.mark = mark;
	}
	
	@Override
	public void run() {
		ArduSim ardusim = API.getArduSim();
		
		while(true) {
			mark.move();
			
			ardusim.sleep(500);
		}
	}
}

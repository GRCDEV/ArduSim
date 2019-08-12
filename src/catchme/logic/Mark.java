package catchme.logic;

import api.API;
import api.pojo.location.Location2D;
import api.pojo.location.Location2DUTM;

public class Mark {

		private long tInit;
		private long tFin;
		private int nl, nr, nu, nd;
		private boolean restart;
		
		public Mark() {
			this.tInit = 0;
			this.tFin = 0;
			this.nl = 0;
			this.nr = 0;
			this.nu = 0;
			this.nd = 0;
			this.restart = true;
		}

		public synchronized void add(int nlAdd, int nrAdd, int nuAdd, int ndAdd) {
			long now = System.currentTimeMillis();
			if(restart == true) {
				tInit = now;
				restart = false;
			}
			nl = nl + nlAdd;
			nr = nr + nrAdd;
			nu = nu + nuAdd;
			nd = nd + ndAdd;
			tFin = now;
			//System.out.println("(" + nl + ", " +  nr + ", " +  nu + ", " + nd + ")");
		}
		
		public synchronized void move() {
			Location2DUTM prev = CatchMeParams.startingLocation.get();
			Location2DUTM current = new Location2DUTM(prev.x, prev.y);
			
			
			if(tFin - tInit == 0) {
				if(nl < 0 || nr > 0 || nu > 0 || nd < 0 ) {
					if(nr + nl > 0) {
						current.setLocation(current.x + 5, current.y);
					} else if(nr + nl < 0){
						current.setLocation(current.x - 5, current.y);
					}
					if(nu + nd > 0) {
						current.setLocation(current.x, current.y + 5);
					} else if(nu + nd < 0){
						current.setLocation(current.x, current.y - 5);
					}
				}
			} else { 
				double pulsV = (((nu + nd)*1.0)/(tFin - tInit));
				double pulsH = (((nr + nl)*1.0)/(tFin - tInit));
								
				if((Math.abs(pulsV + pulsH) * 100) < 1.5) {
					if(pulsH > 0)
						current.setLocation(current.x + 5, current.y);
					else if(pulsH < 0) {
						current.setLocation(current.x - 5, current.y);
					}
					if(pulsV > 0)
						current.setLocation(current.x, current.y + 5);
					else if(pulsV < 0) {
						current.setLocation(current.x, current.y - 5);
					}
				}
				else {
					current.setLocation(current.x + 5 *(pulsH*100), current.y + 5 * (pulsV*100));
				}
				System.out.println(tFin - tInit);
			}
			CatchMeParams.startingLocation.set(current);
			
			
			restart = true;
			nl = 0;
			nr = 0;
			nu = 0;
			nd = 0;
			tFin = 0;
			tInit = 0;
		}
}

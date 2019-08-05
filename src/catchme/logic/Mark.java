package catchme.logic;

import api.API;
import api.pojo.location.Location2D;
import api.pojo.location.Location2DUTM;

public class Mark {

		private static long tInit;
		private static long tFin;
		private static int nl, nr, nu, nd;
		private static boolean restart;
		
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
			nr = nr + nlAdd;
			nu = nu + nlAdd;
			nd = nd + nlAdd;
			tFin = now;
		}
		
		public synchronized void move() {
			Location2DUTM current = CatchMeParams.startingLocation.get();
			
			
			
			if(tFin - tInit == 0) {
				if(nl > 0 || nr > 0 || nu > 0 || nd > 0 ) {
					if(nr - nl > 0) {
						current.setLocation(current.x + 5, current.y);
					} else if(nr - nl < 0){
						current.setLocation(current.x - 5, current.y);
					}
					if(nu - nd > 0) {
						current.setLocation(current.x, current.y + 5);
					} else if(nu - nd < 0){
						current.setLocation(current.x, current.y - 5);
					}
				}
			} else { 
				double pulsV = (((nu - nd)*1.0)/(tFin - tInit));
				double pulsH = (((nr - nl)*1.0)/(tFin - tInit));
				
				if(pulsV + pulsH < 5) {
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
					current.setLocation(current.x + pulsH, current.y + pulsV);
				}
				
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

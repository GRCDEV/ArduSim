package followme.logic;

public class Nodo {
	public int type;
	public long time;
	public double easting,northing,z,zRel,speed,heading;
	public int chan1,chan2,chan3,chan4;
	public String msg;
	public Nodo next;
	
	public Nodo (int tipo, long tiempo, String m) {
		this.type = tipo;
		this.time = tiempo;
		this.msg = m;
		this.next = null;
	}
	public Nodo (int tipo, long tiempo, String m, Nodo n) {
		this.type = tipo;
		this.time = tiempo;
		this.msg = m;
		this.next = n;
	}
	public Nodo (int tipo, long tiempo, double e, double n, double alt, double altRel,double vel, double h) {
		this.type = tipo;
		this.time = tiempo;
		this.easting = e;
		this.northing = n;
		this.z = alt;
		this.zRel = altRel;
		this.speed = vel;
		this.heading = h;
	}
	public Nodo (int tipo, long tiempo, int ch1, int ch2, int ch3, int ch4) {
		this.type = tipo;
		this.time = tiempo;
		this.chan1 = ch1;
		this.chan2 = ch2;
		this.chan3 = ch3;
		this.chan4 = ch4;
	}
	public void print() {
		// TODO Auto-generated method stub
		if(type == 0) {
			System.out.println("Nodo - type: "+type+" time: "+time+" easting: "+easting+" northing: "+northing+" z: "+z
					+" zRel: "+zRel+" speed: "+speed+" heading: "+heading+" next: "+(next!=null));
		}else if (type == 1){
			System.out.println("Nodo - type: "+type+" time: "+time+" chan1: "+chan1+" chan2: "+chan2+" chan3: "+chan3+" chan4: "+chan4+" heading: "+heading+" next: "+(next!=null));
		}
		
	}
	public long getTime() {
		return time;
	}
	
}

package fme.pojo;

import api.pojo.RCValues;

/** Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain). */

public class RemoteInput extends RCValues implements Comparable<RemoteInput> {
	
	public long time;
	
	public RemoteInput(long time, int roll, int pitch, int throttle, int yaw) {
		super(roll, pitch, throttle, yaw);
		this.time = time;
	}
	
	

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof RemoteInput)) {
			return false;
		}
		return this.time == ((RemoteInput)obj).time;
	}

	@Override
	public int compareTo(RemoteInput o) {
		long res = this.time - o.time;
		if (res == 0) {
			return 0;
		} else if (res < 0) {
			return -1;
		} else {
			return 1;
		}
	}



	@Override
	public String toString() {
		return this.time + ": " + this.roll + ", " + this.pitch + ", " + this.throttle + ", " + yaw;
	}
	
	
}

package main.api;

import java.util.Objects;

import api.pojo.AtomicFloat;

/** 
 * Parameters loaded from the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CopterParamLoaded implements Comparable<CopterParamLoaded> {
	
	private String name;
	private AtomicFloat value;
	private int type;
	
	@SuppressWarnings("unused")
	private CopterParamLoaded() {}
	
	/** Creates a new ArduCopter parameter given its name, value, and type of parameter (see MAV_PARAM_TYPE enumerator). */
	public CopterParamLoaded(String name, float value, int type) {
		this.name = name;
		this.value = new AtomicFloat(value);
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public float getValue() {
		return value.get();
	}
	
	public void setValue(float value) {
		this.value.set(value);
	}

	public int getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		// The name is enough to assert if two parameters are equals
		if (obj == this) {
			return true;
		}
		
		if (obj == null || !(obj instanceof CopterParamLoaded)) {
			return false;
		}
		return this.name.equals(((CopterParamLoaded) obj).name);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

	@Override
	public int compareTo(CopterParamLoaded o) {
		return this.name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return this.name + "=" + this.value + "(type: " + this.type + ")";
	}
}

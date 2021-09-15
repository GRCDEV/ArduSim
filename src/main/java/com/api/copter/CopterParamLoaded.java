package com.api.copter;

import io.dronefleet.mavlink.common.MavParamType;
import io.dronefleet.mavlink.util.EnumValue;

import java.util.Objects;

/** 
 * Parameters loaded from the flight controller.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CopterParamLoaded implements Comparable<CopterParamLoaded> {
	
	private String name;
	private float value;
	private EnumValue<MavParamType> type;
	
	@SuppressWarnings("unused")
	private CopterParamLoaded() {}
	
	/** Creates a new ArduCopter parameter given its name, value, and type of parameter (see MAV_PARAM_TYPE enumerator). */
	public CopterParamLoaded(String name, float value, EnumValue<MavParamType> type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public float getValue() {
		return value;
	}
	
	public void setValue(float value) {
		this.value = value;
	}

	public EnumValue<MavParamType> getType() {
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

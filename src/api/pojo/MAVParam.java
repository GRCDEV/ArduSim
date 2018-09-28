package api.pojo;

import java.util.Objects;

public class MAVParam {
	
	private String name;
	private AtomicFloat value;
	private int type;
	
	@SuppressWarnings("unused")
	private MAVParam() {}
	
	/** Creates a new ArduCopter parameter given its name, value, and type of parameter (see MAV_PARAM_TYPE enumerator). */
	public MAVParam(String name, float value, int type) {
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
	public int hashCode() {
		return Objects.hash(this.name);
	}

	@Override
	public boolean equals(Object obj) {
		// The name is enough to assert if two parameters are equals
		if (!(obj instanceof MAVParam)) {
			return false;
		}
		return this.name.equals(((MAVParam) obj).name);
	}

	@Override
	public String toString() {
		return this.name + "=" + this.value + "(type: " + this.type + ")";
	}
	
}

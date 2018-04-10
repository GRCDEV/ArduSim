package uavController;

public class RCValues {
	public int rc1, rc2, rc3, rc4;
	
	@SuppressWarnings("unused")
	private RCValues() {}
	
	public RCValues(int rc1, int rc2, int rc3, int rc4) {
		this.rc1 = rc1;
		this.rc2 = rc2;
		this.rc3 = rc3;
		this.rc4 = rc4;
	}
}

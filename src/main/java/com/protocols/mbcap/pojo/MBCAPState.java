package com.protocols.mbcap.pojo;

import com.protocols.mbcap.logic.MBCAPText;

/** MBCAP finite state machine states enumerator.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public enum MBCAPState {
	NORMAL((short)0, MBCAPText.STATE_NORMAL),
	STAND_STILL((short)1, MBCAPText.STATE_STAND_STILL),
	MOVING_ASIDE((short)2, MBCAPText.STATE_MOVING_ASIDE),
	GO_ON_PLEASE((short)3, MBCAPText.STATE_GO_ON_PLEASE),
	OVERTAKING((short)4, MBCAPText.STATE_OVERTAKING),
	EMERGENCY_LAND((short)5, MBCAPText.STATE_EMERGENCY_LAND);
	
	private final short id;
	private final String name;
	MBCAPState(short id, String name) {
		this.id = id;
		this.name = name;
	}
	public short getId() {
		return this.id;
	}
	public String getName() {
		return this.name;
	}
	public static MBCAPState getSatateById(short id) {
		for (MBCAPState p : MBCAPState.values()) {
			if (p.getId() == id) {
				return p;
			}
		}
		return null;
	}
}

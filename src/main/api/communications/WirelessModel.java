package main.api.communications;

import main.Text;

/**
 * Enumeration with the available wireless models used for simulation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public enum WirelessModel {

	NONE(0, Text.WIRELESS_MODEL_NONE),					// Unrestricted model
	FIXED_RANGE(1, Text.WIRELESS_MODEL_FIXED_RANGE),	// Fixed distance model
	DISTANCE_5GHZ(2, Text.WIRELESS_MODEL_5GHZ);			// Real distance model based on WiFi 5.18 GHz (channel 36) 
	// New models should follow the increasing numeration. The method Tools.isInRange() must also be modified
	
	private final int id;
	private final String name;
	WirelessModel(int id, String name) {
		this.id = id;
		this.name = name;
	}
	public int getId() {
		return this.id;
	}
	public String getName() {
		return this.name;
	}
	public static WirelessModel getHighestIdModel() {
		WirelessModel res = null;
		for (WirelessModel p : WirelessModel.values()) {
			if (res == null) {
				res = p;
			} else {
				if (p.getId() > res.getId()) {
					res = p;
				}
			}
		}
		return res;
	}
	public static String getModelNameById(int id) {
		for (WirelessModel p : WirelessModel.values()) {
			if (p.getId() == id) {
				return p.getName();
			}
		}
		return "";
	}
	public static WirelessModel getModelByName(String name) {
		for (WirelessModel p : WirelessModel.values()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	public static String[] getAllModels(){
		return new String[]{NONE.name, FIXED_RANGE.name,DISTANCE_5GHZ.name};
	}

	@Override
	public String toString() {
		return name;
	}
}

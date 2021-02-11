package com.protocols.mbcap.pojo;

import es.upv.grc.mapper.Location2DUTM;

/** This class generates objects with UAV location and the instant when the data was retrieved.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ErrorPoint extends Location2DUTM {

	private static final long serialVersionUID = 1L;
	public double time;
	
	public ErrorPoint(double time, double x, double y) {
		super(x, y);
		this.time = time;
	}

}

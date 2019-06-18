package main.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

import main.uavController.UAVParam;

/**
 * API used to validate parameters introduced by the user in the GUI, and to format strings and numbers.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ValidationTools {
	
	/**
	 * Check if a String is null or empty.
	 * @param validating String to validate.
	 * @return true if the String is null or empty.
	 */
	public boolean isEmpty(String validating) {
		if (validating == null || validating.length() == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Validate a boolean String.
	 * @param validating String representation of the boolean value.
	 * @return true if the String represents a valid boolean.
	 */
	public boolean isValidBoolean(String validating) {
		if (validating == null) {
			return false;
		}
		if (!validating.equalsIgnoreCase("true") && !validating.equalsIgnoreCase("false")) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a double number.
	 * @param validating String representation of a double.
	 * @return true if the String represents a valid double.
	 */
	public boolean isValidDouble(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			Double.parseDouble(validating);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a non negative integer number.
	 * @param validating String representation of a non negative integer.
	 * @return true if the String represents a valid non negative integer.
	 */
	public boolean isValidNonNegativeInteger(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			int x = Integer.parseInt(validating);
			if (x < 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a TCP port.
	 * @param validating String representation of the TCP port.
	 * @return true if the String represents a valid TCP port.
	 */
	public boolean isValidPort(String validating) {
		if (validating == null) {
			return false;
		}
	
		try {
			int x = Integer.parseInt(validating);
			if (x < 1024 || x > UAVParam.MAX_PORT) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Validate a positive integer number.
	 * @param validating String representation of a positive integer.
	 * @return true if the String represents a valid positive integer.
	 */
	public boolean isValidPositiveInteger(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			int x = Integer.parseInt(validating);
			if (x <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a positive long number.
	 * @param validating String representation of a positive long.
	 * @return true if the String represents a valid positive long.
	 */
	public boolean isValidPositiveLong(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			long x = Long.parseLong(validating);
			if (x <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Validate a positive double number.
	 * @param validating String representation of a positive double.
	 * @return true if the String represents a valid positive double.
	 */
	public boolean isValidPositiveDouble(String validating) {
		if (validating == null) {
			return false;
		}
		try {
			double x = Double.parseDouble(validating);
			if (x <= 0) {
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Round a double number to "places" decimal digits.
	 * @param value Value to be rounded.
	 * @param places Target decimal places.
	 * @return Rounded value.
	 */
	public double roundDouble(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	/**
	 * Format a time range to h:mm.ss.
	 * @param start Initial value retrieved with the method <i>System.curentTimeMillis()</i>.
	 * @param end Final value retrieved with the method <i>System.curentTimeMillis()</i>.
	 * @return String representation in h:mm:ss format.
	 */
	public String timeToString(long start, long end) {
		long time = Math.abs(end - start);
		long h = time/3600000;
		time = time - h*3600000;
		long m = time/60000;
		time = time - m*60000;
		long s = time/1000;
		return h + ":" + String.format("%02d", m) + ":" + String.format("%02d", s);
	}
	
}

package main.api;

/**
 * Exception thrown when the user tries to use functionality not available while starting the application.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ArduSimNotReadyException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ArduSimNotReadyException(String message) {
		super(message);
	}

}

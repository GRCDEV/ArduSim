package com.api.communications.lowLevel;

/**
 * Communications link object.
 * @author Francisco Jos&eacute; Fabra collade, Jamie Wubben
 */

interface InterfaceCommLinkObject {
	void sendBroadcastMessage(int numUAV, byte[] message);
	byte[] receiveMessage(int numUAV, int socketTimeout);
	String toString();
}

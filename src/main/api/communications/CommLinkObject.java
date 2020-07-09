package main.api.communications;

import api.API;
import main.ArduSimTools;
import main.Param;
import main.Text;
import main.api.ArduSim;
import main.api.ValidationTools;
import main.sim.logic.SimParam;
import main.uavController.UAVParam;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Communications link object.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CommLinkObject {
	
	private ArduSim ardusim;
	
	// Real UDP communication parameters:
	private int port;
	private DatagramSocket sendSocket;		// Sending socket
	private DatagramPacket sendPacket;		// Sending packet
	private DatagramSocket receiveSocket;	// Receiving socket
	private DatagramPacket receivePacket;	// Receiving packet
	
	// Virtual communication parameters:
	public static final long RANGE_CHECK_PERIOD = 1000;				// (ms) Time between UAVs range check
	public static AtomicBoolean[][] isInRange;						// Matrix containing the range check result
	private AtomicReferenceArray<IncomingMessage> prevSentMessage;	// Stores the last sent message for each UAV in the current channel (CC)
	public static boolean pCollisionEnabled = true;					// Whether the packet collision detection is enabled or not
	private ReentrantLock[] lock;									// Locks for concurrence when detecting  packet collisions in the CC
	public static boolean carrierSensingEnabled = true;				// Whether the carrier sensing is enabled or not
	private IncomingMessageQueue[] mBuffer;							// Array with the message queues used as buffers in the CC
	public static final int RECEIVING_BUFFER_PACKET_SIZE = 350;		// (packets) Initial size of the incoming queue q
	public static int receivingBufferSize = 163840;					// (bytes) By default, the Raspberry Pi 3 receiving buffer size
	private AtomicLongArray maxCompletedTEndTime;					// (ns) Array with the later completed transfer finish time for each sending UAV when using collision detection in the CC
	private static final long MESSAGE_WAITING_TIME = 1;				// (ms) Waiting time to check if a message arrives
	private ConcurrentSkipListSet<IncomingMessage>[] vBuffer;		// Array with virtual buffer to calculate packet collisions when using packet collision detection in the CC
	public static final int V_BUFFER_SIZE_FACTOR = 3;				// vBuffer is this times the size of mBuffer
	public static int receivingvBufferSize = V_BUFFER_SIZE_FACTOR * CommLinkObject.receivingBufferSize;	// (bytes) Virtual buffer size
	public static final double BUFFER_FULL_THRESHOLD = 0.8;			// Size of the buffer when it is flushed to avoid it to be filled
	public static int receivingvBufferTrigger = (int)Math.rint(BUFFER_FULL_THRESHOLD * receivingvBufferSize);// (bytes) Virtual buffer level when it is flushed to avoid it to be filled
	private AtomicIntegerArray vBufferUsedSpace;					// (bytes) Array containing the current level of the vBuffer in the CC
	public static ConcurrentHashMap<String, String> communicationsClosed; // Counts the CC communication threads that stop sending or receiving messages when the experiment finishes
	public static final int CLOSSING_WAITING_TIME = 5000;			// (ms) Time to wait while the communications are being closed
	
	// Statistics
	//  Simulation
	private int[] packetWaitedPrevSending, packetWaitedMediaAvailable;
	private AtomicIntegerArray receiverOutOfRange, receiverWasSending, receiverVirtualQueueFull, receiverQueueFull, successfullyReceived;
	private AtomicIntegerArray discardedForCollision, successfullyEnqueued, successfullyProcessed;
	//  Simulation and real communications
	private int[] sentPacket;
	private AtomicIntegerArray receivedPacket;
	
	@SuppressWarnings("unused")
	private CommLinkObject() {}
	
	@SuppressWarnings("unchecked")
	public CommLinkObject(int numUAVs, int port) {
		
		this.ardusim = API.getArduSim();
		
		this.port = port;
		
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			prevSentMessage = new AtomicReferenceArray<>(numUAVs);
			mBuffer = new IncomingMessageQueue[numUAVs];
			for (int i = 0; i < Param.numUAVs; i++) {
				mBuffer[i] = new IncomingMessageQueue();
			}
			
			if (pCollisionEnabled) {
				vBuffer = new ConcurrentSkipListSet[Param.numUAVs];
				for (int i = 0; i < Param.numUAVs; i++) {
					vBuffer[i] = new ConcurrentSkipListSet<>();
				}
				vBufferUsedSpace = new AtomicIntegerArray(numUAVs);
				successfullyProcessed = new AtomicIntegerArray(numUAVs);
				maxCompletedTEndTime = new AtomicLongArray(numUAVs);
				lock = new ReentrantLock[numUAVs];
				for (int i = 0; i < numUAVs; i++) {
					lock[i] = new ReentrantLock();
				}
			}
			
			packetWaitedPrevSending = new int[numUAVs];
			packetWaitedMediaAvailable = new int[numUAVs];
			receiverOutOfRange = new AtomicIntegerArray(numUAVs);
			receiverWasSending = new AtomicIntegerArray(numUAVs);
			receiverVirtualQueueFull = new AtomicIntegerArray(numUAVs);
			receiverQueueFull = new AtomicIntegerArray(numUAVs);
			successfullyReceived = new AtomicIntegerArray(numUAVs);
			discardedForCollision = new AtomicIntegerArray(numUAVs);
			successfullyEnqueued = new AtomicIntegerArray(numUAVs);
			if (this.port == UAVParam.broadcastPort) {
				CommLinkObject.communicationsClosed = new ConcurrentHashMap<>(numUAVs);
			}
			
		} else {
			
			try {
				sendSocket = new DatagramSocket();
				sendSocket.setBroadcast(true);
				sendPacket = new DatagramPacket(new byte[CommLink.DATAGRAM_MAX_LENGTH],
						CommLink.DATAGRAM_MAX_LENGTH,
						InetAddress.getByName(UAVParam.broadcastIP),
						port);
				receiveSocket = new DatagramSocket(port);
				receiveSocket.setBroadcast(true);
				receivePacket = new DatagramPacket(new byte[CommLink.DATAGRAM_MAX_LENGTH], CommLink.DATAGRAM_MAX_LENGTH);
			} catch (SocketException | UnknownHostException e) {
				ArduSimTools.closeAll(Text.THREAD_START_ERROR);
			}
			
		}
		
		sentPacket = new int[numUAVs];
		receivedPacket = new AtomicIntegerArray(numUAVs);
		
	}

	public void sendBroadcastMessage(int numUAV, byte[] message) {
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			if (Param.simStatus == Param.SimulatorState.TEST_FINISHED) {
				if (this.port == UAVParam.broadcastPort) {
					CommLinkObject.communicationsClosed.put("s" + numUAV, "s" + numUAV);
				}
				while (true) {
					ardusim.sleep(SimParam.LONG_WAITING_TIME);
				}
			}
			long now = System.nanoTime();
			// 1. Can not send until the last transmission has finished
			IncomingMessage prevMessage = prevSentMessage.get(numUAV);
			if (prevMessage != null) {
				boolean messageWaited = false;
				while (prevMessage.end > now) {
					ardusim.sleep(MESSAGE_WAITING_TIME);
					if (!messageWaited) {
						packetWaitedPrevSending[numUAV]++;
						messageWaited = true;
					}
					now = System.nanoTime();
				}
			}
			
			// 2. Wait if packet transmissions are detected in the UAV range (carrier sensing)
			boolean mediaIsAvailable = false;
			boolean messageWaited = false;
			if (CommLinkObject.carrierSensingEnabled) {
				while (!mediaIsAvailable) {
					boolean wait = false;
					now = System.nanoTime();
					IncomingMessage prevMessageOther;
					for (int i = 0; i < Param.numUAVs && !wait; i++) {
						prevMessageOther = prevSentMessage.get(i);
						if (i != numUAV && CommLinkObject.isInRange[numUAV][i].get() && prevMessageOther != null && prevMessageOther.end > now) {
							wait = true;
							if (!messageWaited) {
								packetWaitedMediaAvailable[numUAV]++;
								messageWaited = true;
							}
						}
					}
					if (wait) {
						ardusim.sleep(MESSAGE_WAITING_TIME);
					} else {
						mediaIsAvailable = true;
					}
				}
			}
			now = System.nanoTime();
			
			// 3. Send the packet to all UAVs but the sender
			IncomingMessage prevMessageOther;
			IncomingMessage sendingMessage = new IncomingMessage(numUAV, now, message);
			sentPacket[numUAV]++;
			prevSentMessage.set(numUAV, sendingMessage);
			for (int i = 0; i < Param.numUAVs; i++) {
				if (i != numUAV) {
					if(CommLinkObject.isInRange[numUAV][i].get()) {
						prevMessageOther = prevSentMessage.get(i);
						// The message can not be received if the destination UAV is already sending
						if (prevMessageOther == null || prevMessageOther.end <= now) {
							if (CommLinkObject.pCollisionEnabled) {
								if (vBufferUsedSpace.get(i) + message.length <= receivingvBufferSize) {
									vBuffer[i].add(new IncomingMessage(sendingMessage));
									vBufferUsedSpace.addAndGet(i, message.length);
									successfullyEnqueued.incrementAndGet(i);
								} else {
									receiverVirtualQueueFull.incrementAndGet(i);
								}
								if (vBufferUsedSpace.get(i) >= receivingvBufferTrigger) {
									this.checkPacketCollisions(i, false);
								}
							} else {
								// Add to destination queue if it fits
								//	As collision detection is disabled and mBuffer is read by one thread and wrote by many,
								//	  the inserted elements are no longer sorted
								if (mBuffer[i].offerLast(new IncomingMessage(sendingMessage))) {
									successfullyReceived.incrementAndGet(i);
								} else {
									receiverQueueFull.incrementAndGet(i);
								}
							}
						} else {
							receiverWasSending.incrementAndGet(i);
						}
					} else {
						receiverOutOfRange.incrementAndGet(i);
					}
				}
			}
		} else {
			sendPacket.setData(message);
			try {
				sendSocket.send(sendPacket);
				sentPacket[0]++;
			} catch (IOException e) {
				API.getGUI(numUAV).logUAV(Text.MESSAGE_ERROR);
			}
		}
	}
	
	public byte[] receiveMessage(int numUAV, int socketTimeout) {
		if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			if (Param.simStatus == Param.SimulatorState.TEST_FINISHED) {
				if (this.port == UAVParam.broadcastPort) {
					CommLinkObject.communicationsClosed.put("r" + numUAV, "r" + numUAV);
				}
				while (true) {
					ardusim.sleep(SimParam.LONG_WAITING_TIME);
				}
			}
			long start = System.currentTimeMillis();
			long elapsedTime = 0;
			byte[] receivedBuffer = null;
			if (CommLinkObject.pCollisionEnabled) {
				while ((receivedBuffer == null && socketTimeout <=0) || (receivedBuffer == null && elapsedTime < socketTimeout)) {
					if (mBuffer[numUAV].isEmpty()) {
						// Check for collisions
						// 1. Wait until at least one message is available
						if (vBuffer[numUAV].isEmpty()) {
							ardusim.sleep(MESSAGE_WAITING_TIME);
							elapsedTime = System.currentTimeMillis() - start;
						} else {
							this.checkPacketCollisions(numUAV, true);
						}
					} else {
						// Wait for transmission end
						IncomingMessage message = mBuffer[numUAV].peekFirst();
						long now = System.nanoTime();
						while (message.end > now) {
							ardusim.sleep(MESSAGE_WAITING_TIME);
						}
						receivedPacket.incrementAndGet(numUAV);
						receivedBuffer = mBuffer[numUAV].pollFirst().message;
					}
				}
			} else {
				boolean isEmpty = mBuffer[numUAV].isEmpty();
				while ((isEmpty && socketTimeout <= 0) || (isEmpty && elapsedTime < socketTimeout)) {
					ardusim.sleep(MESSAGE_WAITING_TIME);
					elapsedTime = System.currentTimeMillis() - start;
					isEmpty = mBuffer[numUAV].isEmpty();
				}
				if (!mBuffer[numUAV].isEmpty()) {
					receivedPacket.incrementAndGet(numUAV);
					receivedBuffer = mBuffer[numUAV].pollFirst().message;
				}
			}
			return receivedBuffer;
		} else {
			receivePacket.setData(new byte[CommLink.DATAGRAM_MAX_LENGTH], 0, CommLink.DATAGRAM_MAX_LENGTH);
			try {
				if (socketTimeout > 0) {
					receiveSocket.setSoTimeout(socketTimeout);
				}
				receiveSocket.receive(receivePacket);
				receivedPacket.incrementAndGet(0);
				return receivePacket.getData();
			} catch (IOException e) { return null; }
		}
	}
	
	private void checkPacketCollisions(int numUAV, boolean isReceiver) {
		lock[numUAV].lock();
		try {
			// Maybe another thread would also try to check at the same time
			if (isReceiver || vBufferUsedSpace.get(numUAV) >= receivingvBufferTrigger) {
				// 1. First iteration through the virtual buffer to check collisions between messages
				Iterator<IncomingMessage> it = vBuffer[numUAV].iterator();
				IncomingMessage prev, pos, next;
				prev = it.next();
				prev.checked.set(true);
				// 1.1. Waiting until the first message is transmitted
				//		Meanwhile, another message could be set as the first one, but it will be detected on the second iteration
				//		and all the process will be repeated again
				long now = System.nanoTime();
				while (prev.end > now) {
					ardusim.sleep(MESSAGE_WAITING_TIME);
					now = System.nanoTime();
				}
				// 1.2. Update the late completely received message finishing time
				if (prev.end > maxCompletedTEndTime.get(numUAV)) {
					maxCompletedTEndTime.set(numUAV, prev.end);
				}
				// 1.3. Iteration over the rest of the elements to check collisions
				while (it.hasNext()) {
					pos = it.next();
					pos.checked.set(true);
					// Collides with the previous message?
					if (pos.start <= maxCompletedTEndTime.get(numUAV)) {
						prev.overlapped.set(true);
						pos.overlapped.set(true);
					}
					// This message is being transmitted?
					if (pos.end > now) {
						// As the message has not been fully transmitted, we stop the iteration, ignoring this message on the second iteration
						pos.checked.set(false);
						if (pos.overlapped.get()) {
							prev.checked.set(false);
						}
						break;
					} else {
						// Update the late completely received message finishing time
						if (pos.end > maxCompletedTEndTime.get(numUAV)) {
							maxCompletedTEndTime.set(numUAV, pos.end);
						}
						prev = pos;
					}
				}
				// 2. Second iteration discarding collided messages and storing received messages on the FIFO buffer
				it = vBuffer[numUAV].iterator();
				while (it.hasNext()) {
					next = it.next();
					if (next.checked.get()) {
						//it.remove();	// Problematic, as you don't know if the item has been really removed
						if (vBuffer[numUAV].remove(next)) {
							vBufferUsedSpace.addAndGet(numUAV, -next.message.length);
							successfullyProcessed.incrementAndGet(numUAV);
							if (next.overlapped.get()) {
								if (!next.alreadyOverlapped.get()) {
									discardedForCollision.incrementAndGet(numUAV);
									next.alreadyOverlapped.set(true);
								}
							} else {
								successfullyReceived.incrementAndGet(numUAV);
								if (!mBuffer[numUAV].offerLast(next)) {
									receiverQueueFull.incrementAndGet(numUAV);
								}
							}
						}
					} else {
						break;
					}
				}
			}
		} finally {
			lock[numUAV].unlock();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(2000);
		long sentPacketTot = 0;
		long receivedPacketTot = 0;
		for (int i = 0; i < Param.numUAVs; i++) {
			sentPacketTot = sentPacketTot + sentPacket[i];
			receivedPacketTot = receivedPacketTot + receivedPacket.get(i);
		}
		if (Param.role == ArduSim.MULTICOPTER) {
			sb.append("\n\t").append(Text.BROADCAST_IP).append(" ").append(UAVParam.broadcastIP);
			sb.append("\n\t").append(Text.BROADCAST_PORT).append(" ").append(port);
			sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(sentPacketTot);
			sb.append("\n\t").append(Text.TOT_PROCESSED).append(" ").append(receivedPacketTot);
		} else if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
			sb.append("\n\t").append(Text.CARRIER_SENSING_ENABLED).append(" ").append(CommLinkObject.carrierSensingEnabled);
			sb.append("\n\t").append(Text.PACKET_COLLISION_DETECTION_ENABLED).append(" ").append(CommLinkObject.pCollisionEnabled);
			sb.append("\n\t").append(Text.BUFFER_SIZE).append(" ").append(CommLinkObject.receivingBufferSize).append(" ").append(Text.BYTES);
			sb.append("\n\t").append(Text.WIFI_MODEL).append(" ").append(Param.selectedWirelessModel.getName());
			if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
				sb.append(": ").append(Param.fixedRange).append(" ").append(Text.METERS);
			}
			sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(sentPacketTot);
			ValidationTools validationTools = API.getValidationTools();
			if (Param.numUAVs > 1 && sentPacketTot > 0) {
				long packetWaitedPrevSendingTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					packetWaitedPrevSendingTot = packetWaitedPrevSendingTot + packetWaitedPrevSending[i];
				}
				sb.append("\n\t\t").append(Text.TOT_WAITED_PREV_SENDING).append(" ").append(packetWaitedPrevSendingTot)
					.append(" (").append(validationTools.roundDouble((100.0 * packetWaitedPrevSendingTot)/sentPacketTot, 3)).append("%)");
				if (CommLinkObject.carrierSensingEnabled) {
					long packetWaitedMediaAvailableTot = 0;
					for (int i = 0; i < Param.numUAVs; i++) {
						packetWaitedMediaAvailableTot = packetWaitedMediaAvailableTot + packetWaitedMediaAvailable[i];
					}
					sb.append("\n\t\t").append(Text.TOT_WAITED_MEDIA_AVAILABLE).append(" ").append(packetWaitedMediaAvailableTot)
						.append(" (").append(validationTools.roundDouble((100.0 * packetWaitedMediaAvailableTot)/sentPacketTot, 3)).append("%)");
				}
				long potentiallyReceived = sentPacketTot * (Param.numUAVs - 1);
				long receiverOutOfRangeTot = 0;
				long receiverWasSendingTot = 0;
				long successfullyReceivedTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					receiverOutOfRangeTot = receiverOutOfRangeTot + receiverOutOfRange.get(i);
					receiverWasSendingTot = receiverWasSendingTot + receiverWasSending.get(i);
					successfullyReceivedTot = successfullyReceivedTot + successfullyReceived.get(i);
				}
				long receiverVirtualQueueFullTot = 0;
				long successfullyEnqueuedTot = 0;
				if (CommLinkObject.pCollisionEnabled) {
					for (int i = 0; i < Param.numUAVs; i++) {
						receiverVirtualQueueFullTot = receiverVirtualQueueFullTot + receiverVirtualQueueFull.get(i);
						successfullyEnqueuedTot = successfullyEnqueuedTot + successfullyEnqueued.get(i);
					}
				}
				long receiverQueueFullTot = 0;
				for (int i = 0; i < Param.numUAVs; i++) {
					receiverQueueFullTot = receiverQueueFullTot + receiverQueueFull.get(i);
				}
				sb.append("\n\t").append(Text.TOT_POTENTIALLY_RECEIVED).append(" ").append(potentiallyReceived);
				sb.append("\n\t\t").append(Text.TOT_OUT_OF_RANGE).append(" ").append(receiverOutOfRangeTot)
					.append(" (").append(validationTools.roundDouble((100.0 * receiverOutOfRangeTot)/potentiallyReceived, 3)).append("%)");
				sb.append("\n\t\t").append(Text.TOT_LOST_RECEIVER_WAS_SENDING).append(" ").append(receiverWasSendingTot)
					.append(" (").append(validationTools.roundDouble((100.0 * receiverWasSendingTot)/potentiallyReceived, 3)).append("%)");
				if (CommLinkObject.pCollisionEnabled) {
					sb.append("\n\t\t").append(Text.TOT_VIRTUAL_QUEUE_WAS_FULL).append(" ").append(receiverVirtualQueueFullTot)
						.append(" (").append(validationTools.roundDouble((100.0 * receiverVirtualQueueFullTot)/potentiallyReceived, 3)).append("%)");
					sb.append("\n\t\t").append(Text.TOT_RECEIVED_IN_VBUFFER).append(" ").append(successfullyEnqueuedTot)
						.append(" (").append(validationTools.roundDouble((100.0 * successfullyEnqueuedTot)/potentiallyReceived, 3)).append("%)");
				} else {
					// We must include the received messages but discarded because the buffer was full
					successfullyReceivedTot = successfullyReceivedTot + receiverQueueFullTot;
					sb.append("\n\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
						.append(" (").append(validationTools.roundDouble((100.0 * successfullyReceivedTot)/potentiallyReceived, 3)).append("%)");
				}
				long inBufferTot = successfullyReceivedTot - receiverQueueFullTot - receivedPacketTot;
				if (CommLinkObject.pCollisionEnabled) {
					if (successfullyEnqueuedTot != 0) {
						long successfullyProcessedTot = 0;
						for (int i = 0; i < Param.numUAVs; i++) {
							successfullyProcessedTot = successfullyProcessedTot + successfullyProcessed.get(i);
						}
						long inVBufferTot = successfullyEnqueuedTot - successfullyProcessedTot;
						sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_VBUFFER).append(" ").append(inVBufferTot)
							.append(" (").append(validationTools.roundDouble((100.0 * inVBufferTot)/successfullyEnqueuedTot, 3)).append("%)");
						sb.append("\n\t\t\t").append(Text.TOT_PROCESSED).append(" ").append(successfullyProcessedTot)
							.append(" (").append(validationTools.roundDouble((100.0 * successfullyProcessedTot)/successfullyEnqueuedTot, 3)).append("%)");
						if (successfullyProcessedTot != 0) {
							long discardedForCollisionTot = 0;
							for (int i = 0; i < Param.numUAVs; i++) {
								discardedForCollisionTot = discardedForCollisionTot + discardedForCollision.get(i);
							}
							sb.append("\n\t\t\t\t").append(Text.TOT_DISCARDED_FOR_COLLISION).append(" ").append(discardedForCollisionTot)
								.append(" (").append(validationTools.roundDouble((100.0 * discardedForCollisionTot)/successfullyProcessedTot, 3)).append("%)");
							sb.append("\n\t\t\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
								.append(" (").append(validationTools.roundDouble((100.0 * successfullyReceivedTot)/successfullyProcessedTot, 3)).append("%)");
							if (successfullyReceivedTot != 0) {
								sb.append("\n\t\t\t\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
									.append(" (").append(validationTools.roundDouble((100.0 * receiverQueueFullTot)/successfullyReceivedTot, 3)).append("%)");
								sb.append("\n\t\t\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(inBufferTot)
									.append(" (").append(validationTools.roundDouble((100.0 * inBufferTot)/successfullyReceivedTot, 3)).append("%)");
								sb.append("\n\t\t\t\t\t").append(Text.TOT_USED_OK).append(" ").append(receivedPacketTot)
									.append(" (").append(validationTools.roundDouble((100.0 * receivedPacketTot)/successfullyReceivedTot, 3)).append("%)");
							}
						}
					}
				} else {
					if (successfullyReceivedTot != 0) {
						sb.append("\n\t\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
						.append(" (").append(validationTools.roundDouble((100.0 * receiverQueueFullTot)/successfullyReceivedTot, 3)).append("%)");
					sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(inBufferTot)
						.append(" (").append(validationTools.roundDouble((100.0 * inBufferTot)/successfullyReceivedTot, 3)).append("%)");
					sb.append("\n\t\t\t").append(Text.TOT_USED_OK).append(" ").append(receivedPacketTot)
						.append(" (").append(validationTools.roundDouble((100.0 * receivedPacketTot)/successfullyReceivedTot, 3)).append("%)");
					}
				}
			}
		}
		return sb.toString();
	}
	
	
}

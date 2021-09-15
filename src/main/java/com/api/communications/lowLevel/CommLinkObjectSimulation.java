package com.api.communications.lowLevel;

import com.api.API;
import com.api.ArduSim;
import com.api.ValidationTools;
import com.api.communications.WirelessModel;
import com.setup.Param;
import com.setup.Text;
import com.setup.sim.logic.SimParam;
import com.uavController.UAVParam;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class CommLinkObjectSimulation implements InterfaceCommLinkObject {

    /**
     * Boolean to enable carrier sensing
     */
    public static boolean carrierSensingEnabled = true;
    /**
     * Boolean to enable collision detecton
     */
    public static boolean pCollisionEnabled = true;
    /**
     * Size of buffer (in Bytes), by default the size of the Raspberry Pi 3 receiving
     */
    public static int receivingBufferSize = 163840;
    /**
     * Factor to increase the virtual buffer size w.r.t the receivingBufferSize
     */
    public static final int V_BUFFER_SIZE_FACTOR = 3;
    /**
     * Size of virtual buffer (in Bytes)
     */
    public static int receivingvBufferSize = V_BUFFER_SIZE_FACTOR * receivingBufferSize;
    /**
     * Counts the CC communication threads that stop
     */
    public static ConcurrentHashMap<String, String> communicationsClosed;
    /**
     * Time (ms) to wait while the communications are
     */
    public static final int CLOSSING_WAITING_TIME = 5000;
    /**
     * (packets) Initial size of the incoming queue q
     */
    public static final int RECEIVING_BUFFER_PACKET_SIZE = 350;
    /**
     * Matrix containing the range check result
     */
    public static AtomicBoolean[][] isInRange;

    private static int receivingvBufferTrigger;
    /**
     * Size of the buffer when it is flushed to avoid it to be filled
     */
    private static final double BUFFER_FULL_THRESHOLD = 0.8;

    private final ArduSim ardusim;
    /**
     * Stores the last sent message.end for each UAV in the current
     */
    private final AtomicLongArray prevMessageEnd;
    /**
     * Array with the message queues used as buffers in the CC
     */
    private MessageQueue[] mBuffer;
    /**
     * Array with virtual buffer to calculate packet collisions
     */
    private ConcurrentSkipListSet<Message>[] vBuffer;
    /**
     * (bytes) Array containing the current level of the vBuffer in the CC
     */
    private AtomicIntegerArray vBufferUsedSpace;
    /**
     * (ns) Array with the later completed transfer finish time for each sending UAV when using collision detection in the CC
     */
    private AtomicLongArray maxCompletedTEndTime;
    /**
     * Locks for concurrence when detecting  packet collisions in the CC
     */
    private ReentrantLock[] lock;

    // Statistics
    /**
     * For the statistics: Total number of packages send
     */
    private int totalPackagesSend;
    /**
     * For the statistics: Total number of packages received
     */
    private int totalPackagesReceived;
    private int[] packetWaitedPrevSending, packetWaitedMediaAvailable;
    private AtomicIntegerArray receiverOutOfRange, receiverWasSending, receiverVirtualQueueFull, receiverQueueFull, successfullyReceived;
    private AtomicIntegerArray discardedForCollision, successfullyEnqueued, successfullyProcessed;

    private static final long MESSAGE_WAITING_TIME = 1;				// (ms) Waiting time to check if a message arrives

    @SuppressWarnings("unchecked")
    public CommLinkObjectSimulation(int numUAVs, int port) {
        this.ardusim = API.getArduSim();
        prevMessageEnd = new AtomicLongArray(numUAVs);
        mBuffer = new MessageQueue[numUAVs];
        for (int i = 0; i < Param.numUAVs; i++) {
            mBuffer[i] = new MessageQueue();
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

        if (port == UAVParam.broadcastPort) {
            communicationsClosed = new ConcurrentHashMap<>(numUAVs);
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
    }

    @Override
    public void sendBroadcastMessage(int numUAV, byte[] message) {
        if (Param.simStatus == Param.SimulatorState.TEST_FINISHED) {
            blockCommunication(numUAV, "s");
        }
        waitForLastTransmission(numUAV);
        if (carrierSensingEnabled) {
            carrierSensing(numUAV);
        }
        sendMessage(numUAV, message);
    }

    private void blockCommunication(int numUAV, String mode) {
        communicationsClosed.put(mode + numUAV, mode + numUAV);
        while (Param.simStatus == Param.SimulatorState.TEST_FINISHED) {
            ardusim.sleep(SimParam.LONG_WAITING_TIME);
        }
    }

    private void sendMessage(int numUAV, byte[] message) {
        // 3. Send the packet to all UAVs but the sender
        Message sendingMessage = new Message(numUAV,System.nanoTime(), message);
        totalPackagesSend++;
        prevMessageEnd.set(numUAV,sendingMessage.end);
        for (int i = 0; i < Param.numUAVs; i++) {
            if(i == numUAV){ continue; }
            if(!isInRange[numUAV][i].get()){
                receiverOutOfRange.incrementAndGet(i);
                continue;
            }
            if(prevMessageEnd.get(i) > System.nanoTime()){
                receiverWasSending.incrementAndGet(i);
                continue;
            }

            if (pCollisionEnabled) {
                if (vBufferUsedSpace.get(i) + message.length <= receivingvBufferSize) {
                    vBuffer[i].add(sendingMessage);
                    vBufferUsedSpace.addAndGet(i, message.length);
                    successfullyEnqueued.incrementAndGet(i);
                } else {
                    receiverVirtualQueueFull.incrementAndGet(i);
                }
                if (vBufferUsedSpace.get(i) >= receivingvBufferTrigger) {
                    checkPacketCollisions(i, false);
                }
            } else {
                if (mBuffer[i].offerLast(sendingMessage)) {
                    successfullyReceived.incrementAndGet(i);
                } else {
                    receiverQueueFull.incrementAndGet(i);
                }
            }
        }
    }

    private void carrierSensing(int numUAV) {
        // 2. Wait if packet transmissions are detected in the UAV range (carrier sensing)
        boolean mediaIsAvailable = false;
        boolean messageWaited = false;
        while (!mediaIsAvailable) {
            mediaIsAvailable = true;
            for (int i = 0; i < Param.numUAVs; i++) {
                boolean otherUAVisSending = i != numUAV && isInRange[numUAV][i].get() && prevMessageEnd.get(i) > System.nanoTime();
                if (otherUAVisSending) {
                    mediaIsAvailable = false;
                    if (!messageWaited) {
                        packetWaitedMediaAvailable[numUAV]++;
                        messageWaited = true;
                    }
                    ardusim.sleep(MESSAGE_WAITING_TIME);
                }
            }
        }
    }

    private void waitForLastTransmission(int numUAV) {
        // 1. Can not send until the last transmission has finished
        long waitTime = (prevMessageEnd.get(numUAV) - System.nanoTime())/1000;
        if(waitTime > 0){
            ardusim.sleep(waitTime);
            packetWaitedPrevSending[numUAV]++;
        }
    }

    @Override
    public byte[] receiveMessage(int numUAV, int socketTimeout) {
        if (Param.simStatus == Param.SimulatorState.TEST_FINISHED) {
            blockCommunication(numUAV, "r");
        }
        long start = System.currentTimeMillis();
        long elapsedTime = 0;
        byte[] receivedBuffer = null;
        if (pCollisionEnabled) {
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
                    Message message = mBuffer[numUAV].peekFirst();
                    long now = System.nanoTime();
                    while (message.end > now) {
                        ardusim.sleep(MESSAGE_WAITING_TIME);
                    }
                    totalPackagesReceived++;
                    message = mBuffer[numUAV].pollFirst();
                    receivedBuffer = message.message;
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
                totalPackagesReceived++;
                receivedBuffer = mBuffer[numUAV].pollFirst().message;
            }
        }
        return receivedBuffer;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(2000);
        sb.append("\n\t").append(Text.CARRIER_SENSING_ENABLED).append(" ").append(carrierSensingEnabled);
        sb.append("\n\t").append(Text.PACKET_COLLISION_DETECTION_ENABLED).append(" ").append(pCollisionEnabled);
        sb.append("\n\t").append(Text.BUFFER_SIZE).append(" ").append(receivingBufferSize).append(" ").append(Text.BYTES);
        sb.append("\n\t").append(Text.WIFI_MODEL).append(" ").append(Param.selectedWirelessModel.getName());
        if (Param.selectedWirelessModel == WirelessModel.FIXED_RANGE) {
            sb.append(": ").append(Param.fixedRange).append(" ").append(Text.METERS);
        }
        sb.append("\n\t").append(Text.TOT_SENT_PACKETS).append(" ").append(totalPackagesSend);
        ValidationTools validationTools = API.getValidationTools();
        if (Param.numUAVs > 1 && totalPackagesSend > 0) {
            long packetWaitedPrevSendingTot = 0;
            for (int i = 0; i < Param.numUAVs; i++) {
                packetWaitedPrevSendingTot = packetWaitedPrevSendingTot + packetWaitedPrevSending[i];
            }
            sb.append("\n\t\t").append(Text.TOT_WAITED_PREV_SENDING).append(" ").append(packetWaitedPrevSendingTot)
                    .append(" (").append(validationTools.roundDouble((100.0 * packetWaitedPrevSendingTot) / totalPackagesSend, 3)).append("%)");
            if (carrierSensingEnabled) {
                long packetWaitedMediaAvailableTot = 0;
                for (int i = 0; i < Param.numUAVs; i++) {
                    packetWaitedMediaAvailableTot = packetWaitedMediaAvailableTot + packetWaitedMediaAvailable[i];
                }
                sb.append("\n\t\t").append(Text.TOT_WAITED_MEDIA_AVAILABLE).append(" ").append(packetWaitedMediaAvailableTot)
                        .append(" (").append(validationTools.roundDouble((100.0 * packetWaitedMediaAvailableTot) / totalPackagesSend, 3)).append("%)");
            }
            long potentiallyReceived = totalPackagesSend * (Param.numUAVs - 1);
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
            if (pCollisionEnabled) {
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
                    .append(" (").append(validationTools.roundDouble((100.0 * receiverOutOfRangeTot) / potentiallyReceived, 3)).append("%)");
            sb.append("\n\t\t").append(Text.TOT_LOST_RECEIVER_WAS_SENDING).append(" ").append(receiverWasSendingTot)
                    .append(" (").append(validationTools.roundDouble((100.0 * receiverWasSendingTot) / potentiallyReceived, 3)).append("%)");
            if (pCollisionEnabled) {
                sb.append("\n\t\t").append(Text.TOT_VIRTUAL_QUEUE_WAS_FULL).append(" ").append(receiverVirtualQueueFullTot)
                        .append(" (").append(validationTools.roundDouble((100.0 * receiverVirtualQueueFullTot) / potentiallyReceived, 3)).append("%)");
                sb.append("\n\t\t").append(Text.TOT_RECEIVED_IN_VBUFFER).append(" ").append(successfullyEnqueuedTot)
                        .append(" (").append(validationTools.roundDouble((100.0 * successfullyEnqueuedTot) / potentiallyReceived, 3)).append("%)");
            } else {
                // We must include the received messages but discarded because the buffer was full
                successfullyReceivedTot = successfullyReceivedTot + receiverQueueFullTot;
                sb.append("\n\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
                        .append(" (").append(validationTools.roundDouble((100.0 * successfullyReceivedTot) / potentiallyReceived, 3)).append("%)");
            }
            long inBufferTot = successfullyReceivedTot - receiverQueueFullTot - totalPackagesReceived;
            if (pCollisionEnabled) {
                if (successfullyEnqueuedTot != 0) {
                    long successfullyProcessedTot = 0;
                    for (int i = 0; i < Param.numUAVs; i++) {
                        successfullyProcessedTot = successfullyProcessedTot + successfullyProcessed.get(i);
                    }
                    long inVBufferTot = successfullyEnqueuedTot - successfullyProcessedTot;
                    sb.append("\n\t\t\t").append(Text.TOT_REMAINING_IN_VBUFFER).append(" ").append(inVBufferTot)
                            .append(" (").append(validationTools.roundDouble((100.0 * inVBufferTot) / successfullyEnqueuedTot, 3)).append("%)");
                    sb.append("\n\t\t\t").append(Text.TOT_PROCESSED).append(" ").append(successfullyProcessedTot)
                            .append(" (").append(validationTools.roundDouble((100.0 * successfullyProcessedTot) / successfullyEnqueuedTot, 3)).append("%)");
                    if (successfullyProcessedTot != 0) {
                        long discardedForCollisionTot = 0;
                        for (int i = 0; i < Param.numUAVs; i++) {
                            discardedForCollisionTot = discardedForCollisionTot + discardedForCollision.get(i);
                        }
                        sb.append("\n\t\t\t\t").append(Text.TOT_DISCARDED_FOR_COLLISION).append(" ").append(discardedForCollisionTot)
                                .append(" (").append(validationTools.roundDouble((100.0 * discardedForCollisionTot) / successfullyProcessedTot, 3)).append("%)");
                        sb.append("\n\t\t\t\t").append(Text.TOT_RECEIVED).append(" ").append(successfullyReceivedTot)
                                .append(" (").append(validationTools.roundDouble((100.0 * successfullyReceivedTot) / successfullyProcessedTot, 3)).append("%)");
                        if (successfullyReceivedTot != 0) {
                            sb.append("\n\t\t\t\t\t").append(Text.TOT_QUEUE_WAS_FULL).append(" ").append(receiverQueueFullTot)
                                    .append(" (").append(validationTools.roundDouble((100.0 * receiverQueueFullTot) / successfullyReceivedTot, 3)).append("%)");
                            sb.append("\n\t\t\t\t\t").append(Text.TOT_REMAINING_IN_BUFFER).append(" ").append(inBufferTot)
                                    .append(" (").append(validationTools.roundDouble((100.0 * inBufferTot) / successfullyReceivedTot, 3)).append("%)");
                            sb.append("\n\t\t\t\t\t").append(Text.TOT_USED_OK).append(" ").append(totalPackagesReceived)
                                    .append(" (").append(validationTools.roundDouble((100.0 * totalPackagesReceived) / successfullyReceivedTot, 3)).append("%)");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private void checkPacketCollisions(int numUAV, boolean isReceiver) {
        // Maybe another thread would also try to check at the same time
        lock[numUAV].lock();
        try {
            if (isReceiver || vBufferUsedSpace.get(numUAV) >= receivingvBufferTrigger) {
                // 1. First iteration through the virtual buffer to check collisions between messages
                Iterator<Message> it = vBuffer[numUAV].iterator();
                Message prev, pos, next;
                if (it.hasNext()) {
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
            }
        } finally {
            lock[numUAV].unlock();
        }
    }


    /**
     * Method to initialize the Commlink
     * @param numUAVs: number of UAVs
     * @param carrierSensing: boolean to turn on carrier sensing
     * @param packetCollisionDetection: boolean to turn on packet Collision Detection
     * @param bufferSize: size of the buffer
     */
    public static void init(int numUAVs,boolean carrierSensing, boolean packetCollisionDetection, int bufferSize){
        if(numUAVs <= 0 | bufferSize <= 0){
            throw new Error("Input parameters invalid");
        }
        initParameters(carrierSensing, packetCollisionDetection, bufferSize);
        initCollisionAndCommunication(numUAVs);
    }
    /**
     * Set the CommLinkobject parameters
     * @param carrierSensing: boolean to turn on carrier sensing
     * @param packetCollisionDetection: boolean to turn on packet Collision Detection
     * @param bufferSize: size of the buffer
     */
    private static void initParameters(boolean carrierSensing, boolean packetCollisionDetection, int bufferSize){
        if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
            carrierSensingEnabled = carrierSensing;
            pCollisionEnabled = packetCollisionDetection;
            receivingBufferSize = bufferSize;
            receivingvBufferSize = V_BUFFER_SIZE_FACTOR * receivingBufferSize;
            receivingvBufferTrigger = (int)Math.rint(BUFFER_FULL_THRESHOLD * receivingvBufferSize);
        }
    }
    /**
     * Create the arrays UAVParam.distance and CommlinkObject.isInRange
     * @param numUAVs: number of UAVs (gives size to 2d arrays)
     */
    private static void initCollisionAndCommunication(int numUAVs) {
        // Collision and communication range parameters
        if (Param.role == ArduSim.SIMULATOR_GUI || Param.role == ArduSim.SIMULATOR_CLI) {
            UAVParam.distances = new AtomicReference[numUAVs][numUAVs];
            CommLinkObjectSimulation.isInRange = new AtomicBoolean[numUAVs][numUAVs];
            for (int i = 0; i < numUAVs; i++) {
                for (int j = 0; j < numUAVs; j++) {
                    UAVParam.distances[i][j] = new AtomicReference<>();
                    CommLinkObjectSimulation.isInRange[i][j] = new AtomicBoolean();
                }
            }
        }
    }
}

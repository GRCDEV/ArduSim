package com.api.communications.lowLevel;

import com.api.communications.lowLevel.CommLinkObjectSimulation;
import com.api.communications.lowLevel.Message;
import com.api.communications.lowLevel.MessageQueue;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link MessageQueue}
 */
class MessageQueueTest {

    MessageQueue queue = new MessageQueue();

    /**
     * Test all the function of the messageQueue at once
     * isEmpty, offer, peek, pop
     */
    @Test
    void TestQueue() {
        int senderPos = 3;
        assertTrue(queue.isEmpty());

        queue.offerLast(new Message(senderPos,0,"a".getBytes(StandardCharsets.UTF_8)));
        assertFalse(queue.isEmpty());

        Message m = queue.peekFirst();
        assertFalse(queue.isEmpty());
        assertEquals(senderPos,m.senderPos);

        Message m1 = queue.pollFirst();
        assertEquals(senderPos,m1.senderPos);
        assertEquals(m.senderPos,m1.senderPos);

        assertTrue(queue.isEmpty());

        Message m2 = queue.peekFirst();
        assertNull(m2);

        Message m3 = queue.pollFirst();
        assertNull(m3);

        String longMessage = "this is a long message to see if the buffer maximum is working as well";
        int nr_of_bytes = longMessage.getBytes(StandardCharsets.UTF_8).length;
        int bufferSize = 0;
        while(queue.offerLast(new Message(senderPos,bufferSize,longMessage.getBytes(StandardCharsets.UTF_8)))){
            bufferSize += nr_of_bytes;
        }
        assertTrue(bufferSize+ nr_of_bytes >= CommLinkObjectSimulation.receivingBufferSize);
    }
}
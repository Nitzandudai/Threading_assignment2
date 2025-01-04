package bgu.spl.mics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import bgu.spl.mics.example.services.ExampleMessageSenderService;

public class MessageBusImplTest {

    private MessageBusImpl messageBus;
    private ArrayList<MicroService> registeredServices;

    @BeforeEach
    public void setUp() {
        messageBus = MessageBusImpl.getInstance();
        registeredServices = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        // ביטול רישום לכל השירותים שנרשמו במהלך הטסט
        for (MicroService service : registeredServices) {
            messageBus.unregister(service);
        }
        registeredServices.clear();
    }

    @Test
    public void subscribeEventTest() {
        CountDownLatch latch = new CountDownLatch(1);
        ExampleEventHandlerService handler1 = new ExampleEventHandlerService("Handler1", new String[]{"2"}, latch);
        ExampleEvent event = new ExampleEvent("TestEvent");

        messageBus.register(handler1);
        registeredServices.add(handler1);

        messageBus.subscribeEvent(event.getClass(), handler1);

        Queue<MicroService> subscribers = messageBus.getEventSub(event.getClass());

        assertNotNull(subscribers, "The subscribers queue for the event should not be null.");
        assertEquals(1, subscribers.size(), "The subscribers queue should contain exactly 1 handler.");
        assertTrue(subscribers.contains(handler1), "The subscribers queue should contain Handler1.");
    }

    @Test
    public void subscribeBroadcastTest() {
        CountDownLatch latch = new CountDownLatch(1);

        ExampleBroadcastListenerService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"5"}, latch);
        ExampleBroadcastListenerService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"3"}, latch);

        messageBus.register(listener1);
        messageBus.register(listener2);
        registeredServices.add(listener1);
        registeredServices.add(listener2);

        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);

        ArrayList<MicroService> subscribers = messageBus.getBrodSub(ExampleBroadcast.class);

        assertNotNull(subscribers, "The subscribers list should not be null.");
        assertEquals(2, subscribers.size(), "The subscribers list should contain exactly 2 listeners.");
        assertTrue(subscribers.contains(listener1), "The subscribers list should contain listener1.");
        assertTrue(subscribers.contains(listener2), "The subscribers list should contain listener2.");
    }

    @Test
    public void completeTest() {
        CountDownLatch latch = new CountDownLatch(1);

        ExampleEvent event = new ExampleEvent("TestSender");
        MicroService handler = new ExampleBroadcastListenerService("Handler", new String[]{"1"}, latch);

        messageBus.register(handler);
        registeredServices.add(handler);

        messageBus.subscribeEvent(event.getClass(), handler);

        Future<String> future = messageBus.sendEvent(event);

        messageBus.complete(event, "Completed");

        assertTrue(future.isDone());
        assertEquals("Completed", future.get());
    }

    @Test
    public void sendBroadcastTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ExampleBroadcastListenerService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"2"}, latch);
        ExampleBroadcastListenerService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"3"}, latch);
        ExampleBroadcast broadcast = new ExampleBroadcast("TestBroadcast");

        messageBus.register(listener1);
        messageBus.register(listener2);
        registeredServices.add(listener1);
        registeredServices.add(listener2);

        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
        messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);

        messageBus.sendBroadcast(broadcast);

        BlockingQueue<Message> queue1 = messageBus.getPersonalQueues(listener1);
        BlockingQueue<Message> queue2 = messageBus.getPersonalQueues(listener2);

        assertTrue(queue1.contains(broadcast));
        assertTrue(queue2.contains(broadcast));
    }

    @Test
    public void sendEventTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ExampleEvent event = new ExampleEvent("TestEvent");
        MicroService handler = new ExampleBroadcastListenerService("Handler", new String[]{"1"}, latch);

        messageBus.register(handler);
        registeredServices.add(handler);

        messageBus.subscribeEvent(event.getClass(), handler);

        Future<String> future = messageBus.sendEvent(event);

        BlockingQueue<Message> queue1 = messageBus.getPersonalQueues(handler);
        assertTrue(queue1.contains(event));
        assertEquals(future, messageBus.getevEntFutureMap(event));
    }

    @Test
    public void registerMicroServiceTest() {
        CountDownLatch latch = new CountDownLatch(1);

        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"5"}, latch);
        messageBus.register(eventHandler);
        registeredServices.add(eventHandler);
        assertNotNull(messageBus.getPersonalQueues(eventHandler));

        MicroService messageSender = new ExampleMessageSenderService("MessageSender", new String[]{"event"}, latch);
        messageBus.register(messageSender);
        registeredServices.add(messageSender);
        assertNotNull(messageBus.getPersonalQueues(messageSender));

        assertNotSame(messageBus.getPersonalQueues(messageSender), messageBus.getPersonalQueues(eventHandler));
    }

    @Test
    public void testUnregister() {
        CountDownLatch latch = new CountDownLatch(1);

        MicroService broadcastListener = new ExampleBroadcastListenerService("BroadcastListener", new String[]{"1"}, latch);
        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"1"}, latch);
        MicroService sender = new ExampleMessageSenderService("Sender", new String[]{"event"}, latch);

        messageBus.register(broadcastListener);
        messageBus.register(eventHandler);
        messageBus.register(sender);
        registeredServices.add(broadcastListener);
        registeredServices.add(eventHandler);
        registeredServices.add(sender);

        messageBus.subscribeBroadcast(ExampleBroadcast.class, broadcastListener);
        messageBus.subscribeEvent(ExampleEvent.class, eventHandler);

        ExampleBroadcast broadcast = new ExampleBroadcast("Sender");
        messageBus.sendBroadcast(broadcast);

        ExampleEvent event = new ExampleEvent("Sender");
        messageBus.sendEvent(event);

        messageBus.unregister(broadcastListener);
        assertFalse(messageBus.getBrodSub(ExampleBroadcast.class).contains(broadcastListener));
        assertNull(messageBus.getPersonalQueues(broadcastListener));

        messageBus.unregister(eventHandler);
        assertFalse(messageBus.getEventSub(ExampleEvent.class).contains(eventHandler));
        assertNull(messageBus.getPersonalQueues(eventHandler));

        assertNotNull(messageBus.getPersonalQueues(sender));
    }

    @Test
    public void testAwaitMessage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ExampleEvent event = new ExampleEvent("testEvent");
        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"1"}, latch);

        messageBus.register(eventHandler);
        registeredServices.add(eventHandler);

        messageBus.subscribeEvent(ExampleEvent.class, eventHandler);

        messageBus.sendEvent(event);
        assertEquals(event, messageBus.awaitMessage(eventHandler));
    }
}
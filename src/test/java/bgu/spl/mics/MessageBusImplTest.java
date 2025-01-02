package bgu.spl.mics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;

import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import bgu.spl.mics.example.services.ExampleMessageSenderService;

public class MessageBusImplTest {

    @Test
    public void subscribeEventTest() {
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        ExampleEventHandlerService handler1 = new ExampleEventHandlerService("Handler1", new String[]{"2"});
        ExampleEvent event = new ExampleEvent("TestEvent");

        messageBus.register(handler1);
        messageBus.subscribeEvent(event.getClass(), handler1);

        Queue<MicroService> subscribers = messageBus.getEventSub(event.getClass());

        assertNotNull(subscribers, "The subscribers queue for the event should not be null.");
        assertEquals(1, subscribers.size(), "The subscribers queue should contain exactly 1 handler.");
        assertTrue(subscribers.contains(handler1), "The subscribers queue should contain Handler1.");
    }

    @Test
    public void subscribeBroadcastTest() {
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        ExampleBroadcastListenerService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"5"});
        ExampleBroadcastListenerService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"3"});

        messageBus.register(listener1);
        messageBus.register(listener2);

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
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        ExampleEvent event = new ExampleEvent("TestSender");
        MicroService handler = new ExampleBroadcastListenerService("Handler", new String[]{"1"});
        messageBus.register(handler);
        messageBus.subscribeEvent(event.getClass(), handler);

        Future<String> future = (Future<String>) messageBus.sendEvent(event);

        messageBus.complete(event, "Completed");

        assertTrue(future.isDone());
        assertEquals("Completed", future.get());
    }

    @Test
    public void sendBroadcastTest() throws InterruptedException {
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        ExampleBroadcastListenerService listener1 = new ExampleBroadcastListenerService("Listener1", new String[]{"2"});
        ExampleBroadcastListenerService listener2 = new ExampleBroadcastListenerService("Listener2", new String[]{"3"});
        ExampleBroadcast broadcast = new ExampleBroadcast("TestBroadcast");

        messageBus.register(listener1);
        messageBus.register(listener2);
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
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        ExampleEvent event = new ExampleEvent("TestEvent");
        MicroService handler = new ExampleBroadcastListenerService("Handler", new String[]{"1"});

        messageBus.register(handler);
        messageBus.subscribeEvent(event.getClass(), handler);

        Future<String> future = messageBus.sendEvent(event);

        BlockingQueue<Message> queue1 = messageBus.getPersonalQueues(handler);
        assertTrue(queue1.contains(event));
        assertEquals(future, messageBus.getevEntFutureMap(event));
    }

    @Test
    public void registerMicroServiceTest() {
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"5"});
        messageBus.register(eventHandler);
        assertNotNull(messageBus.getPersonalQueues(eventHandler));

        MicroService messageSender = new ExampleMessageSenderService("MessageSender", new String[]{"event"});
        messageBus.register(messageSender);
        assertNotNull(messageBus.getPersonalQueues(messageSender));

        assertNotSame(messageBus.getPersonalQueues(messageSender), messageBus.getPersonalQueues(eventHandler));
    }

    @Test
    public void testUnregister() {
        MessageBusImpl messageBus = MessageBusImpl.getInstance();

        MicroService broadcastListener = new ExampleBroadcastListenerService("BroadcastListener", new String[]{"1"});
        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"1"});
        MicroService sender = new ExampleMessageSenderService("Sender", new String[]{"event"});

        messageBus.register(broadcastListener);
        messageBus.register(eventHandler);
        messageBus.register(sender);

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
        MessageBusImpl messageBus = MessageBusImpl.getInstance();
        ExampleEvent event = new ExampleEvent("testEvent");
        MicroService eventHandler = new ExampleEventHandlerService("EventHandler", new String[]{"1"});
        messageBus.register(eventHandler);
        messageBus.subscribeEvent(ExampleEvent.class, eventHandler);
        messageBus.sendEvent(event);
        assertEquals(event, messageBus.awaitMessage(eventHandler));
    }
}

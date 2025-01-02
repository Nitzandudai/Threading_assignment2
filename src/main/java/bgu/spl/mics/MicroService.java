package bgu.spl.mics;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * The MicroService is an abstract class that any micro-service in the system
 * must extend. The abstract MicroService class is responsible to get and
 * manipulate the singleton {@link MessageBus} instance.
 */
public abstract class MicroService implements Runnable {

    private boolean terminated = false;
    private final String name;
    private final HashMap<Class<? extends Message>, Callback<?>> callbackMap;
    private CountDownLatch latch;

    //====================================================================================================================

    /**
     * @param name the micro-service name (used mainly for debugging purposes -
     *             does not have to be unique)
     */
    public MicroService(String name, CountDownLatch latch) {
        this.name = name;
        this.callbackMap = new HashMap<>();
        this.latch = latch;
    }

    //====================================================================================================================

    /**
     * Subscribes to events of type {@code type} with the callback
     * {@code callback}.
     */
    protected final <T, E extends Event<T>> void subscribeEvent(Class<E> type, Callback<E> callback) {
        callbackMap.put(type, callback);
        MessageBusImpl.getInstance().subscribeEvent(type, this);
    }

    //====================================================================================================================

    /**
     * Subscribes to broadcast message of type {@code type} with the callback
     * {@code callback}.
     */
    protected final <B extends Broadcast> void subscribeBroadcast(Class<B> type, Callback<B> callback) {
        callbackMap.put(type, callback);
        MessageBusImpl.getInstance().subscribeBroadcast(type, this);
    }

    //====================================================================================================================

    /**
     * Sends the event {@code e} using the message-bus and receive a {@link Future<T>}
     * object that may be resolved to hold a result.
     */
    protected final <T> Future<T> sendEvent(Event<T> e) {
        return MessageBusImpl.getInstance().sendEvent(e);
    }

    //====================================================================================================================

    /**
     * A Micro-Service calls this method in order to send the broadcast message {@code b} using the message-bus
     * to all the services subscribed to it.
     */
    protected final void sendBroadcast(Broadcast b) {
        MessageBusImpl.getInstance().sendBroadcast(b);
    }

    //====================================================================================================================

    /**
     * Completes the received request {@code e} with the result {@code result}
     * using the message-bus.
     */
    protected final <T> void complete(Event<T> e, T result) {
        MessageBusImpl.getInstance().complete(e, result);
    }

    //====================================================================================================================

    /**
     * this method is called once when the event loop starts.
     */
    protected abstract void initialize();

    //====================================================================================================================

    /**
     * Signals the event loop that it must terminate after handling the current
     * message.
     */
    protected final void terminate() {
        System.out.println("MicroService " + name + " is terminated");
        this.terminated = true;
        Thread.currentThread().interrupt();
    }

    //====================================================================================================================

    /**
     * @return the name of the service - the service name is given to it in the
     *         construction time and is used mainly for debugging purposes.
     */
    public final String getName() {
        return name;
    }

    //====================================================================================================================

    /**
     * The entry point of the micro-service.
     */
    @Override
    public final void run() {
        try {
            MessageBusImpl.getInstance().register(this);
            initialize();
            while (!terminated) {
                try {
                    Message message = MessageBusImpl.getInstance().awaitMessage(this);
                    @SuppressWarnings("unchecked") //לוטם אמר שמותר להוסיף אם הבעיה נובעת מהבדל בין טיפוס גנרי לטיפוס לא ידוע
                    Callback<Message> callback = (Callback<Message>) callbackMap.get(message.getClass());
                    if (callback != null) {//בדיקה לא בטוח הכרחית אבל ליתר ביטחון
                        callback.call(message);
                    }
                } catch (InterruptedException e) {
                    if (terminated) {
                        break; 
                    }
                    Thread.currentThread().interrupt(); //כדי שהת'רד ידע שהוא INTERRUPTED גם אחרי שהוא תפס את החריגה
                }
            }
        } finally {
            MessageBusImpl.getInstance().unregister(this); // שחרור רישום
        }
    }


    //====================================================================================================================

    public CountDownLatch getLatch() {
        return latch;
    }
}

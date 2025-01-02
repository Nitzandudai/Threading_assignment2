package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting
 * TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {

    int TickTime;
    int Duration;
    int currTime;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime The duration of each tick in milliseconds.
     * @param Duration The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration, CountDownLatch latch) {
        super("TimeService", latch);
        this.TickTime = 1000;
        this.Duration = Duration;
        this.currTime = 1;

    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified
     * duration.
     */
    @Override
    protected void initialize() {
        this.subscribeBroadcast(ShutAll.class, ShutAll -> {
            this.terminate();
        });

        this.subscribeBroadcast(CrashedBroadcast.class, CrashedBroadcast -> {
            this.terminate();
        });

        this.subscribeBroadcast(TickBroadcast.class, TickBroadcast -> {
            System.out.println("Tick " + TickBroadcast.getTick() + " exepted");
            this.currTime = TickBroadcast.getTick();
            if (Duration < currTime) {
                sendBroadcast(new ShutAll());
                this.terminate();
            }
            try {
                Thread.sleep(this.TickTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace(); // במידה וכן יש לנו INTERRUPT נרצה לדעת מה גרם לזה
            }

            sendBroadcast(new TickBroadcast(this.currTime + 1));
            StatisticalFolder.getInstance().addTime();
        });

        // Notify that registration is complete
        getLatch().countDown();

        // Wait for all services to register
        try {
            getLatch().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(getName() + " was interrupted while waiting for latch.");
            return;
        }

        // Start by sending the first TickBroadcast
        System.out.println("Try sending first TickBroadcast.");
        StatisticalFolder.getInstance().addTime();
        sendBroadcast(new TickBroadcast(this.currTime));
        System.out.println("Sending first TickBroadcast.");
        

    }

}

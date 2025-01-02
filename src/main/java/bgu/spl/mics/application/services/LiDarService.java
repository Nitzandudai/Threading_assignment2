package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and
 * process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    private LiDarWorkerTracker lidar;
    private int timeToTerminate;
    private int currTime;
    private ConcurrentHashMap<CompositeKey, DetectObjectsEvent> TrackedOToEvent;

    // ====================================================================================================================

    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service
     *                           will use to process data.
     */
    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker, CountDownLatch latch) {
        super("Lidar", latch);
        this.lidar = LiDarWorkerTracker;
        this.timeToTerminate = lidar.getFrequency();
        this.currTime = 0;
        this.TrackedOToEvent = new ConcurrentHashMap<>();

    }

    // ====================================================================================================================
    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {

        this.subscribeBroadcast(CrashedBroadcast.class, CrashedBroadcast -> {
            StatisticalFolder.getInstance().addToLastTracked(this.lidar.getID(), this.lidar.getLast());
            this.terminate();
        });

        this.subscribeBroadcast(TickBroadcast.class, TickBroadcast -> {
            this.currTime = TickBroadcast.getTick();
            MissionPreformer(this.currTime);
        });

        this.subscribeBroadcast(ShutAll.class, ShutAll ->{
        this.terminate();
        });

        this.subscribeEvent(DetectObjectsEvent.class, DetectObjectsEvent -> {
            this.lidar.addStampedObjects(DetectObjectsEvent.getObjects().getTime(), DetectObjectsEvent.getObjects());
            CompositeKey key = new CompositeKey(DetectObjectsEvent.getCameraId(),
                    DetectObjectsEvent.getObjects().getTime());
            TrackedOToEvent.putIfAbsent(key, DetectObjectsEvent);
            MissionPreformer(this.currTime);
        });

        getLatch().countDown();

    }

    // ====================================================================================================================

    public void MissionPreformer(int time) {
        ArrayList<TrackedObject> currObject = this.lidar.getTrackedObjects(this.currTime);
        if (currObject == null) {
            if (this.lidar.geStatus() == STATUS.DOWN) {
                timeToTerminate--;
                if (timeToTerminate == 0) {
                    sendBroadcast(new TerminatedBroadcast("LiDar" + lidar.getID()));
                    terminate();
                }
            } else {
                if (this.lidar.geStatus() == STATUS.ERROR) {
                    sendBroadcast(new CrashedBroadcast(lidar.getID(), "LiDar"));
                    this.terminate();
                }
            }
        } else {
            for (TrackedObject t : currObject) {
                CompositeKey key = new CompositeKey(t.getId(), t.getTime());
                DetectObjectsEvent event = TrackedOToEvent.get(key);
                this.complete(event, currObject);
            }
            this.sendEvent(new TrackedObjectsEvent(currObject));
        }

    }
    // ====================================================================================================================

    // Composite key for map
    private static class CompositeKey {
        private final String id;
        private final int time;

        public CompositeKey(String id, int time) {
            this.id = id;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CompositeKey that = (CompositeKey) o;
            return time == that.time && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + time;
        }
    }
}

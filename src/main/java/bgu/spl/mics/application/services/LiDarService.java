package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
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
    private PriorityBlockingQueue<StampedDetectedObjects> StampedObjects;

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
        Comparator<StampedDetectedObjects> comparator = Comparator.comparingInt(StampedDetectedObjects::getTime);
        this.StampedObjects = new PriorityBlockingQueue<>(11, comparator);

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

        this.subscribeBroadcast(ShutAll.class, ShutAll -> {
            this.terminate();
        });

        this.subscribeEvent(DetectObjectsEvent.class, DetectObjectsEvent -> {
            this.StampedObjects.add(DetectObjectsEvent.getObjects());
            CompositeKey key = new CompositeKey(DetectObjectsEvent.getObjects().getId(),
                    DetectObjectsEvent.getObjects().getTime());
            TrackedOToEvent.putIfAbsent(key, DetectObjectsEvent);
            MissionPreformer(this.currTime);
        });

        getLatch().countDown();

    }

    // ====================================================================================================================

    public void MissionPreformer(int time) {
        this.lidar.findErrorOrDown(time);
        if (this.lidar.geStatus() == STATUS.DOWN) {
            timeToTerminate--;
            if (timeToTerminate <= 0) {
                sendBroadcast(new TerminatedBroadcast("LiDar" + lidar.getID()));
                this.terminate();
            }
        }
        if (this.lidar.geStatus() == STATUS.ERROR) {
            sendBroadcast(new CrashedBroadcast(this.lidar.getID(), "LiDar"));
            this.terminate();
        }

        ArrayList<TrackedObject> currObject = new ArrayList<TrackedObject>();
        while (!StampedObjects.isEmpty() && this.StampedObjects.peek().getTime() <= time) {
            StampedDetectedObjects curr = this.StampedObjects.poll();
            ArrayList<TrackedObject> toAdd = this.lidar.getTrackedObjects(curr);
            currObject.addAll(toAdd);
            CompositeKey key = new CompositeKey(curr.getId(), curr.getTime());
            DetectObjectsEvent event = TrackedOToEvent.get(key);
            this.complete(event, toAdd);
        }
        if (!currObject.isEmpty()) {
            System.out.println("Sending TrackedObjectsEvent with " + currObject.size() + " tracked objects.");
            sendEvent(new TrackedObjectsEvent(currObject));
        }
    }
    // ====================================================================================================================

    // Composite key for map
    private static class CompositeKey {
        private final int id;
        private final int time;

        public CompositeKey(int id, int time) {
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
            return time == that.time && id == that.id;
        }

        @Override
        public int hashCode() {
            return 31 * id + time;
        }

    }
}

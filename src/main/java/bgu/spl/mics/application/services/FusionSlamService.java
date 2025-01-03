package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents
 * from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private int aliveSensor;
    private FusionSlam fusionSlam;
    private BlockingQueue<TrackedObject> lonelyTrackedObjects;
    

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global
     *                   map.
     */
    public FusionSlamService(FusionSlam fusionSlam, int aliveSensor, CountDownLatch latch) {
        super("FusionSlamService", latch);
        this.aliveSensor = aliveSensor;
        this.fusionSlam = FusionSlam.getInstance();
        this.lonelyTrackedObjects = new LinkedBlockingQueue<TrackedObject>();
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and
     * TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {

        this.subscribeBroadcast(CrashedBroadcast.class, CrashedBroadcast -> {
            StatisticalFolder.getInstance().setLandmarks(this.fusionSlam.getLandmarks());
            this.terminate();
        });

        this.subscribeBroadcast(ShutAll.class, ShutAll -> {
            StatisticalFolder.getInstance().setLandmarks(this.fusionSlam.getLandmarks());
            this.terminate();
        });

        this.subscribeEvent(PoseEvent.class, PoseEvent -> {
            fusionSlam.addPose(PoseEvent.getPose());
            this.complete(PoseEvent, true);
            ArrayList<TrackedObject> listy = new ArrayList<TrackedObject>();
            while (!lonelyTrackedObjects.isEmpty() && this.lonelyTrackedObjects.peek().getTime() == PoseEvent.getPose().getTime()) {
                listy.add(this.lonelyTrackedObjects.poll());
            }
            if (!listy.isEmpty()) {
                Pose posy = PoseEvent.getPose();
                fusionSlam.addOrChangeLM(listy, posy);
            }
        });

        this.subscribeBroadcast(TerminatedBroadcast.class, TerminatedBroadcast -> {
            this.aliveSensor--;
            if (this.aliveSensor == 0) {
                sendBroadcast(new ShutAll());
                StatisticalFolder.getInstance().setLandmarks(this.fusionSlam.getLandmarks());
                this.terminate();
            }
        });

        this.subscribeEvent(TrackedObjectsEvent.class, TrackedObjectsEvent -> {
            ArrayList<TrackedObject> listy = TrackedObjectsEvent.getList();
            Pose posy = fusionSlam.getPose(listy.get(0).getTime() - 1);
            if(posy == null){
                this.lonelyTrackedObjects.addAll(listy);
            }else{
                ArrayList<LandMark> updateLandMark = fusionSlam.addOrChangeLM(listy, posy);
               this.complete(TrackedObjectsEvent, updateLandMark);
            }
        });

        getLatch().countDown();

    }
}

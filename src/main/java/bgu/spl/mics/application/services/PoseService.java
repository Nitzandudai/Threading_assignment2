package bgu.spl.mics.application.services;


import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private GPSIMU gpsimu;
    private Pose currPose;
    private int tick;
    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu, CountDownLatch latch) {
        super("PoseService", latch);
        this.gpsimu = gpsimu;
        this.currPose = gpsimu.getCurrPose();
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(CrashedBroadcast.class, CrashedBroadcast ->{
            StatisticalFolder.getInstance().setPoses(this.gpsimu.getPoseList(this.tick));
            this.terminate();
        });

        this.subscribeBroadcast(ShutAll.class, ShutAll -> {
            this.terminate();
        });

        this.subscribeBroadcast(TickBroadcast.class, TickBroadcast->{
            this.tick = TickBroadcast.getTick();
            if(this.gpsimu.geStatus().equals(STATUS.DOWN)){
                sendBroadcast(new TerminatedBroadcast("GPSIMU"));
                this.terminate();
            }
            else{
                this.sendEvent(new PoseEvent(currPose));
                this.currPose = gpsimu.getCurrPose();
            }
        });

        getLatch().countDown();

    }
}

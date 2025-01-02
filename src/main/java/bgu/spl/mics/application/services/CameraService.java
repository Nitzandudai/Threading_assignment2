package bgu.spl.mics.application.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.ShutAll;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private Camera camera;
    private BlockingQueue<StampedDetectedObjects> detectedWOfreq;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera, CountDownLatch latch) {
        super("CameraService", latch);
        this.camera = camera;
        this.detectedWOfreq = new LinkedBlockingQueue<StampedDetectedObjects>();
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for
     * sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        this.subscribeBroadcast(ShutAll.class, ShutAll ->{
            this.terminate();
        });
        
        this.subscribeBroadcast(CrashedBroadcast.class, CrashedBroadcast -> {
            StatisticalFolder.getInstance().addToLastDetected(this.camera.getId(), this.camera.getLastDes());
            this.terminate();
        });

        this.subscribeBroadcast(TickBroadcast.class, TickBroadcast -> {
            StampedDetectedObjects currTimeObject = camera.next(TickBroadcast.getTick());

            //בודקות האם יש שגיאה או האם נגמר מה לשלוח
            if (currTimeObject == null) {
                if (camera.getStatus() == STATUS.DOWN) {
                    if(detectedWOfreq.isEmpty()){
                        sendBroadcast(new TerminatedBroadcast("Camera" + camera.getId()));
                        this.terminate();
                    }
                } else {
                    if (camera.getStatus() == STATUS.ERROR) {
                        sendBroadcast(new CrashedBroadcast(camera.getId(), "Camera"));
                        this.terminate();
                    }
                }
            } else {
                detectedWOfreq.add(currTimeObject);
                // לבדוק אם למצלמה יש מה לשלוח עכשי בעזרת התור כולל freq
                while (!detectedWOfreq.isEmpty() && TickBroadcast.getTick() >= (detectedWOfreq.peek().getTime() + camera.getFrequency())) {
                    StampedDetectedObjects toSend = detectedWOfreq.poll();
                    this.sendEvent(new DetectObjectsEvent(toSend, "Camera"+camera.getId()));
                    StatisticalFolder.getInstance().addToNumDetectedObjects(toSend.getDetectedObjects().size());
                }
            }
        });

        getLatch().countDown();

    }
}

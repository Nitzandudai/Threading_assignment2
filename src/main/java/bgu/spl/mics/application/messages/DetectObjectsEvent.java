package bgu.spl.mics.application.messages;

import java.util.ArrayList;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.TrackedObject;

public class DetectObjectsEvent implements Event<ArrayList<TrackedObject>> {
    private StampedDetectedObjects detectObjects;
    private String cameraId;

    public DetectObjectsEvent(StampedDetectedObjects detectObjects, String cameraId) {
        this.detectObjects = detectObjects;
        this.cameraId = cameraId;
    }

    public StampedDetectedObjects getObjects() {
        return this.detectObjects;
    }

    public String getCameraId() {
        return this.cameraId;
    }
}

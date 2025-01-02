package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time;
    private int id;
    private ArrayList<DetectedObject> DetectedObjects;

    public StampedDetectedObjects(int time, int id, ArrayList<DetectedObject> DetectedObjects) {
        this.time = time;
        this.id = id;
        this.DetectedObjects = DetectedObjects;

    }

    public int getTime() {
        return this.time;
    }

    public ArrayList<DetectedObject> getDetectedObjects() {
        return this.DetectedObjects;
    }

    public boolean isEmpty() {
        return (this.DetectedObjects.size() == 0);
    }

    public int getId() {
        return this.id;

    }
}

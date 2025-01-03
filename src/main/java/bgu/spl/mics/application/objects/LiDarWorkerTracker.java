package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using
 * data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    private int id;
    private int frequency;
    private STATUS status;
    private ArrayList<TrackedObject> lastTrackedObjects;

    // ====================================================================================================================
    /**
     * Constructs a LiDarWorkerTracker instance.
     * 
     * @param id                 the unique ID of the LiDAR worker
     * @param frequency          the frequency of object tracking updates
     * @param lastTrackedObjects the list of last tracked objects
     * 
     * @pre id > 0
     * @pre frequency > 0
     * @pre lastTrackedObjects != null
     */

    public LiDarWorkerTracker(int id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<TrackedObject>();
    }

    // ====================================================================================================================
    /**
     * Retrieves the tracked objects for a given timestamp.
     * 
     * @param time the current time to process tracked objects
     * @return a list of tracked objects or null if no objects are tracked
     * 
     * @pre time > 0
     * @post getTrackedObjects(time) != null implies output.size() > 0
     * @post geStatus() == STATUS.DOWN if all objects are processed
     * @post geStatus() == STATUS.ERROR if an error is found in LiDarDataBase
     */

    public void findErrorOrDown(int time) {
        if (LiDarDataBase.getInstance(null).isDONE() == true) {
            this.status = STATUS.DOWN;
        }
        if (LiDarDataBase.getInstance(null).findError(time) == true) {
            StatisticalFolder.getInstance().setError("disconnected", "LiDar" + this.id);
            StatisticalFolder.getInstance().addToLastTracked(this.id,this.lastTrackedObjects.get(this.lastTrackedObjects.size() - 1));
            StatisticalFolder.getInstance().setIfThereIsError(true);
            this.status = STATUS.ERROR;
        }
    }

    public ArrayList<TrackedObject> getTrackedObjects (StampedDetectedObjects StampedDetectedObjects){
        ArrayList<TrackedObject> output = new ArrayList<>();
        for (DetectedObject d : StampedDetectedObjects.getDetectedObjects()) {
            String id = d.getId();
            String description = d.getDescription();
            int reveilingTime = StampedDetectedObjects.getTime();
            ArrayList<CloudPoint> cloudy = LiDarDataBase.getInstance(null).findPoints(id, reveilingTime);

            TrackedObject tracky = new TrackedObject(id, reveilingTime, description, cloudy);
            this.lastTrackedObjects.add(tracky); // מוסיפות לרשימה של אוביקטים שנצפו
            output.add(tracky);// מוסיפות לרשימה שנחזיר בסוף
            LiDarDataBase.getInstance(null).add1();// מעדכנות שמצאנו עוד אוביקט לטובת מעקב אחר האם נגמר על מה לעקוב
            StatisticalFolder.getInstance().addToNumTrackedObjects(1); // מוסיפות לSTAT FOLDER שמצאנו עוד אוביקט
        }
        System.out.println("size in Worker "+ output.size());
        return output;
    }

    // ====================================================================================================================
    public int getID() {
        return this.id;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public STATUS geStatus() {
        return this.status;
    }

    public TrackedObject getLast() {
        return lastTrackedObjects.get(lastTrackedObjects.size() - 1);
    }

}

package bgu.spl.mics.application.objects;

import java.util.ArrayList;


/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */

public class Camera {

    private int id;
    private int frequency;
    private STATUS status;
    private ArrayList<StampedDetectedObjects> detectedObjectsList;
    private int currentIndex; // שדה כדי לעקוב אחרי המיקום ברשימה
    private StampedDetectedObjects last;

    /**
     * @pre id > 0
     * @pre frequency >= 0
     * @pre detectedObjectsList != null
     */

    public Camera(int id, int frequency, ArrayList<StampedDetectedObjects> detectedObjectsList) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjectsList = detectedObjectsList;
        this.currentIndex = 0;
    }

    /**
     * Retrieves the next detected object at the given time.
     * If no object matches the time or the camera has completed its detections,
     * the camera may transition to a "DOWN" or "ERROR" state.
     *
     * @param time the current timestamp to check for detections
     * @return the next detected object at the given time, or null if no match is
     *         found
     *
     * @pre time >= 0
     * @post getStatus() == STATUS.DOWN if no more detections are available
     * @post getStatus() == STATUS.ERROR if an "ERROR" object is detected
     */
    public StampedDetectedObjects next(int time) {
        if (currentIndex >= detectedObjectsList.size()) {
            this.status = STATUS.DOWN;
            return null;
        } else {
            if (detectedObjectsList.get(currentIndex).getTime() == time) {
                this.currentIndex++;
                StampedDetectedObjects curr = detectedObjectsList.get(currentIndex - 1);
                for (DetectedObject d : curr.getDetectedObjects()) {
                    if (d.getId().equals("ERROR")) {
                        StatisticalFolder.getInstance().setError(d.getDescription(), "Camera" + this.id);
                        StatisticalFolder.getInstance().addToLastDetected(this.id, curr);
                        StatisticalFolder.getInstance().setIfThereIsError(true);
                        this.status = STATUS.ERROR;
                        return null;
                    }
                }
                this.last = curr;
                return curr;
            } else {
                return null;
            }
        }
    }

    public STATUS getStatus() {
        return this.status;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public int getId() {
        return this.id;
    }

    public StampedDetectedObjects getLastDes(){
        return this.last;
    }

}

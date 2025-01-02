package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping
 * (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update
 * a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam
 * exists.
 */
public class FusionSlam {

    private HashMap<String, LandMark> landmarks; // שינוי לרשימת HashMap
    private ArrayList<Pose> Poses;

    public FusionSlam() {
        this.landmarks = new HashMap<>(); // אתחול ה-HashMap
        this.Poses = new ArrayList<Pose>();
    }

    // Singleton instance holder
    private static class FusionSlamHolder {
        private static final FusionSlam instance = new FusionSlam();
    }

    public HashMap<String, LandMark> getLandmarks() {
        return landmarks;
    }

    public static FusionSlam getInstance() {
        return FusionSlamHolder.instance;
    }

    public void addPose(Pose pose) {
        this.Poses.add(pose);
    }

    /**
     * Retrieves a pose by its index.
     * 
     * @param index the index of the pose to retrieve
     * @return the pose at the specified index, or null if the index is out of
     *         bounds
     * @pre index >= 0
     */
    public Pose getPose(int index) {
        if (index >= this.Poses.size()) {
            return null;
        }
        return this.Poses.get(index);
    }

    /**
     * Adds or updates a landmark based on its ID.
     * If the landmark does not exist, it is created. If it exists, its coordinates
     * are updated.
     * 
     * @param coordinates the coordinates of the landmark
     * @param id          the unique ID of the landmark
     * @param description the description of the landmark
     * @return the updated or newly created landmark
     * 
     * @pre coordinates != null
     * @pre id != null
     * @pre description != null
     * @post landmarks.containsKey(id)
     */
    // ממוצע אם כבר קיים או מוסיף אם חדש
    public LandMark addOavLandMark(ArrayList<CloudPoint> coordinates, String id, String description) {
        LandMark l = landmarks.get(id);
        if (l == null) {
            LandMark output = new LandMark(id, description, coordinates);
            StatisticalFolder.getInstance().addToNumLandmarks(1);
            this.landmarks.put(id, output);
            StatisticalFolder.getInstance().addToNumLandmarks(1);
            return output;
        } else {
            l.setCoordinates(coordinates);
            return l;
        }
    }
    
    /**
     * Transforms tracked objects into landmarks using the given pose.
     * Updates the global map by adding new landmarks or updating existing ones.
     * 
     * @param trackedObjects the list of tracked objects to process
     * @param pose           the pose used for transforming the objects
     * @return a list of updated or newly created landmarks
     * 
     * @pre trackedObjects != null
     * @pre pose != null
     * @post addOrChangeLM(trackedObjects, pose).size() == trackedObjects.size()
     */
    public ArrayList<LandMark> addOrChangeLM(ArrayList<TrackedObject> listy, Pose posy) {
        ArrayList<LandMark> landMark = new ArrayList<>();
        for (TrackedObject o : listy) {
            ArrayList<CloudPoint> updatedCoordinates = new ArrayList<>();
            for (CloudPoint point : o.getCoordinates()) {
                // חישוב המיקום המוסב לכל נקודה
                double newX = posy.getX() +
                        point.getX() * Math.cos(Math.toRadians(posy.getYaw())) -
                        point.getY() * Math.sin(Math.toRadians(posy.getYaw()));

                double newY = posy.getY() +
                        point.getX() * Math.sin(Math.toRadians(posy.getYaw())) +
                        point.getY() * Math.cos(Math.toRadians(posy.getYaw()));

                // יצירת CloudPoint 
                updatedCoordinates.add(new CloudPoint(newX, newY, point.getZ()));
                // עדכון הפיוז'ן סלאם עם ה-TrackedObject
            }
            LandMark landMarkOutput = this.addOavLandMark(updatedCoordinates, o.getId(), o.getDescription());
            landMark.add(landMarkOutput);
        }
        return landMark;
    }
}

package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticalFolder {
    // בסוף כל סיום תכנית

    // מתעדכן בזמן ריצה
    private AtomicInteger systemRuntime;
    private AtomicInteger numDetectedObjects;
    private AtomicInteger numTrackedObjects;
    private AtomicInteger numLandmarks;
    // מתעדכן בסוף
    private HashMap<String, LandMark> landmarks;

    // רק למקרה של שגיאות
    private boolean error;

    private String errorDescription;
    private String faultySensor;
    // הדברים האחרונים של הסנסורים
    private Map<Integer, StampedDetectedObjects> lastDetectedByCamera;
    private Map<Integer, List<TrackedObject>> lastTrackedByLidar;
    private ArrayList<Pose> Poses;

    private StatisticalFolder() {
        this.systemRuntime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
        this.errorDescription = null;
        this.faultySensor = null;
        this.lastDetectedByCamera = new HashMap<>();
        this.lastTrackedByLidar = new HashMap<>();
        this.landmarks = new HashMap<>();
        this.Poses = new ArrayList<Pose>();
        this.error = false;
    }

    private static class StatisticalFolderHolder {
        private static final StatisticalFolder instance = new StatisticalFolder();
    }

    public static StatisticalFolder getInstance() {
        return StatisticalFolderHolder.instance;
    }

    // Setters and Incrementers
    public void setError(String errorDescription, String faultySensor) {
        System.out.println("Error: " + errorDescription);
        this.errorDescription = errorDescription;
        this.faultySensor = faultySensor;
    }

    public void addToSystemRuntime(int runtime) {
        System.out.println("Runtime: " + runtime);
        this.systemRuntime.addAndGet(runtime);
    }

    public void addToNumDetectedObjects(int count) {
        System.out.println("Detected: " + count);
        this.numDetectedObjects.addAndGet(count);
    }

    public void addToNumTrackedObjects(int count) {
        System.out.println("Tracked: " + count);
        this.numTrackedObjects.addAndGet(count);
    }

    public void addToNumLandmarks(int count) {
        System.out.println("Landmarks: " + count);
        this.numLandmarks.addAndGet(count);
    }

    public void setLastTrackedByLidar(Map<Integer, List<TrackedObject>> trackedByLidar) {
        System.out.println("Updating Tracked by Lidar");
        this.lastTrackedByLidar = trackedByLidar;
    }

    public void setLandmarks(HashMap<String, LandMark> landmarks) {
        System.out.println("Updating Landmarks");
        this.landmarks = landmarks;
    }

    public void setPoses(ArrayList<Pose> poses) {
        System.out.println("Updating Poses");
        this.Poses = poses;
    }

    public void addToLastDetected(int cameraId, StampedDetectedObjects detected) {
        System.out.println("Adding Detected by Camera");
        lastDetectedByCamera.putIfAbsent(cameraId, detected);
    }

    public void addToLastTracked(int lidarId, TrackedObject tracked) {
        System.out.println("Adding Tracked by Lidar");
        lastTrackedByLidar.putIfAbsent(lidarId, new ArrayList<>());
        lastTrackedByLidar.get(lidarId).add(tracked);
    }

    public void setIfThereIsError(boolean error) {
        System.out.println("Error: " + error);
        this.error = error;
    }

    // Getters
    public int getSystemRuntime() {
        return this.systemRuntime.get();
    }

    public int getNumDetectedObjects() {
        return this.numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return this.numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return this.numLandmarks.get();
    }

    public String getErrorDescription() {
        return this.errorDescription;
    }

    public String getFaultySensor() {
        return this.faultySensor;
    }

    public boolean isError() {
        return this.error;
    }

    public Map<Integer, StampedDetectedObjects> getLastDetectedByCamera() {
        return this.lastDetectedByCamera;
    }

    public Map<Integer, List<TrackedObject>> getLastTrackedByLidar() {
        return this.lastTrackedByLidar;
    }

    public HashMap<String, LandMark> getLandmarks() {
        return this.landmarks;
    }

    public ArrayList<Pose> getPoses() {
        return this.Poses;
    }

    // JSON Output
    public void generateOutputFile(String outputPath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(outputPath)) {
            if (this.error) {
                Map<String, Object> errorOutput = new HashMap<>();
                errorOutput.put("Error", this.errorDescription);
                errorOutput.put("faultySensor", this.faultySensor);
                errorOutput.put("lastFrames", Map.of(
                        "cameras", this.lastDetectedByCamera,
                        "lidar", this.lastTrackedByLidar));
                errorOutput.put("poses", this.Poses);
                errorOutput.put("statistics", Map.of(
                        "systemRuntime", this.systemRuntime.get(),
                        "numDetectedObjects", this.numDetectedObjects.get(),
                        "numTrackedObjects", this.numTrackedObjects.get(),
                        "numLandmarks", this.numLandmarks.get()));
                gson.toJson(errorOutput, writer);
            } else {
                Map<String, Object> successOutput = new HashMap<>();
                successOutput.put("statistics", Map.of(
                        "systemRuntime", this.systemRuntime.get(),
                        "numDetectedObjects", this.numDetectedObjects.get(),
                        "numTrackedObjects", this.numTrackedObjects.get(),
                        "numLandmarks", this.numLandmarks.get()));
                successOutput.put("landMarks", this.landmarks);
                gson.toJson(successOutput, writer);
            }
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
        }
    }
}

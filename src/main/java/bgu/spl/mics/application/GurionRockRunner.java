package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GurionRockRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GurionRockRunner <path-to-configuration-file>");
            return;
        }

        String configFilePath = args[0];
        Path configDirectory = Paths.get(configFilePath).getParent();
        String outputFilePath = "output_file.json";

        try {
            Gson gson = new Gson();

            // קריאת קובץ הקונפיגורציה
            Type configType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> config = gson.fromJson(new FileReader(configFilePath), configType);

            String cameraDataPath = (String) ((Map<String, Object>) config.get("Cameras")).get("camera_datas_path");
            String poseDataPath = (String) config.get("poseJsonFile");
            String lidarDataPath = (String) ((Map<String, Object>) config.get("LiDarWorkers")).get("lidars_data_path");

            Path cameraDataAbsolutePath = configDirectory.resolve(cameraDataPath).toAbsolutePath();
            Path poseDataAbsolutePath = configDirectory.resolve(poseDataPath).toAbsolutePath();
            Path lidarDataAbsolutePath = configDirectory.resolve(lidarDataPath).toAbsolutePath();

            // אתחול מאגר ה-LiDAR
            LiDarDataBase.getInstance(lidarDataAbsolutePath.toString());

            // קריאת נתוני מצלמות מתוך קובץ JSON
            Type cameraDataType = new TypeToken<Map<String, List<Map<String, Object>>>>() {
            }.getType();
            Map<String, List<Map<String, Object>>> cameraData = gson
                    .fromJson(new FileReader(cameraDataAbsolutePath.toString()), cameraDataType);

            ArrayList<Camera> cameras = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : cameraData.entrySet()) {
                String cameraId = entry.getKey();
                List<Map<String, Object>> detectedObjects = entry.getValue();

                ArrayList<StampedDetectedObjects> stampedObjectsList = new ArrayList<>();
                int stampedObjectId = 1; // Counter for unique IDs
                for (Map<String, Object> objData : detectedObjects) {
                    int time = ((Double) objData.get("time")).intValue();
                    List<Map<String, String>> objects = (List<Map<String, String>>) objData.get("detectedObjects");

                    ArrayList<DetectedObject> detectedObjectList = new ArrayList<>();
                    for (Map<String, String> obj : objects) {
                        String id = obj.get("id");
                        String description = obj.get("description");
                        detectedObjectList.add(new DetectedObject(id, description));
                    }

                    stampedObjectsList.add(new StampedDetectedObjects(time, stampedObjectId++, detectedObjectList));
                }

                int frequency = ((Double) ((List<Map<String, Object>>) ((Map<String, Object>) config.get("Cameras"))
                        .get("CamerasConfigurations")).stream()
                        .filter(cam -> ((Double) cam.get("id")).intValue() == Integer.parseInt(cameraId.replace("camera", "")))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Camera not found"))
                        .get("frequency")).intValue();

                Camera camera = new Camera(Integer.parseInt(cameraId.replace("camera", "")), frequency, stampedObjectsList);
                cameras.add(camera);
            }

            // קריאת נתוני פוזות
            Type poseDataType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> poseData = gson.fromJson(new FileReader(poseDataAbsolutePath.toString()),
                    poseDataType);

            ArrayList<Pose> poses = new ArrayList<>();
            for (Map<String, Object> poseEntry : poseData) {
                int time = ((Double) poseEntry.get("time")).intValue();
                float x = ((Double) poseEntry.get("x")).floatValue();
                float y = ((Double) poseEntry.get("y")).floatValue();
                float yaw = ((Double) poseEntry.get("yaw")).floatValue();

                poses.add(new Pose(x, y, yaw, time));
            }
            GPSIMU gpsimu = new GPSIMU(poses);

            // קריאת נתוני LIDAR
            ArrayList<Map<String, Object>> lidarConfigs = (ArrayList<Map<String, Object>>) ((Map<String, Object>) config
                    .get("LiDarWorkers")).get("LidarConfigurations");
            ArrayList<LiDarWorkerTracker> lidars = new ArrayList<>();
            for (Map<String, Object> lidarConfig : lidarConfigs) {
                int lidarId = ((Double) lidarConfig.get("id")).intValue();
                int frequency = ((Double) lidarConfig.get("frequency")).intValue();
                LiDarWorkerTracker worker = new LiDarWorkerTracker(lidarId, frequency);
                lidars.add(worker);
            }

            System.out.println("Simulation initialized successfully.");

            // CountDownLatch אתחול
            int totalServices = cameras.size() + lidars.size() + 3; // כולל PoseService, FusionSlamService,
                                                                    // ו-TimeService
            CountDownLatch latch = new CountDownLatch(totalServices);

            // אתחול חוטים למצלמות, LiDARים ופוזות
            List<Thread> serviceThreads = new ArrayList<>();
            for (Camera camera : cameras) {
                Thread thread = new Thread(() -> {
                    new CameraService(camera, latch).run();
                });
                serviceThreads.add(thread);
                thread.start();
            }
            for (LiDarWorkerTracker lidar : lidars) {
                Thread thread = new Thread(() -> {
                    new LiDarService(lidar, latch).run();
                });
                serviceThreads.add(thread);
                thread.start();
            }
            Thread poseThread = new Thread(() -> {
                new PoseService(gpsimu, latch).run();
            });
            serviceThreads.add(poseThread);
            poseThread.start();

            // אתחול FusionSlamService
            FusionSlam fusionSlamInstance = FusionSlam.getInstance();
            int aliveSensorCount = cameras.size() + lidars.size() + 1; // CameraServices + LiDarServices + PoseService
            Thread fusionSlamThread = new Thread(() -> {
                new FusionSlamService(fusionSlamInstance, aliveSensorCount, latch).run();
            });
            serviceThreads.add(fusionSlamThread);
            fusionSlamThread.start();

            // אתחול TimeService
            int tickTime = ((Double) config.get("TickTime")).intValue();
            int duration = ((Double) config.get("Duration")).intValue();
            Thread timeServiceThread = new Thread(() -> {
                new TimeService(tickTime, duration, latch).run();
            });
            serviceThreads.add(timeServiceThread);
            timeServiceThread.start();

            // המתנה לסיום כל החוטים
            for (Thread thread : serviceThreads) {
                thread.join();
            }

            // כתיבת פלט אחרי סיום הסימולציה
            StatisticalFolder.getInstance().generateOutputFile(outputFilePath);
            System.out.println("Simulation completed. Output written to: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("Error reading JSON files: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Simulation interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
        }
    }
}

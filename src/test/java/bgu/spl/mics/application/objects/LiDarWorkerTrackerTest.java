// package bgu.spl.mics.application.objects;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import java.util.ArrayList;

// import static org.junit.jupiter.api.Assertions.*;

// public class LiDarWorkerTrackerTest {

//     private LiDarWorkerTracker lidarWorker;
//     private ArrayList<TrackedObject> lastTrackedObjects;
//     private LiDarDataBase lidarDatabase;

//     @BeforeEach
//     public void setUp() {
//         lastTrackedObjects = new ArrayList<>();
//         lidarWorker = new LiDarWorkerTracker(1, 10, lastTrackedObjects);
//         lidarDatabase = LiDarDataBase.getInstance(null);
//     }

//     @Test
//     public void testGetTrackedObjects_AllProcessed() {
//         // Mock LiDarDataBase to simulate all objects processed
//         lidarDatabase.add1();
//         lidarDatabase.add1();

//         ArrayList<StampedCloudPoints> cloudPointsData = new ArrayList<>();
//         cloudPointsData.add(new StampedCloudPoints("obj1", 0, new ArrayList<>()));

//         assertTrue(lidarDatabase.isDONE(), "LiDarDataBase should indicate all objects are processed.");

//         // Call method
//         ArrayList<TrackedObject> trackedObjects = lidarWorker.getTrackedObjects(10);

//         // Verify output
//         assertNull(trackedObjects, "Tracked objects should be null if all objects are processed.");
//         assertEquals(STATUS.DOWN, lidarWorker.geStatus(), "Status should be DOWN if all objects are processed.");
//     }

//     @Test
//     public void testGetTrackedObjects_WithError() {
//         // Simulate error in LiDarDataBase
//         lidarDatabase.add1();
//         lidarDatabase.add1();

//         ArrayList<StampedCloudPoints> cloudPointsData = new ArrayList<>();
//         cloudPointsData.add(new StampedCloudPoints("ERROR", 0, new ArrayList<>()));

//         assertTrue(lidarDatabase.findError(0), "LiDarDataBase should indicate an error.");

//         // Call method
//         ArrayList<TrackedObject> trackedObjects = lidarWorker.getTrackedObjects(10);

//         // Verify output
//         assertNull(trackedObjects, "Tracked objects should be null if there is an error in LiDarDataBase.");
//         assertEquals(STATUS.ERROR, lidarWorker.geStatus(), "Status should be ERROR if an error is found in LiDarDataBase.");
//     }

//     @Test
//     public void testGetTrackedObjects_ValidData() {
//         // Add valid data to LiDarDataBase
//         ArrayList<CloudPoint> cloudPoints = new ArrayList<>();
//         cloudPoints.add(new CloudPoint(1.0, 1.0, 1.0));
//         cloudPoints.add(new CloudPoint(2.0, 2.0));

//         StampedCloudPoints stampedCloudPoints = new StampedCloudPoints("obj1", 0, cloudPoints);
//         lidarDatabase.add1();

//         StampedDetectedObjects stampedDetectedObjects = new StampedDetectedObjects(0, new ArrayList<>());
//         stampedDetectedObjects.getDetectedObjects().add(new DetectedObject("obj1", "description1"));
//         lidarWorker.addStampedObjects(0, stampedDetectedObjects);

//         // Call method
//         ArrayList<TrackedObject> trackedObjects = lidarWorker.getTrackedObjects(10);

//         // Verify output
//         assertNotNull(trackedObjects, "Tracked objects should not be null for valid data.");
//         assertEquals(1, trackedObjects.size(), "Tracked objects size should be 1 for valid data.");
//         assertEquals("obj1", trackedObjects.get(0).getId(), "First tracked object ID should match.");
//         assertEquals("description1", trackedObjects.get(0).getDescription(), "First tracked object description should match.");
//     }
// }

package bgu.spl.mics.application.objects;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

public class CameraTest {

    private Camera camera;
    private ArrayList<StampedDetectedObjects> detectedObjectsList;

    @BeforeEach
    void setUp() {
        detectedObjectsList = new ArrayList<>();

        // Add sample detected objects
        ArrayList<DetectedObject> objectsAtTime1 = new ArrayList<>();
        objectsAtTime1.add(new DetectedObject("obj1", "description1"));

        ArrayList<DetectedObject> objectsAtTime2 = new ArrayList<>();
        objectsAtTime2.add(new DetectedObject("obj2", "description2"));

        detectedObjectsList.add(new StampedDetectedObjects(1, objectsAtTime1));
        detectedObjectsList.add(new StampedDetectedObjects(2, objectsAtTime2));

        camera = new Camera(1, 2, detectedObjectsList);
    }

    @Test
    void testNextReturnsCorrectObject() {
        // Test the first time step
        StampedDetectedObjects result = camera.next(1);
        assertNotNull(result, "The result should not be null.");
        assertEquals(1, result.getTime(), "The time should match the requested time.");
        assertEquals(1, result.getDetectedObjects().size(), "There should be one detected object.");
        assertEquals("obj1", result.getDetectedObjects().get(0).getId(), "The ID of the object should match.");

        // Test the second time step
        result = camera.next(2);
        assertNotNull(result, "The result should not be null.");
        assertEquals(2, result.getTime(), "The time should match the requested time.");
        assertEquals(1, result.getDetectedObjects().size(), "There should be one detected object.");
        assertEquals("obj2", result.getDetectedObjects().get(0).getId(), "The ID of the object should match.");
    }

    @Test
    void testNextWithNoMoreDetections() {
        // Exhaust all detections
        camera.next(1);
        camera.next(2);

        // Call next again
        StampedDetectedObjects result = camera.next(3);
        assertNull(result, "The result should be null when no more detections are available.");
        assertEquals(STATUS.DOWN, camera.getStatus(), "The camera status should be DOWN.");
    }

    @Test
    void testNextDetectsError() {
        // Add an ERROR object to the list
        ArrayList<DetectedObject> errorObjects = new ArrayList<>();
        errorObjects.add(new DetectedObject("ERROR", "description"));
        detectedObjectsList.add(new StampedDetectedObjects(3, errorObjects));

        // Check for error detection
        camera.next(1);
        camera.next(2);
        StampedDetectedObjects result = camera.next(3);
        assertNull(result, "The result should be null when an ERROR is detected.");
        assertEquals(STATUS.ERROR, camera.getStatus(), "The camera status should be ERROR.");
    }

    @Test
    void testNextWithNoMatchingTime() {
        // Request a time that doesn't exist
        StampedDetectedObjects result = camera.next(99);
        assertNull(result, "The result should be null when no matching time is found.");
        assertEquals(STATUS.UP, camera.getStatus(), "The camera status should remain UP.");
    }

    @Test
    void testCameraInitialization() {
        assertEquals(1, camera.getId(), "Camera ID should be initialized correctly.");
        assertEquals(2, camera.getFrequency(), "Camera frequency should be initialized correctly.");
        assertEquals(STATUS.UP, camera.getStatus(), "Camera status should be UP upon initialization.");
    }
}

package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class FusionSlamTest {

    private FusionSlam fusionSlam;
    private Pose pose;
    private ArrayList<TrackedObject> trackedObjects;


    @Test
    public void testAddOrChangeLM_NewLandmarks() {
        fusionSlam = FusionSlam.getInstance();
        pose = new Pose(0.0f, 0.0f, 0.0f, 0);

        
        // יצירת רשימה של אובייקטים למעקב
        trackedObjects = new ArrayList<>();

        ArrayList<CloudPoint> points1 = new ArrayList<>();
        points1.add(new CloudPoint(1.0, 1.0, 1.0));
        points1.add(new CloudPoint(2.0, 2.0, 1.0));

        ArrayList<CloudPoint> points2 = new ArrayList<>();
        points2.add(new CloudPoint(-1.0, -1.0, 1.0));
        points2.add(new CloudPoint(-2.0, -2.0, 1.0));

        trackedObjects.add(new TrackedObject("obj1", 0, "description1", points1));
        trackedObjects.add(new TrackedObject("obj2", 0, "description2", points2));
        // בודק אם הפונקציה יוצרת נכון Landmarks עבור אובייקטים חדשים
        ArrayList<LandMark> landmarks = fusionSlam.addOrChangeLM(trackedObjects, pose);

        assertEquals(2, landmarks.size());
        LandMark lm1 = landmarks.get(0);
        LandMark lm2 = landmarks.get(1);

        assertEquals("obj1", lm1.getId());
        assertEquals("obj2", lm2.getId());
        assertEquals("description1", lm1.getDescripiot());
        assertEquals("description2", lm2.getDescripiot());

    }

    @Test
    public void testAddOrChangeLM_UpdateExistingLandmarks() {
        fusionSlam = FusionSlam.getInstance();
        pose = new Pose(0.0f, 0.0f, 0.0f, 0);

        
        // יצירת רשימה של אובייקטים למעקב
        trackedObjects = new ArrayList<>();

        ArrayList<CloudPoint> points1 = new ArrayList<>();
        points1.add(new CloudPoint(1.0, 1.0, 1.0));
        points1.add(new CloudPoint(2.0, 2.0, 1.0));

        ArrayList<CloudPoint> points2 = new ArrayList<>();
        points2.add(new CloudPoint(-1.0, -1.0, 1.0));
        points2.add(new CloudPoint(-2.0, -2.0, 1.0));

        trackedObjects.add(new TrackedObject("obj1", 0, "description1", points1));
        trackedObjects.add(new TrackedObject("obj2", 0, "description2", points2));
        // הוספה ראשונה
        fusionSlam.addOrChangeLM(trackedObjects, pose);

        // יצירת קורדינטות חדשות לעדכון
        ArrayList<CloudPoint> newPoints = new ArrayList<>();
        newPoints.add(new CloudPoint(3.0, 3.0, 1.0));

        ArrayList<TrackedObject> updatedObjects = new ArrayList<>();
        updatedObjects.add(new TrackedObject("obj1", 0, "description1", newPoints));

        // עדכון
        ArrayList<LandMark> updatedLandmarks = fusionSlam.addOrChangeLM(updatedObjects, pose);

        assertEquals(1, updatedLandmarks.size());
        LandMark updatedLandmark = updatedLandmarks.get(0);

        assertEquals("obj1", updatedLandmark.getId());
        assertEquals(1.96875, updatedLandmark.getCoordinates().get(0).getX());
        assertEquals(2.21875, updatedLandmark.getCoordinates().get(0).getY());
    }

    @Test
    public void testAddOrChangeLM_WithTransformedCoordinates() {
        fusionSlam = FusionSlam.getInstance();
        pose = new Pose(0.0f, 0.0f, 0.0f, 0);

        
        // יצירת רשימה של אובייקטים למעקב
        trackedObjects = new ArrayList<>();

        ArrayList<CloudPoint> points1 = new ArrayList<>();
        points1.add(new CloudPoint(1.0, 1.0, 1.0));
        points1.add(new CloudPoint(2.0, 2.0, 1.0));

        ArrayList<CloudPoint> points2 = new ArrayList<>();
        points2.add(new CloudPoint(-1.0, -1.0, 1.0));
        points2.add(new CloudPoint(-2.0, -2.0, 1.0));

        trackedObjects.add(new TrackedObject("obj1", 0, "description1", points1));
        trackedObjects.add(new TrackedObject("obj2", 0, "description2", points2));
        // שימוש בפוז עם YAW לצורך בדיקת טרנספורמציה
        Pose transformedPose = new Pose(1.0f, 1.0f, 90.0f, 0);

        ArrayList<LandMark> landmarks = fusionSlam.addOrChangeLM(trackedObjects, transformedPose);

        assertEquals(2, landmarks.size());
        CloudPoint transformedPoint = landmarks.get(0).getCoordinates().get(0);

        // בדיקה אם הקורדינטות עברו טרנספורמציה נכון
        assertEquals(0.875, transformedPoint.getX(), 0.01);
        assertEquals(1.875, transformedPoint.getY(), 0.01);
    }

    @Test
    public void testAddOrChangeLM_DifferentLengthCoordinates() {
        fusionSlam = FusionSlam.getInstance();
        pose = new Pose(0.0f, 0.0f, 0.0f, 0);

        
        // יצירת רשימה של אובייקטים למעקב
        trackedObjects = new ArrayList<>();

        ArrayList<CloudPoint> points1 = new ArrayList<>();
        points1.add(new CloudPoint(1.0, 1.0, 1.0));
        points1.add(new CloudPoint(2.0, 2.0, 1.0));

        ArrayList<CloudPoint> points2 = new ArrayList<>();
        points2.add(new CloudPoint(-1.0, -1.0, 1.0));
        points2.add(new CloudPoint(-2.0, -2.0, 1.0));

        trackedObjects.add(new TrackedObject("obj1", 0, "description1", points1));
        trackedObjects.add(new TrackedObject("obj2", 0, "description2", points2));
        // יצירת רשימה ראשונה (ארוכה יותר)
        ArrayList<CloudPoint> initialPoints = new ArrayList<>();
        initialPoints.add(new CloudPoint(1.0, 1.0, 1.0));
        initialPoints.add(new CloudPoint(2.0, 2.0, 1.0));
        initialPoints.add(new CloudPoint(3.0, 3.0, 1.0)); // נקודה נוספת

        // יצירת רשימה שנייה (קצרה יותר)
        ArrayList<CloudPoint> newPoints = new ArrayList<>();
        newPoints.add(new CloudPoint(4.0, 4.0, 1.0));
        newPoints.add(new CloudPoint(5.0, 5.0, 1.0));

        // יצירת TrackedObject
        ArrayList<TrackedObject> trackedObjects = new ArrayList<>();
        trackedObjects.add(new TrackedObject("obj1", 0, "description1", initialPoints));

        // הוספה ראשונית
        fusionSlam.addOrChangeLM(trackedObjects, pose);

        // יצירת TrackedObject עם רשימה קצרה יותר לעדכון
        ArrayList<TrackedObject> updatedObjects = new ArrayList<>();
        updatedObjects.add(new TrackedObject("obj1", 0, "description1", newPoints));

        // עדכון
        ArrayList<LandMark> updatedLandmarks = fusionSlam.addOrChangeLM(updatedObjects, pose);

        // בדיקה
        assertEquals(1, updatedLandmarks.size());
        LandMark updatedLandmark = updatedLandmarks.get(0);

        // בדיקת נקודות מעודכנות
        assertEquals(3, updatedLandmark.getCoordinates().size()); // אורך הרשימה נשאר 3

        // ממוצע של נקודות משותפות
        CloudPoint averagedPoint1 = updatedLandmark.getCoordinates().get(0);
        assertEquals(2.5, averagedPoint1.getX());
        assertEquals(2.5, averagedPoint1.getY());

        CloudPoint averagedPoint2 = updatedLandmark.getCoordinates().get(1);
        assertEquals(3.5, averagedPoint2.getX());
        assertEquals(3.5, averagedPoint2.getY());

        // הנקודה השלישית נשארת כפי שהיא
        CloudPoint unchangedPoint = updatedLandmark.getCoordinates().get(2);
        assertEquals(3.0, unchangedPoint.getX());
        assertEquals(3.0, unchangedPoint.getY());
    }
}

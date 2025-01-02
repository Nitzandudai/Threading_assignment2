package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
private String id;
private String description;
private ArrayList<CloudPoint> coordinates;

public LandMark (String id , String description , ArrayList<CloudPoint> coordinates){
    this.id = id;
    this. description = description;
    this.coordinates = coordinates;
    }


public void setCoordinates(ArrayList<CloudPoint> newCoordinates) {
    if (coordinates == null || coordinates.isEmpty()) {
        // אם זה אובייקט חדש, פשוט נעדכן את הקורדינטות החדשות
        coordinates = new ArrayList<>(newCoordinates);
        return;
    }

    // חישוב ממוצע של הקורדינטות הישנות והחדשות
    int minSize = Math.min(coordinates.size(), newCoordinates.size());

    for (int i = 0; i < minSize; i++) {
        CloudPoint oldPoint = coordinates.get(i);
        CloudPoint newPoint = newCoordinates.get(i);

        double avgX = (oldPoint.getX() + newPoint.getX()) / 2.0;
        double avgY = (oldPoint.getY() + newPoint.getY()) / 2.0;

        // עדכון הקורדינטה הנוכחית
        oldPoint.setX(avgX);
        oldPoint.setY(avgY);
    }

    // אם הרשימה החדשה ארוכה יותר, נוסיף את הנקודות הנותרות כמו שהן
    for (int i = minSize; i < newCoordinates.size(); i++) {
        coordinates.add(newCoordinates.get(i));
    }
    }

    public String getId(){
        return this.id;
    }

    public String getDescripiot(){
        return this.description;
    }

    public ArrayList<CloudPoint> getCoordinates(){
        return this.coordinates;
    }
}




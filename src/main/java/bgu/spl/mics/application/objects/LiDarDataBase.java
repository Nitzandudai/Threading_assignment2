package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    private ConcurrentHashMap<CompositeKey, StampedCloudPoints> cloudPointsMap;
    private AtomicInteger allTracked;
    private int currTime;

//====================================================================================================================

    public LiDarDataBase (){
        this.cloudPointsMap = new ConcurrentHashMap<>();
        this.allTracked = new AtomicInteger(0); //הפכנו לאטומיק כדי למנוע שני ת'רדים שעושים את הפעולה באותו זמן 
        this.currTime = 0;
    }

//====================================================================================================================
    /**
     * Checks if all tracked objects have been processed.
     * 
     * @return true if all objects are tracked, false otherwise.
     */
    public boolean isDONE(){
        return allTracked.get() >= cloudPointsMap.size();
    }

//====================================================================================================================
    /**
     * Searches for errors in the LiDAR data for a given time.
     * If an "ERROR" ID is found in the data at or before the given time, returns true.
     * 
     * @param time the current time to check for errors.
     * @return true if an error is found, false otherwise.
     * 
     * @pre time >= 0
     * @post currTime == max(currTime, time)
     */
    public synchronized boolean findError(int time) {
        if (time <= currTime) {//אין צורך לבדוק שוב על אותו זמן
            return false;
        }
        currTime = time;// מעדכנים את הזמן הנוכחי לזמן שנבדק

        for (StampedCloudPoints stamped : cloudPointsMap.values()) {
            if (stamped.getTime() <= time && "ERROR".equals(stamped.getID())) {
                return true;
            }
        }
        return false; 
    }
//====================================================================================================================
    /**
     * Increments the count of tracked objects.
     */
    public void add1() {
        allTracked.incrementAndGet();
    }
//====================================================================================================================
    /**
     * Finds the list of cloud points for a specific ID and time.
     * 
     * @param id   the ID of the object to find.
     * @param time the timestamp of the object.
     * @return the list of cloud points for the given ID and time, or null if not found.
     * 
     * @pre id != null
     * @pre time >= 0
     * @post output can't be null
     */
    public ArrayList<CloudPoint> findPoints(String id, int time) {
        CompositeKey key = new CompositeKey(id, time);
        StampedCloudPoints stamped = cloudPointsMap.get(key);
        if(stamped == null){
            return null;
        }
        else{
            return stamped.getList();
        }
    }
//====================================================================================================================

    // Singleton instance holder
    private static class LiDarDataBaseHolder {
        private static LiDarDataBase instance = new LiDarDataBase();
    }
//====================================================================================================================

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    @SuppressWarnings("unchecked")
    public static LiDarDataBase getInstance(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
                FileReader reader = new FileReader(filePath);
    
                ArrayList<Map<String, Object>> data = gson.fromJson(reader, listType);
                reader.close();
    
                LiDarDataBase instance = LiDarDataBaseHolder.instance;
    
                // Populate the map with data
                for (Map<String, Object> entry : data) {
                    int time = ((Double) entry.get("time")).intValue();
                    String id = (String) entry.get("id");
                    ArrayList<ArrayList<Double>> cloudPointsRaw = (ArrayList<ArrayList<Double>>) entry.get("cloudPoints");
    
                    ArrayList<CloudPoint> cloudPoints = new ArrayList<>();
                    for (ArrayList<Double> point : cloudPointsRaw) {
                        double x = point.get(0);
                        double y = point.get(1);
                        double z = point.get(2);
                        cloudPoints.add(new CloudPoint(x, y, z));
                    }
    
                    StampedCloudPoints stamped = new StampedCloudPoints(id, time, cloudPoints);
                    CompositeKey key = new CompositeKey(id, time);
                    instance.cloudPointsMap.put(key, stamped);
                }
                return instance;
    
            } catch (IOException e) {
                System.err.println("Error reading JSON file: " + e.getMessage());
                e.printStackTrace();
            } catch (ClassCastException | NullPointerException e) {
                System.err.println("Error parsing JSON data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        return LiDarDataBaseHolder.instance;
    }
    
//====================================================================================================================

     // Composite key for map
     private static class CompositeKey {
        private final String id;
        private final int time;

        public CompositeKey(String id, int time) {
            this.id = id;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompositeKey that = (CompositeKey) o;
            return time == that.time && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + time;
        }
    }

}

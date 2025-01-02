package bgu.spl.mics.application.objects;

import java.util.ArrayList;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick;
    private STATUS status;
    private ArrayList<Pose> PoseList;

    public GPSIMU (ArrayList<Pose> PoseList){
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.PoseList = PoseList;
    }

    public ArrayList<Pose> getPoseList(int time){
        ArrayList<Pose> output = new ArrayList<Pose>();
        for(int i = 0; i<time; i++){
            output.add(this.PoseList.get(i));
        }
        return output;
    }

    public Pose getCurrPose (){ // מחזיר את הפוז לפי המיקום במערך כל פעם שנבקש יתקדם בטיק אחד כי נבקש רק אחרי כל טיים טיק
        if(currentTick>=PoseList.size()){
            this.status = STATUS.DOWN;
            return null;
        }
        else{
            Pose output = this.PoseList.get(currentTick);
            this.currentTick ++ ;
            return output;
        }
    }

    public STATUS geStatus(){
        return this.status;
    }
}
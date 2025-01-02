package bgu.spl.mics.application.objects;


/**
 * CloudPoint represents a specific point in a 3D space as detected by the LiDAR.
 * These points are used to generate a point cloud representing objects in the environment.
 */
public class CloudPoint {
    double x;
    double y;
    double z;

    public CloudPoint(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX(){
        return this.x;
    }

    public double getY(){
        return this.y;
    }

    public double getZ(){
        return this.z;
    }

    public void setX(double x){
        this.x = x;
    }

    public void setY(double y){
        this.y = y;
    }
}
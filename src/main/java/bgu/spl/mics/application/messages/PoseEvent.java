package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Boolean> {
    Pose pose;

    public PoseEvent(Pose pose){
        this.pose = pose;
    }

    public Pose getPose(){
        return this.pose;
    }
}

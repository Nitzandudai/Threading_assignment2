package bgu.spl.mics.application.messages;

import java.util.ArrayList;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.TrackedObject;

public class TrackedObjectsEvent implements Event<ArrayList<LandMark>> {
    private ArrayList<TrackedObject> trackedObject;

    public TrackedObjectsEvent(ArrayList<TrackedObject> trackedObject){
        this.trackedObject = trackedObject;
    }

    public ArrayList<TrackedObject> getList(){
        return this.trackedObject;
    }
}

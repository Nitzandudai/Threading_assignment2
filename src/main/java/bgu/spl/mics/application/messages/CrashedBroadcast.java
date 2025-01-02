package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    int objectId;
    String type;

    public CrashedBroadcast (int objectIdd, String type){
        this.objectId = objectIdd;
        this.type = type;
    }
}

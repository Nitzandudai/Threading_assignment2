package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast {
    private String sensor;

    public TerminatedBroadcast(String sensor){
        this.sensor = sensor;
    }

    public String getName(){
        return this.sensor;
    }

}

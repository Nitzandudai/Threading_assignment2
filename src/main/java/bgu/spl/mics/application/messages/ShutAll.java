package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class ShutAll implements Broadcast {
    private String output;

    public ShutAll (){
        this.output = "Time Is Up!";
    }

}

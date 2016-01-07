package com.example.david.sharing;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

//help by http://www.parcelabler.com/

/**
 * Created by david on 12-12-2015.
 */
public class Beacon implements Parcelable {

    private ArrayList<Session> sessionList;

    public boolean addSession(Session session)
    {
       return sessionList.add(session);
    }

    public Session getSession(int i){
        return sessionList.get(i);
    }

    public int nSessions(){
        return sessionList.size();
    }

    public enum State{
        UNKNOWN,
        FREE,
        OCCUPIED,
        OPEN
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getLastRSSI() {
        return lastRSSI;
    }

    public void setLastRSSI(int lastRSSI) {
        this.lastRSSI = lastRSSI;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void resetCounter() {
        this.counter = MAX_COUNT;
    }

    public void decCounter(){
        //Log.d("Beacon", uuid + " decremented");
        if(counter > 0)
            counter--;
    }

    public void incCounter(){
        //Log.d("Beacon", uuid + " incremented");
        if(counter < MAX_COUNT)
            counter++;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getVirtId() {
        return virtId;
    }

    public void setVirtId(int virtId) {
        this.virtId = virtId;
    }

    private BluetoothDevice device;
    private int major;
    private int minor;
    private int lastRSSI;
    private String uuid;
    private int counter;
    private final int MAX_COUNT = 10;

    private int virtId;

    private State state;

    private Beacon(){};

    public Beacon(BluetoothDevice device, int RSSI, int major, int minor, String uuid, State pState){
        this.device = device;
        this.major = major;
        this.minor = minor;
        this.lastRSSI = RSSI;
        this.uuid = uuid;
        counter = MAX_COUNT;
        state = pState;
        virtId = 0;

        sessionList = new ArrayList<>();
    }

    protected Beacon(Parcel in) {
        device = (BluetoothDevice) in.readValue(BluetoothDevice.class.getClassLoader());
        major = in.readInt();
        minor = in.readInt();
        lastRSSI = in.readInt();
        uuid = in.readString();
    }

    public void destroy(){
        for(int i = 0 ; i < sessionList.size(); i++)
            sessionList.get(i).leaveSession();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(device);
        dest.writeInt(major);
        dest.writeInt(minor);
        dest.writeInt(lastRSSI);
        dest.writeString(uuid);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Beacon> CREATOR = new Parcelable.Creator<Beacon>() {
        @Override
        public Beacon createFromParcel(Parcel in) {
            return new Beacon(in);
        }

        @Override
        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };
}
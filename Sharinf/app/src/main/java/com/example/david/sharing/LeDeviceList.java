package com.example.david.sharing;

import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by david on 01-01-2016.
 */
public class LeDeviceList {
    private ArrayList<Beacon> mLeDevices;

    LeDeviceList(){
        mLeDevices = new ArrayList<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i;
                while (true) {
                    i = 0;
                    for (Beacon it : mLeDevices) {
                        it.decCounter();
                        if (it.getCounter() == 0) {
//                            Log.d("Beacon", "Beacon removed: " + it.getUuid());
//                            removeDevice(i);
                        }
                        i++;
                    }
                    SystemClock.sleep(10000);
                }
            }
        }).start();

        updateBeacon();
    }

    public void updateBeacon(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    for (int i = 0; i < mLeDevices.size(); i++) {
                        ServerCon.getInstance().beaconDetected.request(mLeDevices.get(i));
                    }
                    SystemClock.sleep(3000);
                }
            }
        }).start();
    }

    public boolean addDevice(BluetoothDevice device, int RSSI, int major, int minor, String uuid) {

        boolean contains = false;

        for(Beacon it : mLeDevices)
            if(it.getDevice().equals(device)) {
                contains = true;
                it.setLastRSSI(RSSI);
                it.resetCounter();
                break;
            }

        if(!contains) {
            Beacon beacon = new Beacon(device, RSSI, major, minor, uuid, Beacon.State.UNKNOWN);
            beacon.resetCounter();
            ServerCon.getInstance().beaconDetected.request(beacon);
            mLeDevices.add(beacon);
            return true;
        }
        return false;
    }

    public Beacon getBeaconOfAddress(String address){
        for(Beacon it : mLeDevices)
            if(it.getDevice().getAddress().equals(address)) {
                return it;
            }

        return null;
    }

    public boolean removeDevice(final int index)
    {
        mLeDevices.get(index).destroy();
        ServerCon.getInstance().beaconUndetected.request(mLeDevices.get(index));
        mLeDevices.remove(index);
        return true;
    }

    public Beacon getItem(int i) {
        return mLeDevices.get(i);
    }

    public int getCount(){
        return mLeDevices.size();
    }
}

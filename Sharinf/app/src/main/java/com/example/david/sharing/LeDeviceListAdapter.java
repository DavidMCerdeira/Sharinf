package com.example.david.sharing;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 27-11-2015. 
 * "based" on: http://pastebin.com/Cgq7Mp5N
 */
/* Adapter for holding devices found through scanning. */
class LeDeviceListAdapter extends BaseAdapter {
    LeDeviceList mLeDevices;
    private LayoutInflater mInflator;
    Activity mother;

    public LeDeviceListAdapter(Activity mother, LeDeviceList deviceList) {
        super();
        this.mother = mother;
        mLeDevices = deviceList;
        mInflator = (LayoutInflater) mother.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        refreshDevices();
    }

    public void refreshDevices() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    dataChanged();
                    SystemClock.sleep(500);
                }
            }
        }).start();
    }

    public void dataChanged() {
        mother.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public int getCount() {
        return mLeDevices.getCount();
    }

    public Object getItem(int i) {
        return mLeDevices.getItem(i);
    }

    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_view, null);
            viewHolder = new ViewHolder();
            //viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.otherData = (TextView) view.findViewById(R.id.otherData);
            viewHolder.state = (TextView) view.findViewById(R.id.beacon_state);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        Beacon device = (mLeDevices.getItem(i));
        final String deviceName = device.getDevice().getName();

        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);

        //viewHolder.deviceAddress.setText(device.getDevice().getAddress());
        viewHolder.otherData.setText("RSSI: " + device.getLastRSSI());

        String stateStr = "State: ";
        switch(mLeDevices.getItem(i).getState())
        {
            case UNKNOWN:
                stateStr += "Unknown";
                break;
            case FREE:
                stateStr += "Free";
                break;
            case OCCUPIED:
                stateStr += "Occupied";
                break;
            case OPEN:
                stateStr += "Open";
                break;
            default:
                stateStr += "wat.";
        }

        viewHolder.state.setText(stateStr);
        return view;
    }

    static class ViewHolder{
        TextView deviceName;
        //TextView deviceAddress;
        TextView otherData;
        TextView state;
    }
}

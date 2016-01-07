package com.example.david.sharing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.SystemClock;
<<<<<<< HEAD

=======
>>>>>>> 72ddf9f654b41866a310bda7a8c8b785ab27a831
import android.util.Log;

/**
 * Created by david on 27-11-2015.
 */
public class BeaconFinder{
    Context mContext;
    BluetoothManager mBtManager;
    BluetoothAdapter mBtAdapter;
    private boolean mScanning;
    private LeDeviceList mLeDeviceList;

    public int getMajor(byte[] mScanRecord) {
        String major = String.valueOf((mScanRecord[25] & 0xff) * 0x100 + (mScanRecord[26] & 0xff));
        return Integer.parseInt(major);
    }

    public int getMinor(byte[] mScanRecord) {
        String minor = String.valueOf( (mScanRecord[27] & 0xff) * 0x100 + (mScanRecord[28] & 0xff));
        return Integer.parseInt(minor);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            int count;

            int N = 6;

            String uuid = new String();

            /* get UUID */
            for (count = 0; count < 16; count++) {
                uuid += String.format("%02x", scanRecord[9 + count]);
                //buuid[count] = scanRecord[9 + count];
            }

            /* convert data to hex */
            String data = new String();
            for (byte b : scanRecord)
                data += String.format("%02x", b);

/*            if(uuid.equals("0123456789abcdef0123000000000001")) {*/

                if (mLeDeviceList.addDevice(device, rssi, getMajor(scanRecord), getMinor(scanRecord), uuid)) {
                    count = 0;
                    Log.d("Device discovered:", ++count + "/" + N + ") Address: " + device.getAddress());
                    Log.d("Device discovered:", ++count + "/" + N + ") Name: " + device.getName());
                    Log.d("Device discovered:", ++count + "/" + N + ") UUID: " + uuid);
                    Log.d("Device discovered:", ++count + "/" + N + ") RSSI: " + rssi);
                    Log.d("Device discovered:", ++count + "/" + N + ") Data: " + data);
                    Log.d("Device discovered:", ++count + "/" + N + ") Class: M:" + getMajor(scanRecord) + " m:" + getMinor(scanRecord));
                }
            }
/*        }*/
    };

    /*
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Log.d("CharacteristicChanged", "");
            // this will get called anytime you perform a read or write characteristic operation
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            Log.d("onConnectionStateChange", "");
            // this will get called when a device connects or disconnects
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.d("onServicesDiscovered", "");
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
        }

    };*/

    private BeaconFinder(BluetoothManager BM) {
        mScanning = false;

        mBtManager = BM;//(BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
        mLeDeviceList = new LeDeviceList();
    }

    private static class BeaconFinderHolder {
        private static BeaconFinder INSTANCE = null;

        static void createInstance(BluetoothManager BM){
            INSTANCE = new BeaconFinder(BM);
        }
    }

    public static BeaconFinder getInstance(BluetoothManager BM) {
        if(BeaconFinderHolder.INSTANCE == null){
            BeaconFinderHolder.createInstance(BM);
        }

        return BeaconFinderHolder.INSTANCE;
    }

    public boolean getScanningState() {
        return mScanning;
    }

    public void scan(boolean enable) {
        if (enable) {
            mScanning = true;
            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
    }

    void autoRefresh(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mBtAdapter.startLeScan(mLeScanCallback);
                SystemClock.sleep(1000);
            }
        }).start();
    }

    LeDeviceList getDeviceList(){
        return mLeDeviceList;
    }

    public boolean isHardwareEnabled(){
        return (mBtAdapter != null && !mBtAdapter.isEnabled());
    }
}

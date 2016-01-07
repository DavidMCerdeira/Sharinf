package com.example.david.sharing;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

public class ConfigureSessionActivity extends AppCompatActivity {

    Session session;
    EditText session_name_edit;
    Spinner typeSpinner;
    Spinner whoSpinner;
    SeekBar distanceBar;
    Intent intent;
    Beacon beacon;
    ProgressDialog dialog;

    final int CHOOSE_BEACON = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_session);

        intent = this.getIntent();

        Bundle b = this.getIntent().getExtras();
        if (b != null) {
            String address = b.getString("beaconAddress");
            beacon = BeaconFinder.getInstance((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getDeviceList().getBeaconOfAddress(address);
        }else {
            beacon = null;
        }

        final Spinner typeSpinner = (Spinner) findViewById(R.id.type_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterType = ArrayAdapter.createFromResource(this,
            R.array.session_types, R.layout.my_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        typeSpinner.setAdapter(adapterType);

        whoSpinner = (Spinner) findViewById(R.id.who_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterWho = ArrayAdapter.createFromResource(this,
                R.array.who_shares, R.layout.my_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterWho.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        whoSpinner.setAdapter(adapterWho);

        session_name_edit = (EditText) findViewById(R.id.session_name_edit);
        distanceBar = (SeekBar) findViewById(R.id.seekBar);

        Button button = (Button) findViewById(R.id.Ok_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //dialog = ProgressDialog.show(ConfigureSessionActivity.this, "Creating Session", "Please Wait");
                session = new Session();
                //session.setDistance(distanceBar.getProgress());
                session.setName(session_name_edit.getText().toString());
                session.setShPerm((int) whoSpinner.getSelectedItemId());
                session.setType((int) typeSpinner.getSelectedItemId());

                Toast toast;
                Context context = getApplicationContext();
                if (ServerCon.getInstance().createSession.request(session, beacon)) {
                    toast = Toast.makeText(context, R.string.success_session_create, Toast.LENGTH_SHORT);

                    Intent i = new Intent();
                    Bundle b = new Bundle();

                    if(beacon == null){
                        Log.d("Configure_Session", "Beacon is null");
                    }else{
                        Log.d("Configure_Session", "Beacon is not null");
                    }

                    if(session == null){
                        Log.d("Configure_Session", "session is null");
                    }else{
                        Log.d("Configure_Session", "session is not null");
                    }

                    beacon.addSession(session);

                    b.putString("beaconAddress", beacon.getDevice().getAddress());
                    i.putExtras(b);
                    i.setClass(ConfigureSessionActivity.this, SessionActivity.class);
                    startActivity(i);

                } else {
                    toast = Toast.makeText(context, R.string.failed_session_create, Toast.LENGTH_SHORT);
                }
                //dialog.dismiss();
                toast.show();

                finish();
            }
        });
    }
}

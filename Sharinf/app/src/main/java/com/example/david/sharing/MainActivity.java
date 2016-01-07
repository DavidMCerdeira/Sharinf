package com.example.david.sharing;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    BeaconFinder beacon;
    private final int REQUEST_ENABLE_BT = 1;
    private final int CREATE_SESSION = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int PICKFILE_RESULT_CODE = 4;
    private LeDeviceListAdapter mLeDeviceList;
    ListView mListView;

    private GoogleApiClient mGoogleApiClient;
    private String userEmail;
    private static final String TAG = "Sharinf";
    private ProgressDialog mProgressDialog;
    public static SharinfFunctions shareFunc;
    Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        userEmail = i.getStringExtra("email");
        if(userEmail == null){
            userEmail = UserInfo.mail;
        }else{
            UserInfo.mail = userEmail;
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (beacon.getScanningState()) {
                    beacon.scan(false);
                    beacon.scan(true);
                } else {
                    beacon.scan(false);
                }

                Snackbar.make(view, "Scanning Beacons", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View view = findViewById(R.id.content_main);
        Toast toast;
        String toastMsg;
        if (!ServerCon.getInstance().isConnected()) {
            Log.d("MainActivity", "Opening connection");
            if(ServerCon.getInstance().openConnection()){
                toastMsg = "Connected";
            }else{
                toastMsg = "Not Connected";
            }
            toast = Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT);
            toast.show();
        }

        /* ble */
        beacon = BeaconFinder.getInstance((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE));
        beacon.autoRefresh();

        view = findViewById(R.id.content_main);
        String snackMsg;
        if (!ServerCon.getInstance().createConnection.request(UserInfo.mail)) {
            snackMsg = getString(R.string.login_failed);
        } else {
            snackMsg = getString(R.string.login_successful) + "\n" + "Welcome " + UserInfo.mail;
        }
        Log.d("MainActivity", "Login: " + snackMsg);
        Snackbar.make(view, snackMsg, Snackbar.LENGTH_LONG).setAction("Action", null).show();


        mListView = (ListView) findViewById(R.id.list1);

        Log.d("Test", "testing");

        beacon.scan(true);

        LeDeviceListAdapter listAdapter = new LeDeviceListAdapter(this, beacon.getDeviceList());
        mListView.setAdapter(listAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent i = new Intent();
                Beacon beac = beacon.getDeviceList().getItem((int) id);
                Bundle b = new Bundle();
                b.putString("beaconAddress", beac.getDevice().getAddress());
                i.putExtras(b);

                if (false) {
                    i.setClass(MainActivity.this, SessionActivity.class);
                }
                else {
                    if (beac.getState() == Beacon.State.UNKNOWN) {
                        Toast toast;
                        toast = Toast.makeText(getApplicationContext(), "Beacon is in an unknown state", Toast.LENGTH_SHORT);

                        toast.show();
                        return;
                    } else if (beac.getState() == Beacon.State.FREE) {
                        i.setClass(MainActivity.this, ConfigureSessionActivity.class);
                    } else {
                        Session sess = beac.getSession(0);
                        if (sess.joinSession()) {
                            i.setClass(MainActivity.this, SessionActivity.class);
                        }else
                            return;
                    }
                }
                startActivity(i);
            }
        });
        /* ble */
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient = GoogleApiClientHolder.getApiClient(this);

        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.registerConnectionCallbacks(this);

        mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }

    }

    @Override
    protected void onRestart(){
        super.onRestart();
//        if(beacon.getDeviceList().getCount() > 0)
//            ServerCon.getInstance().beaconDetected.request(beacon.getDeviceList().getItem(0));
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle){
        Log.d(TAG, "Google API Client connected.");
        shareFunc = SharinfFunctions.getInstance(mGoogleApiClient, userEmail, getApplicationContext());
        shareFunc.getSharinfFolder();
    }

    @Override
    public void onConnectionSuspended(int i){
        Log.d(TAG, "GoogleApiClient suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    // [START handleSignInResult]
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
    }
    // [END handleSignInResult]

    // [START signOut]
    public void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        Log.d(TAG, "handleSignOutResult:" + status.isSuccess());
                        finish();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    public void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        Log.d(TAG, "handleRevokeResult:" + status.isSuccess());
                        finish();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]

    private void get_file(){
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {

                }else {
                    finish();
                }
                break;
            case CREATE_SESSION:
                if(resultCode == RESULT_OK){
                }
                break;
            case REQUEST_CODE_RESOLUTION:
                if(resultCode == RESULT_OK){
                }
                break;
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    fileUri = data.getData();
                }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            ServerCon.getInstance().closeConnection();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

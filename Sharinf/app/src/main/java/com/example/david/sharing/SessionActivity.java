package com.example.david.sharing;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

public class SessionActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks{

    private final String TAG = "Session_ACtivity";

    FileListAdapter fileListAdapter;
    UserListAdapter userListAdapter;
    Session session;
    Beacon beacon;

    ListView mListView;
    TextView sessionName;

    Uri fileUri = null;
    private final int PICKFILE_RESULT_CODE = 1;
    private ProgressDialog mProgressDialog;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Created");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               ServerCon.getInstance().getSessionFiles.request();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Bundle b = this.getIntent().getExtras();
        if (b != null) {
            Log.d(TAG,"creating session");
            String address = b.getString("beaconAddress");
            beacon = BeaconFinder.getInstance((BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE)).getDeviceList().getBeaconOfAddress(address);
            session = beacon.getSession(0);

            sessionName = (TextView) findViewById(R.id.sessionTitle);
            sessionName.setText(sessionName.getText() + session.getName());
            setFileListView();

        } else
            beacon = null;

        Log.d(TAG, "ended");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = GoogleApiClientHolder.getApiClient(this);
        mGoogleApiClient.registerConnectionCallbacks(this);
    }

    private void setFileListView(){
        fileListAdapter = new FileListAdapter(getBaseContext(), this, session.getFileList());
        mListView = (ListView) findViewById(R.id.sessionListView);

        //TODO remove/open file
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File)fileListAdapter.getItem(position);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getLink()));
                startActivity(browserIntent);
            }
        });

        mListView.setAdapter(fileListAdapter);
    }

    private void setUserListView(){
        userListAdapter = new UserListAdapter(getBaseContext(), this, session.getUserList());
        mListView = (ListView) findViewById(R.id.sessionListView);

        //TODO remove/open User
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /* */
            }
        });

        mListView.setAdapter(userListAdapter);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Uploading");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void get_file(){
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    @Override
    public void onConnected(Bundle bundle){
        Log.d(TAG, "Google API Client connected.");
        SharinfFunctions.getInstance().createSessionFolder(session.getName());
    }

    @Override
    public void onConnectionSuspended(int i){
        Log.d(TAG, "GoogleApiClient suspended.");
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK && data != null) {
                    Log.d(TAG, "got file");
                    fileUri = data.getData();
                    SharinfFunctions.getInstance().saveFileToDrive(session.getName(), fileUri, getContentResolver());
                    showProgressDialog();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(!SharinfDriveEventService.UPLOADED);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SharinfDriveEventService.UPLOADED=false;
                                    hideProgressDialog();
                                }
                            });
                        }
                    }).start();
                }
        }
    }

    @Override
    public void onDestroy() {
        ServerCon.getInstance().leaveSession.request(session);
//        ServerCon.getInstance().closeConnection();
//        while(!ServerCon.getInstance().openConnection());
//        ServerCon.getInstance().createConnection.request(UserInfo.mail);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.session, menu);
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
        } else if (id == R.id.action_SessionSettings) {
            Intent intent = new Intent(SessionActivity.this, ConfigureSessionActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            ServerCon.getInstance().getUsersFromSession.request();
            setUserListView();
        } else if (id == R.id.nav_gallery) {
            ServerCon.getInstance().getSessionFiles.request();
            setFileListView();
        } else if (id == R.id.nav_share) {
            get_file();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Session Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.david.sharing/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
        SharinfFunctions.getInstance().setClient(mGoogleApiClient);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Session Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.david.sharing/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }
}

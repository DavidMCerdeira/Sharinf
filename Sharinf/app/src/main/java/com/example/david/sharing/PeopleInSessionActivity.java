package com.example.david.sharing;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by david on 11-12-2015.
 */
public class PeopleInSessionActivity extends AppCompatActivity {

    UserListAdapter userListAdapter;
    ListView mUserListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("PeopleInSession", "Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.people_in_session_activity);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                Snackbar.make(view, "Scanning Beacons", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
//            }
//        });

        ServerCon.getInstance().getUsersFromSession.request();
        userListAdapter = new UserListAdapter(getBaseContext(), this, ServerCon.getInstance().sessionInfo.activeSession.getUserList());

        mUserListView = (ListView) findViewById(R.id.userListView);

            //TODO remove/open User
            mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    /* */
                }
            });

            mUserListView.setAdapter(userListAdapter);

    }
}

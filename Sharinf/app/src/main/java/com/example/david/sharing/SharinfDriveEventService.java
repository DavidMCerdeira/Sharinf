package com.example.david.sharing;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.util.Arrays;

public class SharinfDriveEventService extends DriveEventService implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    private final String TAG = "DriveApp";
    public String email = null;
    private final String[] SCOPES = { DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE};
    public GoogleAccountCredential mCredential;
    public static boolean UPLOADED = false;

    @Override
    public void onCompletion(CompletionEvent event) {
        Log.d(TAG, "Action completed with status: " + event.getStatus());

        if (event.getStatus() == CompletionEvent.STATUS_SUCCESS) {
            // Commit completed successfully.
            Log.d(TAG, "Commit completed");

            silentSignin();

            mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff())
                    .setSelectedAccountName(email);

            new changeFilePermissions().new MakeRequestTask(mCredential, event.getDriveId().getResourceId()).execute();
        }
        event.dismiss();
    }

    public void silentSignin(){
        GoogleApiClient mGoogleApiClient = GoogleApiClientHolder.getApiClient(this);

        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            if(result.isSuccess()) email = result.getSignInAccount().getEmail();
            else Log.d(TAG,"failed to get account");
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            GoogleSignInAccount account = opr.await().getSignInAccount();
            if(account != null) email=account.getEmail();
            else Log.d(TAG,"failed to get account");
        }
    }

    @Override
    public void onConnected(Bundle bundle){
        Log.d(TAG, "Google API Client connected.");
    }

    @Override
    public void onConnectionSuspended(int i){
        Log.d(TAG, "GoogleApiClient suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
    }
}




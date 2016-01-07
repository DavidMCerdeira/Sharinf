package com.example.david.sharing;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;

public class changeFilePermissions extends Activity {
    private static final String TAG = "DriveApp";

    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    public class MakeRequestTask extends AsyncTask<Void, Void, String> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private String fileId;

        public MakeRequestTask(GoogleAccountCredential credential, String Id) {
            fileId = Id;

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("DriveApp")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                setFileLinkPerm();
                return getFileLink();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private void setFileLinkPerm() throws IOException {
            Permission newPermission = new Permission();

            newPermission.setType("anyone");
            newPermission.setRole("reader");
            newPermission.setAllowFileDiscovery(false);
            try {
                mService.permissions().create(fileId, newPermission).execute();
                Log.e(TAG, "Permission changed");
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e);
            }
        }

        private String getFileLink() throws IOException {
            try {
                com.google.api.services.drive.model.File file = mService.files().get(fileId)
                        .setFields("id,name,webContentLink")
                        .execute();
                String filelink = file.getWebContentLink();
                Log.e(TAG, "Link of file (\""+file.getName()+"\") got: " + filelink);

                ServerCon.getInstance().sessionInfo.activeSession.addFile(new File(filelink, file.getName()));
                SharinfDriveEventService.UPLOADED = true;

                return filelink;
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            changeFilePermissions.REQUEST_AUTHORIZATION);
                } else {
                    Log.e(TAG,"The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                Log.e(TAG, "Request cancelled.");
            }
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                changeFilePermissions.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Failure authorizing");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }



}


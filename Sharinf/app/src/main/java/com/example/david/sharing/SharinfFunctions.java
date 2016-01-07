package com.example.david.sharing;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SharinfFunctions {
    private static final String TAG = "DriveApp";

    private static final String[] SCOPES = { DriveScopes.DRIVE_APPDATA, DriveScopes.DRIVE_FILE};

    private InputStream fInputStream;
    private String filename;
    private String mimeType;
    private long filesize;

    private DriveId SharinfFolderId = null;
    private DriveId rootId = null;

    public static GoogleAccountCredential mCredential;
    public static GoogleApiClient mGoogleApiClient;


    private static class SharinfFunctionsHolder{
        private static SharinfFunctions INSTANCE = null;

        static public SharinfFunctions createInstance(GoogleApiClient ApiClient, String email, Context con){
            if(INSTANCE == null)
                INSTANCE =  new SharinfFunctions(ApiClient, email, con);

            return INSTANCE;
        }
    }

    public static SharinfFunctions getInstance(){
        return SharinfFunctionsHolder.INSTANCE;
    }

    public static SharinfFunctions getInstance(GoogleApiClient ApiClient, String email, Context con){
        return SharinfFunctionsHolder.createInstance(ApiClient, email, con);
    }

    public void setClient(GoogleApiClient apiClient){
        mGoogleApiClient = apiClient;
    }

    private SharinfFunctions(GoogleApiClient ApiClient, String email, Context con){
        mGoogleApiClient = ApiClient;
        mCredential = GoogleAccountCredential.usingOAuth2(
                con, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(email);

        rootId = Drive.DriveApi.getRootFolder(mGoogleApiClient).getDriveId();
    }

    public void getSharinfFolder(){
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Getting sharinf folder");
                DriveId foundFolderId = findSharinfSessionFolder("Sharinf", rootId);

                if(foundFolderId != null) {
                    if (foundFolderId.toInvariantString().equals( rootId.toInvariantString() ) ){
                        Log.e(TAG, "Creating Sharinf Folder");

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle("Sharinf")
                                .build();

                        SharinfFolderId = foundFolderId.asDriveFolder().createFolder(mGoogleApiClient, changeSet).await().getDriveFolder().getDriveId();
                        if (SharinfFolderId == null) {
                            Log.e(TAG, "Unable to create Sharinf Folder");
                            return;
                        }
                        Log.e(TAG, "Sharinf Folder created");
                    }
                    else SharinfFolderId = foundFolderId;
                }
            }
        }.start();
    }

    private DriveId findSharinfSessionFolder(String folder_name, DriveId parentId){

        Log.d(TAG, "Finding Sharing Session folder " + folder_name);
        Query folderQuery = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, folder_name),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();

        MetadataBuffer metadatas=parentId.asDriveFolder().queryChildren(mGoogleApiClient, folderQuery).await().getMetadataBuffer();

        if (metadatas == null) {
            Log.e(TAG, "Unable to retrieve queried folders.");
            return null;
        }

        if (metadatas.getCount() == 0) {
            Log.e(TAG, "No folder named \""+folder_name+"\" was found");
            metadatas.release();
            return rootId;
        } else if (metadatas.getCount() > 1){
            Log.d(TAG, "More than one folder named \"" + folder_name + "\" found");
            metadatas.release();
            return null;
        }
        Log.d(TAG, "Folder \"" + folder_name + "\" found");
        DriveId folderId = metadatas.get(0).getDriveId();

        metadatas.release();

        return folderId;
    }

    private DriveId findSharinfSessionFile(String file_name, String parent_session){
        Log.d(TAG, "Finding Sharinf Session file: " + file_name);
        DriveId sessionFolderId = findSharinfSessionFolder(parent_session, SharinfFolderId);

        Query folderQuery = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, file_name),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();

        if(sessionFolderId != null) {
            MetadataBuffer metadatas = sessionFolderId.asDriveFolder().queryChildren(mGoogleApiClient, folderQuery).await().getMetadataBuffer();

            if (metadatas == null) {
                Log.e(TAG, "Unable to retrieve queried files.");
                return null;
            }

            if (metadatas.getCount() == 0) {
                Log.e(TAG, "No file named \"" + file_name + "\" was found");
                metadatas.release();

                return rootId;
            } else if (metadatas.getCount() > 1) {
                Log.d(TAG, "More than one file named \"" + file_name + "\" found");
                metadatas.release();
                return null;
            }
            DriveId fileId = metadatas.get(0).getDriveId();

            metadatas.release();

            MetadataBuffer parents = fileId.asDriveFile().listParents(mGoogleApiClient).await().getMetadataBuffer();

            Log.d(TAG,"Found file in folder:" + parents.get(0).getTitle());
            parents.release();
            return fileId;
        }
        return null;
    }

    public void createSessionFolder(final String session_name){
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Creating session folder");

                DriveId foundFolderId = findSharinfSessionFolder(session_name, SharinfFolderId);

                if(foundFolderId != null){
                    if(foundFolderId.toInvariantString().equals(rootId.toInvariantString())){
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(session_name)
                                .build();

                        DriveId createdFolderId = SharinfFolderId.asDriveFolder().createFolder(mGoogleApiClient, changeSet).await().getDriveFolder().getDriveId();

                        if (createdFolderId == null) {
                            Log.e(TAG, "Unable to create folder.");
                            return;
                        }
                        Log.e(TAG, "Created Folder with id \"" + session_name + "\"");
                    }
                }
            }
        }.start();
    }

    public void deleteSessionFolder(final String session_name){
        new Thread() {
            @Override
            public void run() {
                DriveId foundFolderId = findSharinfSessionFolder(session_name, SharinfFolderId);

                if(foundFolderId != null){
                    if(!foundFolderId.toInvariantString().equals(rootId.toInvariantString())){
                        Log.e(TAG, "Deleting Folder with id \"" + session_name + "\"");
                        deleteSharinfResource(foundFolderId);
                    }
                }
            }
        }.start();
    }

    public void deleteSessionFile(final String file_name, final String parent_session,final GoogleApiClient mGoogleApiClient){
        new Thread() {
            @Override
            public void run() {
                DriveId foundFileId = findSharinfSessionFile(file_name, parent_session);

                if(foundFileId != null){
                    if(!foundFileId.toInvariantString().equals(rootId.toInvariantString())){
                        Log.e(TAG, "Deleting File with name \"" + file_name + "\"");
                        deleteSharinfResource(foundFileId);
                    }
                }
            }
        }.start();
    }

    private void deleteSharinfResource(final DriveId resourceId){

        Status status = resourceId.asDriveResource().delete(mGoogleApiClient).await().getStatus();

        if(status.getStatus().isSuccess()) {
            Log.e(TAG, "Unable to delete resource.");
        } else
            Log.e(TAG, "Deleted reaource with id \"" + resourceId + "\"");
    }



    // Save file to session folder
    public void saveFileToDrive(final String session_name, final Uri fileUri, final ContentResolver conRes) {
        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                getFileInfo(fileUri, conRes);
                DriveId sessionFolderId = findSharinfSessionFolder(session_name, SharinfFolderId);

                if (sessionFolderId == null) {
                    Log.e(TAG, "Unable to retrieve queried folders.");
                    return;
                }
                if(sessionFolderId.toInvariantString().equals(rootId.toInvariantString())){
                    Log.e(TAG, "Please open the session first");
                    return;
                }
                Log.d(TAG, "Creating File.");
                // create a file on root folder
                DriveId foundFileId = findSharinfSessionFile(filename,session_name);

                if (foundFileId == null) {
                    Log.e(TAG, "Error while trying to check for duplicate files");
                    return;
                }
                if(!foundFileId.toInvariantString().equals(rootId.toInvariantString())){
                    Log.e(TAG, "Found a file with the same name");
                    return;
                }



                Log.i(TAG, "Creating new contents.");
                DriveContents result = Drive.DriveApi.newDriveContents(mGoogleApiClient).await().getDriveContents();

                if (result == null) {
                    Log.e(TAG, "Error while trying to create new file contents");
                    return;
                }

                // write content to DriveContents
                OutputStream outputStream = result.getOutputStream();

                // Add the file to the output stream
                addFileToOutputStream(outputStream);





                // Create the initial metadata - MIME type and title.
                // Note that the user will be able to change the title later.
                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setMimeType(mimeType)
                        .setTitle(filename)
                        .build();

                ExecutionOptions executionOptions = new ExecutionOptions.Builder()
                        .setNotifyOnCompletion(true)
                        .setTrackingTag(filename)
                        .build();

                DriveId createdFileId = sessionFolderId.asDriveFolder().createFile(mGoogleApiClient, metadataChangeSet, result, executionOptions).await().getDriveFile().getDriveId();

                if(createdFileId == null){
                    Log.d(TAG, "Error creating file");
                    return;
                }

                Log.d(TAG, "Created a file with content: " + createdFileId.toInvariantString());
            }
        }.start();
    }

    //get input stream from text file, read it and put into the output stream
    private void addFileToOutputStream(OutputStream outputStream) {
        Log.i(TAG, "adding file to outputstream...");
        byte[] bFile = new byte[ (int) filesize];
        int bytesRead;

        try {
            BufferedInputStream inputStream = new BufferedInputStream(
                    fInputStream);

            while ((bytesRead = inputStream.read(bFile)) != -1) {
                outputStream.write(bFile, 0, bytesRead);
            }
            Log.d(TAG, "Done");
        } catch (IOException e) {
            Log.i(TAG, "problem converting input stream to output stream: " + e);
            e.printStackTrace();
        }
    }

    private void getFileInfo(Uri fileUri, ContentResolver conRes){
        File file = new File(fileUri.getPath());

        mimeType = conRes.getType(fileUri);
        if (mimeType == null) mimeType = "*/*";

        if (fileUri.getScheme().equals("content")) {
            Cursor returnCursor =
                    conRes.query(fileUri, null, null, null, null);
            if (returnCursor != null) {
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();

                filename = returnCursor.getString(nameIndex);
                filesize = returnCursor.getLong(sizeIndex);

                returnCursor.close();
            } else {
                filesize = 0;
                filename = "File not found";
            }


            try {
                fInputStream = conRes.openInputStream(fileUri);
            } catch (IOException e) {
                Log.i(TAG, "Error: " + e);
            }
        } else if (fileUri.getScheme().equals("file")) {
            filesize = file.length();
            filename = file.getName();
            try {
                fInputStream = new FileInputStream(fileUri.getPath());
            } catch (IOException e) {
                Log.i(TAG, "Error: " + e);
            }
        }
        if(filesize == 0) Log.e(TAG, "Filesize is 0");
    }
}

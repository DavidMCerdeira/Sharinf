package com.example.david.sharing;

import java.util.ArrayList;

/**
 * Created by david on 05-12-2015.
 */
public class Session {
    private ArrayList<User> userList;
    private ArrayList<File> sessionFiles;

    public static class Type{
        static final int PUBLIC = 0;
        static final int PRIVATE = 1;
        static final int SECRET = 2;
    }

    public static class SharingPermission{
        static final int EVERYBODY = 0;
        static final int RESTRICTED = 1;
    }

    static private final int MIN_DIST = 0;
    static private final int MAX_DIST = 2;

    private String mName;
    private int mDistance;
    private int mShPerm;
    private int mType;
    private int sessionID;

    public ArrayList<User> getUserList() {
        return userList;
    }

    public ArrayList<File> getFileList(){
        return sessionFiles;
    }

    public boolean addUser(String mail)
    {
        return true; //ServerCon.getInstance()
    }

    public boolean addFile(File file){
        return ServerCon.getInstance().loadFileSession.request(file);
    }

    public boolean addFileFromServer(File file){
        return sessionFiles.add(file);
    }

    public boolean addUserFromServer(User user){
        return userList.add(user);
    }

    public boolean removeFile(int id){
        return true;
    }

    public void removeFiles(){ sessionFiles.clear(); }

    public boolean joinSession(){
        return ServerCon.getInstance().joinSession.request(this);
    }

    public void leaveSession(){
        ServerCon.getInstance().leaveSession.request(this);
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }

    public int getType() {
        return mType;
    }

    public String getStrType() {
        switch(mType){
            case Type.PRIVATE:
                return "Private";
            case Type.PUBLIC:
                return "Public";
            case Type.SECRET:
                return "Secret";
            default:
                return "Error";
        }
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public int getShPerm() {
        return mShPerm;
    }

    public String getStrShPerm() {
        switch(mShPerm){
            case SharingPermission.EVERYBODY:
                return "Everybody";
            case SharingPermission.RESTRICTED:
                return "Resctricted";

            default:
                return "Error";
        }
    }

    public void setShPerm(int ShPerm) {
        this.mShPerm = ShPerm;
    }

    public int getDistance() {
        return mDistance;
    }

    public void setDistance(int mDistance) {
        this.mDistance = mDistance;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public Session(String mName, int mShPerm, int mType, int sessionID) {
        this.mName = mName;
        this.mShPerm = mShPerm;
        this.mType = mType;
        this.sessionID = sessionID;
        sessionFiles = new ArrayList<>();
        userList = new ArrayList<>();
    }

    Session(){
        sessionFiles = new ArrayList<>();
        userList = new ArrayList<>();
    }
}

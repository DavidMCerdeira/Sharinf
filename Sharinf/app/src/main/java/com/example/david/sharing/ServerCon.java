package com.example.david.sharing;

import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by david on 01-12-2015.
 */
public class ServerCon {

    private boolean block;

    Socket mSocketn;
    private boolean connected = false;
    private boolean closed = false;
    private boolean login = false;
    private boolean sessionCreated = false;
    private boolean sessionJoined = false;
    private boolean sessionleft = false;
    private boolean fileLoaded = false;
    private boolean autoUpdate = false;

    private final String IP = "192.168.8.100";
    private final int PORT = 8888;
    private final int TIMEOUT = 5000;

    private final int  CREATE_CONNECTION  =  0;
    private final int  BEACON_DETECTED    =  1;
    private final int  CREATE_SESSION     =  2;
    private final int  GUSERFROMSESS      =  3;
    private final int  GFILEF_FROM_SESS   =  4;
    private final int  JOIN_SESS          =  5;
    private final int  GETOUT_SESS        =  6;
    private final int  CHNG_SESS_CONFIG   =  7;
    private final int  LDFILE_TO_SESS     =  8;
    private final int  SESSENDED          =  9;
    private final int  YOURNEWADMIN       = 16;
    private final int  REQUESTJOINSESSION = 11;
    private final int  UPDATEUSERS        = 12;
    private final int  UPDATEFILES        = 15;
    private final int  BEACON_UNDETECTED  = 17;

    CreateConnection createConnection         = new CreateConnection(CREATE_CONNECTION);
    BeaconDetected beaconDetected             = new BeaconDetected(BEACON_DETECTED);
    CreateSession createSession               = new CreateSession(CREATE_SESSION);
    GetUsersFromSession getUsersFromSession   = new GetUsersFromSession(GUSERFROMSESS);
    GetSessionFiles getSessionFiles           = new GetSessionFiles(GFILEF_FROM_SESS);
    JoinSession joinSession                   = new JoinSession(JOIN_SESS);
    LeaveSession leaveSession                 = new LeaveSession(GETOUT_SESS);
    LoadFileSession loadFileSession           = new LoadFileSession(LDFILE_TO_SESS);
    SessionEnded sessionEnded                 = new SessionEnded(SESSENDED);
    YouAreAdmin youAreAdmin                   = new YouAreAdmin(YOURNEWADMIN);
    RequestToJoinSession requestToJoinSession = new RequestToJoinSession(REQUESTJOINSESSION);
    UpdateSessionFiles updateSessionFiles     = new UpdateSessionFiles(UPDATEFILES);
    UpdateSessionUsers updateSessionUsers     = new UpdateSessionUsers(UPDATEUSERS);
    BeaconUndetected beaconUndetected         = new BeaconUndetected(BEACON_UNDETECTED);

    ArrayList<SharinfP> sharinfPList = new ArrayList<>();

    public abstract class SharinfP{
        int CMD;
        final static char SUCCESS = 1;
        final static char UNSUCCESS = 0;

        final static String  BEGIN_FRAME = "SHARINF";
        final static int     BEGIN_FRAME_OFFSET = 7;
        final static int     CMD_POS = BEGIN_FRAME_OFFSET;

        ArrayList<byte[]> received = new ArrayList<>();
        ArrayList<byte[]> send     = new ArrayList<>();
    }

    public class SessionInfo {
        Session activeSession = null;
        boolean admin = false;
        String mail;

        Thread t0, t1, t2, t3;

        void activateSession(Session session) {
            activeSession = session;
            autoUpdateSession();
        }

        void deactivateSession(){
            stopAutoUpdateSession();
            admin = false;
            activeSession = null;
            for(int i = 0; i < sharinfPList.size(); i++){
                sharinfPList.clear();
            }
        }

        public void autoUpdateSession(){
            if(!autoUpdate) {
                autoUpdate = true;
                t0 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(autoUpdate) {
                            updateSessionUsers.parse();
                        }
                    }
                });
                t0.start();

                t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(autoUpdate){
                            updateSessionFiles.parse();
                        }
                    }
                });
                t1.start();

                t2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(autoUpdate){
                            youAreAdmin.parse();
                        }
                    }
                });
                t2.start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(autoUpdate) {
                            sessionEnded.parse();
                        }
                    }
                }).start();

                t3 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(autoUpdate){
                            requestToJoinSession.parse();
                        }
                    }
                });
                t3.start();
            }
        }

        public void stopAutoUpdateSession(){
            autoUpdate = false;
            t0.interrupt();
            t1.interrupt();
            t2.interrupt();
            t3.interrupt();
        }
    }

    SessionInfo sessionInfo = new SessionInfo();

    public abstract class Command extends SharinfP{
        final static int     RESULT_POS = CMD_POS + 1;
        final static int     MSG_POS = RESULT_POS + 1;
        final static String  END_FRAME = "\\endF";

        public byte[] parseSharinfResult(int command, byte[] buff) {
            for(int i = 0; i < BEGIN_FRAME.length(); i++)
                if(buff[i] != BEGIN_FRAME.charAt(i))
                    return null;

            if(buff[CMD_POS] != command)
                return null;

            if(buff[RESULT_POS] == UNSUCCESS) {
                parseError(buff);
                return null;
            }

            byte[] second = new byte[buff.length];

            for(int i = 0; i < (buff.length - (MSG_POS)); i++){
                second[i] = buff[i + MSG_POS];
            }

            if(second.length > 0)
                return second;
            else
                return "123".getBytes();
        }

        public void parseError(byte[] buff){
            JsonReader jreader;
            String errorDescr;

            byte[] holder = new byte[buff.length];

            for(int i = 0; i < (buff.length - (BEGIN_FRAME_OFFSET +2)); i++){
                holder[i] = buff[i + BEGIN_FRAME_OFFSET +2];
            }

            try {
                jreader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(holder), "UTF-8"));
                jreader.setLenient(true);
                jreader.beginObject();
            /*Error*/jreader.nextName(); errorDescr = jreader.nextString();
                jreader.endObject();
                Log.d("Server Con", "Sharinf error: " + errorDescr);
            }
            catch (Exception e)
            {
                Log.d("Server Con", "Parsing Error error: " + e.getMessage());
            }
        }
    }

    public class CreateConnection extends Command{
        CreateConnection(int i){
            CMD = i;
        }

        /* blocking */
        public boolean request(final String mail){
            Thread loginMail = new Thread(new Runnable() {
                @Override
                public void run() {
                    if(true) {
                        synchronized (mSocketn) {
                            try {
                                Log.d("Server Con", "Trying to login");
                                ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                                JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                                oBuff.write((BEGIN_FRAME + CMD).getBytes());
                                jwriter.beginObject();
                                jwriter.name("email").value(mail);
                                jwriter.endObject();
                                jwriter.flush();
                                oBuff.write(END_FRAME.getBytes());
                                send.add(oBuff.toByteArray());
                                oBuff.reset();

                                Log.d("Server Con", "Login sent");
                                /* receive received */
                                if (parse()) {
                                    sessionInfo.mail = mail;
                                    Log.d("Server Con", "Login Successful");
                                } else {
                                    Log.d("Server Con", "Login Unsuccessful");
                                }

                            } catch (Exception e) {
                                Log.d("Server Con", "Couldn't login: " + e.getMessage());
                                login = false;
                            }
                        }
                    }else
                        Log.d("Server Con", "Not connected!");
                }
            });

            loginMail.start();

            try{
                loginMail.join();
                return login;
            }catch (Exception e){
                Log.d("Server Con", "Exception joining thread" + e.getMessage());
                return false;
            }
        }

        public boolean parse() {
            try{
                try {
                    while (received.size() == 0) {
                        SystemClock.sleep(500);
                    }
                }catch (Exception e){
                    return false;
                }


            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                login = false;
                return false;
            }
            }catch (Exception e){

            }

            login = true;
            return login;
        }
    }

    public class BeaconDetected extends Command {

        BeaconDetected(int i) {
            CMD = i;
        }

        /* non blocking */
        public void request(final Beacon beacon) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSocketn) {
                        waitLogin();
                        try {
                            Log.d("Server Con", "Trying to send detected beacon");

                            ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                            oBuff.write((BEGIN_FRAME + CMD).getBytes());
                            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                            jwriter.beginArray();
                            jwriter.beginObject();
                            jwriter.name("uuid").value(beacon.getUuid());
                            jwriter.name("major").value(beacon.getMajor());
                            jwriter.name("minor").value(beacon.getMinor());
                            jwriter.name("lvirtual").value(0);
                            jwriter.endObject();
                            jwriter.endArray();
                            jwriter.flush();
                            oBuff.write(END_FRAME.getBytes());
                            send.add(oBuff.toByteArray());
                            oBuff.reset();

                            /* wait received */
                            parse(beacon);

                        } catch (Exception e) {
                            Log.d("Server Con", "Couldn't send beacon details: " + e.getMessage());
                        }
                    }
                }
            }).start();
        }

        private boolean parse(Beacon beacon) {

                Log.d("Server Con", "Parsing beacon");

                try {
                    while (received.size() == 0) {
                        SystemClock.sleep(500);
                    }
                } catch (Exception e) {
                    return false;
                }

                byte[] second = parseSharinfResult(CMD, received.get(0));
                Log.d("Server Con", "Beacon detected response " + new String(received.get(0)));
                received.remove(0);
                if (second == null) {
                    return false;
                }

                String beaconStateSessioned;
                int perm;
                int type;

                int virtId;
                int sessionId;
                String name;

                try {
                    JSONObject jObject = new JSONObject(new String(second));

                    virtId = jObject.getJSONArray("BeaconList").getJSONObject(0).getJSONObject("BeaconState").getInt("localizationID");
                    beaconStateSessioned = jObject.getJSONArray("BeaconList").getJSONObject(0).getJSONObject("BeaconState").getString("state");

                    if (beaconStateSessioned.equals("free")) {
                        beacon.setState(Beacon.State.FREE);
                    } else {
                        type = jObject.getJSONArray("SessionList").getJSONObject(0).getJSONObject("SessionConfig").getInt("type");
                        perm = jObject.getJSONArray("SessionList").getJSONObject(0).getJSONObject("SessionConfig").getInt("permissions");
                        sessionId = jObject.getJSONArray("SessionList").getJSONObject(0).getInt("sessionID");
                        name = jObject.getJSONArray("SessionList").getJSONObject(0).getString("s_name");

                        if (type == Session.Type.PUBLIC) {
                            beacon.setState(Beacon.State.OPEN);
                        } else {
                            beacon.setState(Beacon.State.OCCUPIED);
                        }

                        beacon.addSession(new Session(name, perm, type, sessionId));
                    }

                    beacon.setVirtId(virtId);
                    Log.d("Server Con", "Parsing beacon successful!");
                } catch (Exception e) {
                    Log.d("Server Con", "Parsing beacon: " + e.getMessage());
                    return false;
                }

                return true;
            }
    }

    public class BeaconUndetected extends Command{

        BeaconUndetected(int i){
            CMD = i;
        }
        /* non blocking */
        public void request(final Beacon beacon){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSocketn) {
                        waitLogin();
                        try {
                            Log.d("Server Con", "Trying to send detected beacon");

                            ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                            oBuff.write((BEGIN_FRAME + CMD).getBytes());
                            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                            jwriter.beginArray();
                            jwriter.beginObject();
                            jwriter.name("uuid").value(beacon.getUuid());
                            jwriter.name("major").value(beacon.getMajor());
                            jwriter.name("minor").value(beacon.getMinor());
                            jwriter.name("lvirtual").value(0);
                            jwriter.endObject();
                            jwriter.endArray();
                            jwriter.flush();
                            oBuff.write(END_FRAME.getBytes());

                            send.add(oBuff.toByteArray());
                            oBuff.reset();

                            /* wait received */
                            parse(beacon);

                        } catch (Exception e) {
                            Log.d("Server Con", "Couldn't send beacon details: " + e.getMessage());
                        }
                    }
                }
            }).start();
        }

        private boolean parse(Beacon beacon){

            Log.d("Server Con", "Parsing beacon");

            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            String beaconStateSessioned;
            int perm;
            int type;

            int virtId;
            int sessionId;
            String name;

            try {
                JSONObject jObject = new JSONObject(new String(second));

                virtId = jObject.getJSONArray("BeaconList").getJSONObject(0).getJSONObject("BeaconState").getInt("localizationID");
                beaconStateSessioned = jObject.getJSONArray("BeaconList").getJSONObject(0).getJSONObject("BeaconState").getString("state");

                if(beaconStateSessioned.equals("free")) {
                    beacon.setState(Beacon.State.FREE);
                }
                else {
                    type = jObject.getJSONArray("SessionList").getJSONObject(0).getJSONObject("SessionConfig").getInt("type");
                    perm = jObject.getJSONArray("SessionList").getJSONObject(0).getJSONObject("SessionConfig").getInt("permissions");
                    sessionId = jObject.getJSONArray("SessionList").getJSONObject(0).getInt("sessionID");
                    name = jObject.getJSONArray("SessionList").getJSONObject(0).getString("s_name");

                    if(type == Session.Type.PUBLIC){
                        beacon.setState(Beacon.State.OPEN);
                    }else{
                        beacon.setState(Beacon.State.OCCUPIED);
                    }

                    beacon.addSession(new Session(name, perm, type, sessionId));
                }

                beacon.setVirtId(virtId);
                Log.d("Server Con", "Parsing beacon successful!");
            } catch (Exception e){
                Log.d("Server Con", "Parsing beacon: " + e.getMessage());
                return false;
            }

            return true;
        }
    }

    public class CreateSession extends Command{
        CreateSession(int i){
            CMD = i;
        }
        /* blocking */
        public boolean request(final Session session, final Beacon beacon){
            Thread sessionT = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Server Con", "Trying to create session");
                    //waitLogin();
                    synchronized (mSocketn) {
                        if(true) {
                            try {
                                if (sessionInfo.activeSession != null) {
                                    Log.d("Server Con", "You're already in a session");
                                } else {
                                    ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                                    oBuff.write((BEGIN_FRAME + CMD).getBytes());
                                    JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                                    jwriter.beginObject();
                                    jwriter.name("SessionConfig");
                                    jwriter.beginObject();
                                    jwriter.name("permissions").value(session.getShPerm());
                                    jwriter.name("type").value(session.getType());
                                    jwriter.name("s_name").value(session.getName());
                                    jwriter.endObject();
                                    jwriter.name("BeaconState");
                                    jwriter.beginObject();
                                    jwriter.name("localizationID").value(beacon.getVirtId());
                                    jwriter.name("state").value(0);
                                    jwriter.endObject();
                                    jwriter.endObject();
                                    jwriter.flush();
                                    oBuff.write(END_FRAME.getBytes());
                                    send.add(oBuff.toByteArray());
                                    oBuff.reset();

                                /* wait received */
                                    sessionCreated = parse(session);
                                    if (sessionCreated) {
                                        sessionInfo.activateSession(session);
                                        getSessionFiles.request();
                                        getUsersFromSession.request();
                                        Log.d("Server Con", "Created session " + session.getName());
                                    } else {
                                        Log.d("Server Con", "Could\'t create session " + session.getName());
                                    }
                                }
                            } catch (Exception e) {
                                sessionCreated = false;
                                Log.d("Server Con", "Couldn't Create Session: " + e.getMessage());
                            }
                        }else
                        Log.d("Server Con", "Not connected!");
                    }
                }
            });
            sessionT.start();

            try{
                sessionT.join();
                return sessionCreated;
            }catch (Exception e){
                Log.d("Server Con", "Exception joining thread" + e.getMessage());
                return false;
            }
        }

        private boolean parse(Session session){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            boolean active;
            int id;

            try {
                JSONObject jObject = new JSONObject(new String(second));

                active = jObject.getBoolean("activeFlag");
                id = jObject.getInt("sessionID");

                Log.d("Server Con", "Session Created active:" + active + " Id:" + id);
            } catch (Exception e){
                Log.d("Server Con", "Parsing Create Session: " + e.getMessage());
                return false;
            }

            if(active){
                Log.d("Server Con", "Session active");
                session.setSessionID(id);
            }
            else{
                Log.d("Server Con", "Session not active");
            }

            return true;
        }
    }

    public class GetUsersFromSession extends Command{
        private boolean autoUpdateFlag;

        GetUsersFromSession(int i){
            CMD = i;
        }
        /* non blocking */
        public void request(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mSocketn) {
                        try {
                            ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                            Log.d("Server Con", "Trying to send detected beacon");
                            oBuff.write((BEGIN_FRAME + CMD).getBytes());
                            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                            jwriter.beginObject().name("sessionID").value(sessionInfo.activeSession.getSessionID()).endObject();
                            jwriter.flush();
                            oBuff.write(END_FRAME.getBytes());
                            send.add(oBuff.toByteArray());
                            oBuff.reset();

                        /* wait received */
                            parse();
                        } catch (Exception e) {
                            Log.d("Server Con", " Exceptio Getting user from session " + e.getMessage());
                        }
                    }
                }
            }).start();
        }

        private void parse(){
            Log.d("Server Con", "Getting users");
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return;
            }

            JsonReader jreader;
            String mail;

            Log.d("Server Con", "Getting users: " + new String(second));
            sessionInfo.activeSession.getUserList().clear();
            try {
                int i = 0;
                    JSONObject jObject = new JSONObject(new String(second));
                    JSONArray jArray = jObject.getJSONArray("userList");

                    for(i = 0; i < jArray.length(); i++) {
                        JSONObject file = jArray.getJSONObject(i);
                        mail = file.getString("email");
                        Log.d("Server Con", "Added user " + mail);
                        sessionInfo.activeSession.addUserFromServer(new User(mail));
                    }
            }
            catch (Exception e){
                Log.d("Server Con", "Exception Getting users from session: " + e.getMessage());
            }
        }

//        public void startAutoUpdate(){
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    autoUpdateFlag = true;
//                    while(autoUpdateFlag) {
//                        parse();
//                    }
//                }
//            }).start();
//        }
//
//        public void endAutoUpdate(){
//            autoUpdateFlag = false;
//        }
    }

    public class GetSessionFiles extends Command{
        private boolean autoUpdateFlag = false;

        GetSessionFiles(int i){
            CMD = i;
        }

        /* non blocking */
        public void request(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    waitLogin();
                    synchronized (mSocketn) {
                        try {
                            ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                            Log.d("Server Con", "Trying to get sessionFiles");
                            oBuff.write((BEGIN_FRAME + CMD).getBytes());
                            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                            jwriter.beginObject().name("sessionID").value(sessionInfo.activeSession.getSessionID()).endObject();
                            jwriter.flush();
                            oBuff.write(END_FRAME.getBytes());
                            send.add(oBuff.toByteArray());
                            oBuff.reset();

                            parse();
                        } catch (Exception e) {
                            Log.d("Server Con", "Exception getting files from session: " + e.getMessage());
                        }
                    }
                }
            }).start();
        }

        private boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }
            Log.d("Server Con", "Getting session files: " + new String(received.get(0)));
            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            String link;
            String owner;
            String name;

            int i = 0;
            try {
                JSONObject jObject = new JSONObject(new String(second));
                sessionInfo.activeSession.removeFiles();
                JSONArray jArray = jObject.getJSONArray("fileList");

                for(i = 0; i < jArray.length(); i++) {
                    JSONObject file = jArray.getJSONObject(i);
                    name = file.getString("name");
                    link = file.getString("link");
                    owner = file.getString("owneremail");
                    Log.d("Server Con", "File received: name " + name + " - link " + link + " email " + owner);
                    sessionInfo.activeSession.addFileFromServer(new File(link, name, owner));
                }
            } catch (Exception e){
                Log.d("Server Con", "Parsing get session files: " + e.getMessage());
                return false;
            }
            return true;
        }
//        public void startAutoUpdate(){
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    autoUpdateFlag = true;
//                    while(autoUpdateFlag) {
//                        parse();
//                    }
//                }
//            }).start();
//        }
//
//        public void endAutoUpdate(){
//            autoUpdateFlag = false;
//        }
    }

    public class JoinSession extends Command{
        JoinSession(int i){
            CMD = i;
        }

        /* blocking */
        public boolean request(final Session session){
            Thread joinSess = new Thread(new Runnable() {
                @Override
                public void run() {
                    waitLogin();

                    try {
                        Log.d("Server Con", "Trying to send get sessionFiles");
                        ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                        oBuff.write((BEGIN_FRAME + CMD).getBytes());
                        JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                        jwriter.beginObject();
                        jwriter.name("sessionID"); jwriter.value(session.getSessionID());
                        jwriter.endObject();
                        jwriter.flush();
                        oBuff.write(END_FRAME.getBytes());
                        Log.d("Server Con", "Sending " + oBuff.toString());
                        send.add(oBuff.toByteArray());
                        oBuff.reset();

                        sessionJoined = parse();
                        if(sessionJoined){
                            sessionInfo.activateSession(session);
                            getSessionFiles.request();
                            getUsersFromSession.request();
                            Log.d("Server Con", "Joined session " + session.getName() + " " + session.getSessionID());
                        }
                        else{
                            Log.d("Server Con", "Could\'t join session " + session.getName() + " " + session.getSessionID());
                        }
                    } catch (Exception e) {
                        Log.d("Server Con", "Exception joining session: " + e.getMessage());
                    }
                }
            });

            joinSess.start();

            try{
                joinSess.join();
                return sessionJoined;
            }catch (Exception e){
                Log.d("Server Con", "Exception joining thread" + e.getMessage());
                return false;
            }
        }

        public boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            return true;
        }
    }

    public class LeaveSession extends Command{
        LeaveSession(int i){
            CMD = i;
        }
        /* blocking */
        public boolean request(Session session) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    if (sessionInfo.activeSession != null) {
                        try {
                            Log.d("Server Con", "Trying to leave session");
                            ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                            oBuff.write((BEGIN_FRAME + CMD).getBytes());
                            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                            jwriter.beginObject();
                            jwriter.name("sessionID");
                            jwriter.value(sessionInfo.activeSession.getSessionID());
                            jwriter.endObject();
                            jwriter.flush();
                            oBuff.write(END_FRAME.getBytes());
                            Log.d("Server Con", "Sending " + oBuff.toString());
                            send.add(oBuff.toByteArray());
                            oBuff.reset();

                            sessionleft = parse();
                            if (sessionleft) {
                                Log.d("Server Con", "Left session " + sessionInfo.activeSession.getName() + " " + sessionInfo.activeSession.getSessionID());
                                /*sessionInfo.deactivateSession();*/
                            } else {
                                Log.d("Server Con", "Could\'t leave session " + sessionInfo.activeSession.getName() + " " + sessionInfo.activeSession.getSessionID());
                            }
                        } catch (Exception e) {
                            Log.d("Server Con", "Exception leaving session: " + e.getMessage());
                        }
                    }
                }
            });

            thread.start();
            try {
                thread.join();
                return sessionleft;
            } catch (Exception e) {
                Log.d("Server Con", "Exception joining thread" + e.getMessage());
                return false;
            }
        }

        public boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }
            return true;
        }
    }

    public class LoadFileSession extends Command{
        LoadFileSession(int i){
            CMD = i;
        }

        /* blocking */
        public boolean request(final File file){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    waitLogin();
                    try {
                        Log.d("Server Con", "Trying to load file to session");
                        ByteArrayOutputStream oBuff = new ByteArrayOutputStream();
                        oBuff.write((BEGIN_FRAME + CMD).getBytes());
                        JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(oBuff, "UTF-8"));
                        jwriter.beginObject()
                                .name("File").beginObject()
                                    .name("name").value(file.getName())
                                    .name("link").value(file.getLink())
                                    .name("owneremail").value(sessionInfo.mail)/*This users mail*/
                                .endObject()
                            .name("sessionID").value(sessionInfo.activeSession.getSessionID())
                        .endObject();
                        jwriter.flush();
                        oBuff.write(END_FRAME.getBytes());
                        send.add(oBuff.toByteArray());
                        oBuff.reset();

                        fileLoaded = parse();
                        if(fileLoaded){
                            Log.d("Server Con", "File loaded to session " + sessionInfo.activeSession.getName() + " " + sessionInfo.activeSession.getSessionID());
                        }
                        else{
                            Log.d("Server Con", "Could\'t laod file to session " + sessionInfo.activeSession.getName() + " " + sessionInfo.activeSession.getSessionID());
                        }
                    } catch (Exception e) {
                        Log.d("Server Con", "Exception loading file to session: " + e.getMessage());
                    }
                }
            });

            thread.start();

            try{
                thread.join();
                return fileLoaded;
            }catch (Exception e){
                Log.d("Server Con", "Exception joining thread" + e.getMessage());
                return false;
            }
        }

        public boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinfResult(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            return true;
        }
    }

    public abstract class ServerMsg extends SharinfP{
        final static int MSG_POS = CMD_POS + 1;

        public byte[] parseSharinf(int command, byte[] buff){

            for(int i = 0; i < BEGIN_FRAME.length(); i++)
                if(buff[i] != BEGIN_FRAME.charAt(i))
                    return null;

            if(buff[CMD_POS] != command)
                return null;

            byte[] second = new byte[buff.length];

            for(int i = 0; i < (buff.length - (MSG_POS)); i++){
                second[i] = buff[i + MSG_POS];
            }

            if(second.length > 0)
                return second;
            else
                return "123".getBytes();
        }
    }

    public class SessionEnded extends ServerMsg{

        SessionEnded(int cmd){
            CMD = cmd;
        }

        void parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return;
            }

            byte[] second = parseSharinf(CMD, received.get(0));
            received.remove(0);
            if(second == null){

            }
        }
    }

    public class YouAreAdmin extends ServerMsg{
        YouAreAdmin(int cmd){
            CMD = cmd;
        }

        void parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return;
            }

            byte[] second = parseSharinf(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return;
            }
            sessionInfo.admin = true;
        }
    }

    //TODO request to join session
    public class RequestToJoinSession extends ServerMsg{
        RequestToJoinSession(int cmd){
            CMD = cmd;
        }

        void parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return;
            }

            byte[] second = parseSharinf(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return;
            }
        }
    }

    public class UpdateSessionFiles extends ServerMsg{
        UpdateSessionFiles(int cmd){ CMD = cmd; }

        boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinf(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            String link;
            String owner;
            String name;
            int sessionID;

            int i = 0;
            try {
                JSONObject jObject = new JSONObject(new String(second));
                sessionID = jObject.getInt("sessionID");
                if(sessionID != sessionInfo.activeSession.getSessionID())
                    return false;

                sessionInfo.activeSession.removeFiles();
                JSONArray jArray = jObject.getJSONArray("fileList");

                for(i = 0; i < jArray.length(); i++) {
                    JSONObject file = jArray.getJSONObject(i);
                    name = file.getString("name");
                    link = file.getString("link");
                    owner = file.getString("owneremail");
                    Log.d("Server Con", "File received: name " + name + " - link " + link + " email " + owner);
                    sessionInfo.activeSession.addFileFromServer(new File(link, name, owner));
                }
            } catch (Exception e){
                Log.d("Server Con", "Parsing update session files at " + i + ": " + e.getMessage());
                return false;
            }

            Log.d("Server Con", "Update files succesful");
            return true;
        }
    }

    public class UpdateSessionUsers extends ServerMsg{
        UpdateSessionUsers(int cmd){ CMD = cmd; }

        boolean parse(){
            try {
                while (received.size() == 0) {
                    SystemClock.sleep(500);
                }
            }catch (Exception e){
                return false;
            }

            byte[] second = parseSharinf(CMD, received.get(0));
            received.remove(0);
            if(second == null){
                return false;
            }

            String mail;
            int sessionID;

            int i = 0;
            try {
                JSONObject jObject = new JSONObject(new String(second));
                sessionID = jObject.getInt("sessionID");
                if(sessionID != sessionInfo.activeSession.getSessionID())
                    return false;

                //sessionInfo.activeSession.removeFiles();
                JSONArray jArray = jObject.getJSONArray("userList");
                sessionInfo.activeSession.getUserList().clear();
                for(i = 0; i < jArray.length(); i++) {
                    JSONObject file = jArray.getJSONObject(i);
                    mail = file.getString("email");
                    sessionInfo.activeSession.addUserFromServer(new User(mail));
                }
            }
            catch (Exception e){
                Log.d("Server Con", "Exception updating users from session: " + e.getMessage());
                return false;
            }

            Log.d("Server Con", "Update to users successful");
            return true;
        }
    }

    private ServerCon() {
        sharinfPList.add(createConnection);
        sharinfPList.add(beaconDetected);
        sharinfPList.add(createSession);
        sharinfPList.add(getUsersFromSession);
        sharinfPList.add(getSessionFiles);
        sharinfPList.add(joinSession);
        sharinfPList.add(leaveSession);
        sharinfPList.add(loadFileSession);
        sharinfPList.add(sessionEnded);
        sharinfPList.add(youAreAdmin);
        sharinfPList.add(requestToJoinSession);
        sharinfPList.add(updateSessionFiles);
        sharinfPList.add(updateSessionUsers);
        sharinfPList.add(beaconUndetected);
    }

    private static class ServerConHolder {
        private static ServerCon INSTANCE = null;

        static public ServerCon createInstance(){
            if(INSTANCE == null)
                INSTANCE = new ServerCon();

            return INSTANCE;
        }
    }

    public static ServerCon getInstance() {
        //Log.d("Server Con", "Instance was requested");
        return ServerConHolder.createInstance();
    }

    public boolean isConnected() {
        return connected;
    }

    public void waitLogin(){
        while(!login){
            SystemClock.sleep(1000);
        }
    }

    /* blocking */
    public boolean openConnection(){
       Thread openCon = new Thread(new Runnable() {
            @Override
            public void run() {

                    try {
                        mSocketn = new Socket();
                        Log.d("Server Con", "Trying to open Connection");
                        mSocketn.connect(new InetSocketAddress(IP, PORT), TIMEOUT);
                        connected = true;
                        Log.d("Server Con", "Open connection sucessful");
                        receptionist();
                        sender();
                    } catch (Exception e) {
                        Log.d("Server Con", "Open connection failed: " + e.getMessage());
                        connected = false;
                    }

            }
        });

        openCon.start();

        try{
            openCon.join();
        }catch(Exception e){
            Log.d("Server Con", "Exception: " + e.getMessage());
        }
        return connected;
    }

    /* non blocking */
    public boolean closeConnection(){
        Thread closeCon = new Thread(new Runnable() {
            @Override
            public void run() {

                synchronized (mSocketn) {
                    try {
                        Log.d("Server Con", "Closing connection");
                        mSocketn.close();
                        closed = true;
                    } catch (Exception e) {

                        Log.d("Server Con", "Closing connection failed");
                        closed = false;
                        return;
                    }
                    closed = true;
                    Log.d("Server Con", "Connection closed");
                }
            }
        });

        closeCon.start();
        return closed;
    }

    public void receptionist(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Server Con", "Receptionist running");
                while(connected) {
                    byte[] buffer = new byte[1024];
                    try {

                        if(mSocketn.isConnected() && !mSocketn.isClosed()) {

                            mSocketn.getInputStream().read(buffer);

                        }else
                            return;

                    } catch (Exception e) {
                        Log.d("Server Con", "Error reading from socket: " + e.getMessage());
                        closeConnection();
                        return;
                    }

                    int i = 0;
                    while(i < sharinfPList.size()){
                        if(sharinfPList.get(i).CMD == buffer[Command.CMD_POS]){
                            if(sharinfPList.get(i).received.size() > 10){
                                Log.d("Server Con", "Message lost: " + buffer[Command.CMD_POS]);
                                return;
                            }else{
                                Log.d("Server Con", "Message received: " + buffer[Command.CMD_POS]);
                                sharinfPList.get(i).received.add(buffer);
                                break;
                            }
                        }
                        i++;
                    }
                }
            }
        }).start();
    }
    
    public void sender(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    for (int i = 0; i < sharinfPList.size(); i++) {
                        if (sharinfPList.get(i).send.size() > 0) {
                            try {
                                Log.d("Server Con", "Tryin to send message " + new String(sharinfPList.get(i).send.get(0)) + " from: " + sharinfPList.get(i).CMD);

                                mSocketn.getOutputStream().write(sharinfPList.get(i).send.get(0));

                                sharinfPList.get(i).send.remove(0);
                            } catch (Exception e) {
                                Log.d("Server Con", "Exception in sender: " + e.getMessage());
                            }
                        }
                    }
                    SystemClock.sleep(500);
                }
            }
        }).start();
        
    }

    public void sayHello(){
        Thread hello = new Thread(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    synchronized (mSocketn) {
                        try {
                            byte result[] = new byte[1024];

                            Log.d("Server Con", "Saying hello");
                            mSocketn.getOutputStream().write("Hello Server!".getBytes());
                            mSocketn.getInputStream().read(result);

                            Log.d("Server Con", "Result: " + new String(result));
                        } catch (Exception e) {
                            Log.d("Server Con", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    Log.d("Server Con", "Not connected!");
                }
            }});
        hello.start();
    }

    /* non blocking */
    public void getNearbyUsers(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Log.d("Server Con", "Trying to get nearby user");
//                    send.add("SHARINFNEARBYUSER".getBytes());
//                    send.add("\\endF".getBytes());
                    /* wait received */
                } catch (Exception e) {
                    Log.d("Server Con", "Couldn't get nearby user");
                }
            }
        }).start();
    }
}

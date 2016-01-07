import mysql.connector

class DB_Accesser():

    def __init__(self):
        self.cnx = mysql.connector.connect(user = 'root', password='/dev/ttyUSB0', host = '127.0.0.1', database = 'sharinf_db')
        self.cursor = self.cnx.cursor(buffered=True)


# {"email" : "<string>"}
    def Insert_User(self, in_user):
        if self.Get_User(in_user) is None:
            insertUser_Qry = "INSERT INTO users VALUE(%(email)s)"
            self.cursor.execute(insertUser_Qry, in_user)
            self.cnx.commit()
            return True
        return False

    def Get_User(self, gt_user):
        getUser_Qry = "SELECT users.userMail FROM users WHERE users.userMail = %(email)s"
        self.cursor.execute(getUser_Qry, gt_user)
        row = self.cursor.fetchone()
        if row is not None:
            return { "email" : row[0] }
        else:
            return None
            ##
# { "link":"<string>", "owneremail":"<string>"}
    def Insert_File(self, in_file):
        result_us = self.Get_User({"email" : in_file['owneremail']})
        result_fl = self.Get_File(in_file)
        if (result_us is not None) and (result_fl is None):
            insertFile_Qry = "INSERT INTO files (Owner, URL, name) VALUE (%(owneremail)s, %(link)s, %(name)s)"
            self.cursor.execute(insertFile_Qry, in_file)
            self.cnx.commit()
            return True
        else:
            return False

    def Get_File(self, gt_file):
        getFile_Qry = "SELECT * FROM files AS f WHERE f.URL = %(link)s"
        self.cursor.execute(getFile_Qry, gt_file)
        row = self.cursor.fetchone()
        if(row is None):
            return None
        else:
            return {"link":row[1], "owneremail":row[0], "name":row[2]}


#''' {"uuid" : "uuidcenas3", "major": 12, "minor": 32,"virtual": 0} this is a BeaconPhysID struct  '''
    def Insert_Location(self, in_location):
        insertLocal_Qry = "INSERT INTO localization (uuid, major, minor, lvirtual) VALUES (%(uuid)s, %(major)s, %(minor)s, %(lvirtual)s)"
        if self.Get_Location(in_location) is None:
            self.cursor.execute(insertLocal_Qry, in_location)
            self.cnx.commit()
            return True
        else:
            return False

#''' Receives a BeaconState struct or a BeaconPhysID struct '''
    def Get_Location(self, gt_location):
        if  "localizationID" in gt_location.keys():
            getLocal_Qry = "SELECT * FROM localization l WHERE l.id = %(localizationID)s"
        else:
            getLocal_Qry = "SELECT * FROM localization AS l WHERE l.uuid=%(uuid)s AND l.minor=%(minor)s AND l.major=%(major)s"
        self.cursor.execute(getLocal_Qry, gt_location)
        row = self.cursor.fetchone()
        if row is None:
            return None
        else:
            tempVirtID = row[0]
            BeaconPhysID = {"uuid":row[1], "major":row[2], "minor":row[3], "lvirtual": row[4]}
            getLocationSession_Qry = "SELECT l.SessionID FROM LocationSession l WHERE l.Flag = True AND l.localID = %(id)s"
            self.cursor.execute(getLocationSession_Qry, {"id":tempVirtID})
            row = self.cursor.fetchone()
            if row is not None:
                state = "occupied"
            else:
                state = "free"
            BeaconState = {"localizationID" : tempVirtID, "state" : state}
            return {"BeaconPhysID" : BeaconPhysID, "BeaconState" : BeaconState}

# {"permissions": <int>, "activeFlag":True, "localizationID":<localizationID>, "s_type":<int>, "permissions":<int>  , "name":<string>->Optional}
    def Insert_Session(self, in_session):
        insertSession_Qry = "INSERT INTO Sessions \
                             (s_begin, activeFlag, localizationID, s_type, permissions"

        isThereName = "s_name" in in_session.keys()

        if isThereName:
            insertSession_Qry +=  ", s_name) VALUES (NOW(), %(activeFlag)s, %(localizationID)s, %(s_type)s, %(permissions)s, %(s_name)s)"
        else:
            insertSession_Qry += ")VALUES (NOW(), %(activeFlag)s, %(localizationID)s, %(s_type)s, %(permissions)s)"

        location = {"localizationID" : in_session["localizationID"]};

        if self.Get_Location(location) is not None and self.Get_ActiveSessionFromLocal(location) is None:
            self.cursor.execute(insertSession_Qry, in_session)
            self.cnx.commit()
            getLastID_Qry = "SELECT LAST_INSERT_ID()"
            self.cursor.execute(getLastID_Qry)
            row = self.cursor.fetchone()
            returnDic = {
                "SessionConfig" : { "permissions": in_session["permissions"], "type": in_session["s_type"]},
                "sessionID" : row[0],
                "activeFlag": in_session["activeFlag"]
               }
            if isThereName:
                returnDic["s_name"] = in_session["s_name"]
            return returnDic
        else:
            return False

#''' {"sessionID" : <int>} '''
    def Get_Session(self, in_session):
        getSession_Qry = "SELECT * FROM Sessions s WHERE s.idSession = %(sessionID)s"
        self.cursor.execute(getSession_Qry, in_session)
        row = self.cursor.fetchone()
        if row is None:
            return None
        else:
            return 	{
                      "SessionConfig": { "permissions": row[6], "type": row[5]},
            		  "sessionID": row[0],
            		  "activeFlag": row[3],
                      "s_name": row[7]
            	    }

#''' {"email": <string>, "sessionID":<int>} '''
    def Insert_UserInSession(self, userSessionRel):
        insertSessUsrList_Qry = "INSERT INTO SessionUserList (sessionID, user, adminFlag) \
                                 VALUES (%(sessionID)s, %(email)s, False)"

        getSessRes = self.Get_Session({"sessionID":userSessionRel["sessionID"]})    #
        getUsrRes = self.Get_User({"email":userSessionRel["email"]}) is not None    #Insertion Condition
        getUsrInSessRes = self.Get_UserInSession(userSessionRel) is None            #

        if getUsrRes and getUsrInSessRes and getSessRes is not None:
            if getSessRes["activeFlag"] == True:
                self.cursor.execute(insertSessUsrList_Qry, userSessionRel)
                self.cnx.commit()
                return True
        return False

    #{"email": <string>, "sessionID":<int>}
    def Remove_UserFromSession(self, userSessionRel):
        removeSessUsrList_Qry = "DELETE FROM sessionuserlist \
                                 Where sessionID = %(sessionID)s and user = %(email)s"

        getSessRes = self.Get_Session({"sessionID":userSessionRel["sessionID"]})   #
        getUsrRes = self.Get_User({"email":userSessionRel["email"]})    #Insertion Condition
        getUsrInSessRes = self.Get_UserInSession(userSessionRel)          #

        if (getSessRes is not None) and (getUsrRes is not None) and (getUsrInSessRes is not None):
            if getSessRes["activeFlag"] == True:
                self.cursor.execute(removeSessUsrList_Qry, userSessionRel)
                self.cnx.commit()
                return True
        return False
#{"email":<string>, "localizationID":<int>}
    def Insert_UserInLocation(self, userLocalRel):
        insertUserInLocal_Qry = "INSERT INTO locationuserlist (userMail, locationId) VALUES (%(email)s, %(localizationID)s)"
        if self.Get_User({"email":userLocalRel["email"]}) is not None and self.Get_Location({"localizationID":userLocalRel["localizationID"]}) and self.Get_UserFromLocation(userLocalRel) is False:
            self.cursor.execute(insertUserInLocal_Qry, userLocalRel)
            self.cnx.commit()
            return True
        else:
            return False
            
    def Remove_UserFromLocation(self, userLocalRel):
        removeUserFromLocal_Qry = "DELETE FROM locationuserlist WHERE userMail = %(email)s AND locationID = %(localizationID)s"
        if self.Get_UserFromLocation(userLocalRel) is True:
            self.cursor.execute(removeUserFromLocal_Qry, userLocalRel)
            self.cnx.commit()
            return True
        else:
            return False

    def Get_UserFromLocation(self, userLocalRel):
        getUserFromLocation_Qry = "SELECT * FROM locationuserlist lul WHERE lul.locationID = %(localizationID)s AND lul.userMail = %(email)s"
        if self.Get_User({"email":userLocalRel["email"]}) is not None and self.Get_Location({"localizationID":userLocalRel["localizationID"]}):
            self.cursor.execute(getUserFromLocation_Qry, userLocalRel)
            row = self.cursor.fetchone()
            if row is not None:
                return True
            else:
                return False
        else:
            return False

#{"email": <string>, "sessionID":<int>}
    def Set_SessionAdmin(self, userSession):
        updateAdmin_Qry = "UPDATE SessionUserList s SET s.adminFlag = 1 WHERE s.user=%(email)s AND s.sessionID= %(sessionID)s"
        if self.Get_Session({"sessionID":userSession["sessionID"]}) is None:
            return False
        if self.Get_User({"email":userSession["email"]}) is None:
            return False
        if self.Get_UserInSession(userSession) is None:
            return False
        self.cursor.execute(updateAdmin_Qry, userSession)
        self.cnx.commit()
        return True

#{"email": <string>, "sessionID":<int>}
    def Get_UserInSession(self, userInSess):
        getUserInSession_Qry = "SELECT * FROM SessionUserList s \
                                WHERE s.sessionID = %(sessionID)s \
                                AND s.user = %(email)s"
        self.cursor.execute(getUserInSession_Qry, userInSess)
        row = self.cursor.fetchone()
        if row is not None:
            return {"sessionID": row[0], "email": row[1], "adminFlag":row[2]}
        else:
            return None #Nothing to return

#{"sessionID" : int}
    def Get_AllUsersInSession(self, in_session):
        getAllUsrSession_Qry = "SELECT u.userMail, su.adminFlag \
                                FROM sessionuserlist su, users u, sessions s \
                                WHERE su.user = u.userMail \
                                AND su.sessionID = s.idSession \
                                AND su.sessionID = %(sessionID)s"
        self.cursor.execute(getAllUsrSession_Qry,in_session)
        row = self.cursor.fetchall()
        if row is None:
            return None
        usersArray = []
        for i in range(0, len(row)):
            usersArray.insert(i, { "email": row[i][0] })
            i += 1
        return usersArray

    def Get_SessionAdmin(self, in_session):
        getSessionAdmin_Qry = "SELECT su.user \
                                FROM sessionuserlist su, sessions s \
                                WHERE su.adminFlag = True \
                                AND su.sessionID = %(sessionID)s"
        self.cursor.execute(getSessionAdmin_Qry, in_session)
        row = self.cursor.fetchone()
        if row is None:
            return None
        else:
            return {"email":row[0]}

    #''' {"sessionID":<int>, "File":{"link" : <string>, "owneremail" : <string>}} '''
    def Insert_FileInSession(self, inFileSess):
        insFileInSess_Qry = "INSERT INTO SessionFileList (sessionID, fileURL) \
                             VALUES (%(sessionID)s, %(link)s)"
        userSession = {"email" : inFileSess["File"]["owneremail"], "sessionID":inFileSess["sessionID"]}
        if self.Get_UserInSession(userSession) is not None and self.Get_FileFromSession(inFileSess) is None:
            sessionLink={"sessionID":inFileSess["sessionID"], "link":inFileSess["File"]["link"]}
            self.cursor.execute( insFileInSess_Qry, sessionLink)
            self.cnx.commit()
            return True     #Insertion Complete
        return False        #the file already exists in that session, or User is not in Session

    # {"sessionID":<int>}
    def Get_FileFromSession(self, fileFromSess):
        #print(fileFromSess)
        gtFilefrmSess_Qry = "SELECT f.* FROM Files f, sessionfilelist sfl \
                             WHERE f.URL = %(link)s AND sfl.fileURL = f.URL   \
                             AND sfl.sessionID = %(sessionID)s"
        tempD = {"link" : fileFromSess["File"]["link"], "sessionID" : fileFromSess["sessionID"]}
        self.cursor.execute(gtFilefrmSess_Qry, tempD)
        row = self.cursor.fetchone()

        if row is None:
            return None
        else:
            return {"link":row[1], "owneremail":row[0], "name":row[2]}

    def Get_AllFilesFromSession(self, in_session):
        getAllFilesFromSess_Qry = "SELECT f.* \
                                   FROM files f, sessionfilelist sf \
                                   WHERE sf.fileURL = f.URL \
                                   AND sf.sessionID = %(sessionID)s"
        self.cursor.execute(getAllFilesFromSess_Qry, in_session)
        row = self.cursor.fetchall()
        if row is None:
            return None
        filesArray = []
        for i in range(0, len(row)):
            filesArray.insert(i, {"owneremail": row[i][2], "link":row[i][1], "name": row[i][0]})
            i += 1
        return filesArray

    #{"File":<File>, "SessionID":<int>}
    def Remove_FileFromSession(self, fileSessionRel):
        remFileFromSess_Qry = "DELETE FROM sessionfilelist WHERE fileURL = %(link)s and sessionID = %(sessionID)s"

        sessFile = {"sessionID" : fileSessionRel["sessionID"], "link":fileSessionRel["File"]["link"]}

        getSessRes = self.Get_Session({"sessionID" : fileSessionRel["sessionID"]})
        getFileRes = self.Get_File(fileSessionRel["File"])
        getFileInSessRes = self.Get_FileFromSession(fileSessionRel)

        print(getSessRes, getFileRes, getFileInSessRes)
        if (getSessRes is not None) and (getFileRes is not None) and (getFileInSessRes is not None):
            self.cursor.execute(remFileFromSess_Qry, sessFile)
            self.cnx.commit()
            return True
        else:
            return False

        #'' ' {"sessionID": "<int>", "permissions":"free or restricted","type":<int> }
        #-If you don't need to change one of the configuration parameters you can ignore it, and don't put it in the string'''
    def Update_SessionConfig(self, sessNewConf):
        updtSessionConfig_Qry = "UPDATE Sessions s SET "
        endQry =" WHERE s.idSession = %(sessionID)s"

        tempSess = self.Get_Session({ "sessionID" : sessNewConf["sessionID"] })

        if tempSess is None:
            return False

        permInNewConf = "permissions" in sessNewConf.keys()
        typeInNewConf = "s_type" in sessNewConf.keys()

        if permInNewConf:
            updtSessionConfig_Qry += "s.permissions = %(permissions)s"
        if permInNewConf and typeInNewConf:
            updtSessionConfig_Qry += ', '
        if typeInNewConf:
            updtSessionConfig_Qry += "s.s_type = %(s_type)s"

        updtSessionConfig_Qry += endQry

        self.cursor.execute(updtSessionConfig_Qry, sessNewConf)
        self.cnx.commit()
        return True

    #''' {"sessionID" : <string>} '''
    def End_Session(self, endSession):
        endSess_Qry = "UPDATE Sessions s SET s.activeFlag = False, s.s_end = NOW() WHERE s.idSession = %(sessionID)s"
        checkActive_Qry = "SELECT s.* FROM sessions s WHERE s.idSession = %(sessionID)s AND s.activeFlag = True"

        self.cursor.execute (checkActive_Qry,  endSession)
        row = self.cursor.fetchone()

        if row is not None: #If session exists and active
            self.cursor.execute(endSess_Qry, endSession)
            self.cnx.commit()
            return True
        return False

    def Get_ActiveSessionFromLocal(self, LocalSess):
        getLocalSession_Qry = "SELECT * FROM LocationSession l WHERE l.localID = %(localizationID)s AND l.Flag = True "
        self.cursor.execute(getLocalSession_Qry, LocalSess)
        row = self.cursor.fetchone()
        if row is not None:
           # session = self.Get_Session({"sessionID": row[0]})
            return { "sessionID" : row[0]}
        else:
            return None

    def print_allUsers(self): #DEBUG only
        self.cursor.execute("SELECT * FROM users")
        row = self.cursor.fetchall()
        print(row)

    def print_allFiles(self): #DEBUG only
        self.cursor.execute("SELECT * FROM files")
        row = self.cursor.fetchall()
        print(row)

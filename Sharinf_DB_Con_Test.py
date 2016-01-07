from Sharinf_DB_Con import DB_Accesser

myReqHandler = DB_Accesser()

m_user={
    "email" : "JoaoAlves@gmail.com"
}

#myReqHandler.Insert_User(m_user)
#myReqHandler.print_allUsers()

m_file = {
    "link" : "https://drive.google.com/drive/my-sdcdsa",
    "owneremail" : "JoaoAlves@gmail.com",
    "name":"Mole.png"
}

#print(myReqHandler.Insert_File(m_file))
#myReqHandler.print_allFiles()
#uuid cenas" AND l.major = 12 AND l.minor = 32;
m_location = {
    "uuid" : "wedewqe4",
    "major": 12,
    "minor": 32,
    "lvirtual": 0
}
Beacon = {
"localizationID":1
}
print(myReqHandler.Insert_Location(m_location))
#print(myReqHandler.Get_Location(Beacon))

#myReqHandler.cursor.close()
#myReqHndler.cnx.close()
sess = {
    "permissions": 3,
    "activeFlag": True,
    "localizationID":1,
    "s_type":1,
    "s_name": "Sharinf_123"
}
sCreated = myReqHandler.Insert_Session(sess)

print (sCreated)

newConfig = {
    "sessionID": 1,
    "permissions":0
}
#}
#print(myReqHandler.Get_Session({"sessionID":1}))
#print(myReqHandler.Update_SessionConfig(newConfig))
#print(myReqHandler.Get_Session({"sessionID":1}))
#print(myReqHandler.End_Session({"sessionID" : 1}))

in_UserSess = {"sessionID" : 2, "email" :  m_user["email"]}
#rint(myReqHandler.Get_UserInSession(in_UserSess))
#print(myReqHandler.Get_User(m_user))
print(myReqHandler.Get_Session({"sessionID":in_UserSess["sessionID"]}))
#print(myReqHandler.Insert_UserInSession(in_UserSess))
#print(myReqHandler.Set_SessionAdmin(in_UserSess))
#print(myReqHandler.Get_AllUsersInSession({"sessionID":1}))

#print(myReqHandler.Insert_FileInSession({"sessionID":1,"File":m_file}))
#print(myReqHandler.Get_AllFilesFromSession({"sessionID":1}))
#print(myReqHandler.Get_FileFromSession({"sessionID":1,"File":m_file}))
#print(myReqHandler.Get_SessionAdmin({"sessionID":1}))
#print(myReqHandler.Remove_FileFromSession({"sessionID":1,"File":m_file}))
#print(myReqHandler.Remove_UserFromSession({"email":m_user["email"], "sessionID": 1}))

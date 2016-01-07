import socket
import SocketServer
import threading
import thread
from threading import Lock
import json
import string
import signal, os
import sys
from datetime import datetime
from time import sleep
from Sharinf_DB_Con import DB_Accesser

SUCCESS = chr(1)
INSUCESS = chr(0)

PUBLIC = 0
PRIVATE = 1
FREE = 0
RESTRICTED = 1

CREATE_CONNECTION = 0
BEACON_DETECTED = 1
CREATE_SESSION = 2
SESSION_USERLIST = 3
SESSION_FILELIST = 4
JOIN_SESSION =  5
GETOUT_SESSION = 6
CHNGE_SESSION = 7
LOAD_FILE_SESSION = 8
REMOVE_FILE_SESSION = 14
SESSION_ENDED = 9
YOURNEWADMIN = 16
REQUESTJOINSESSION = 11
UPDATE_USERS = 12
UPDATE_FILES = 15
BEACON_UNDETECTED = 17

server = None
socketlist = {}

class usersThread (threading.Thread):

    def __init__(self, sessionid):
        threading.Thread.__init__(self)
        self.sessionid = sessionid

    def run(self):

        print "starts user update for all users"
        #sleep(1)
        encoder = json.JSONEncoder()
        bd = DB_Accesser()

        users = bd.Get_AllUsersInSession({"sessionID": self.sessionid})

        data = {"sessionID": self.sessionid, "userList": users}
        datastr = encoder.encode(data)
        cmdstr = unichr(UPDATE_USERS)

        for usr in users:

            skt = socketlist[usr["email"]][0]
            mtx = socketlist[usr["email"]][2]

            sendstr = "SHARINF" + cmdstr + datastr + "\endF"

            mtx.acquire()
            n_send = skt.sendall(sendstr)
            #while(n_send is not None):
            #    n_send = skt.sendall(sendstr)
            mtx.release()

        print "users update done!"
        return

class filesThread (threading.Thread):

    def __init__(self, sessionid):
        threading.Thread.__init__(self)
        self.sessionid = sessionid

    def run(self):
        print "starts file update for all users"
        #sleep(1)
        encoder = json.JSONEncoder()
        bd = DB_Accesser()

        users = bd.Get_AllUsersInSession({"sessionID": self.sessionid})
        files = bd.Get_AllFilesFromSession({"sessionID": self.sessionid})

        data = {"sessionID": self.sessionid, "fileList": files}
        datastr = encoder.encode(data)
        cmdstr = unichr(UPDATE_FILES)

        for usr in users:

            print "update files for user " + usr["email"]

            skt = socketlist[usr["email"]][0]
            mtx = socketlist[usr["email"]][2]

            sendstr = "SHARINF" + cmdstr + datastr + "\endF"

            mtx.acquire()
            n_send = skt.sendall(sendstr)
            #while(n_send is not None):
            #    n_send = skt.sendall(sendstr)
            mtx.release()

        print "files update done!"
        return

def handler(signum, frame):
	server.shutdown()
	server.server_close()
	print "\nExiting Sharinf server...\n"
	sys.exit(0)


class sharinfFrame():

	def __init__(self, cmd, data):

		decoder = json.JSONDecoder()
		self.cmd = int(cmd)
		if(data == [] or data == ''):
			self.data = None
		else:
			try:
				self.data = decoder.decode(data)
			except:
				raise


class MyTCPHandler(SocketServer.BaseRequestHandler):

	def getUser(self):
		db = DB_Accesser()
		data = self.request.recv(5 * 1024)

		if data is None:
			return None

		if(data[:7] != "SHARINF"):
			print "No begin of frame"
			return None

		index = string.find(data,'\endF')

		if(index == -1):
			print "No end of frame"
			return None

		frame = sharinfFrame(data[7], data[8:index])
		cmdstr = unichr(CREATE_CONNECTION)

		if(frame == None or frame.cmd != CREATE_CONNECTION):
			#tempstr = "First I need a create connection request with information about the user..."
			self.request.send("SHARINF" + cmdstr + INSUCESS + "{\"error\":\"" + tempstr +  "\"}" + "\endF")
			#self.returnInsucess(CREATE_CONNECTION, "First I need a create connection request with information about the user...")
			return None

		db.Insert_User(frame.data)
		self.request.send("SHARINF" + cmdstr + SUCCESS + "\endF")
		#self.returnSuccess(CREATE_CONNECTION, None, us = frame.data["email"])

		print "getUser: " + frame.data["email"]
		return frame.data["email"]

	def beaconDetectedHandle(self, data):

		print "Beacon detected: " + self.user
		db = DB_Accesser()

		beaconlist = []
		sessionlist = []

		for beacon in data:

			location = db.Get_Location(beacon)
			while(location == None):
				db.Insert_Location(beacon)
				location = db.Get_Location(beacon)

			print location

			db.Insert_UserInLocation({"email": self.user, "localizationID": location["BeaconState"]["localizationID"]})
			beaconlist.append(location)
			tempsessid = db.Get_ActiveSessionFromLocal({"localizationID": location["BeaconState"]["localizationID"]})
			if(tempsessid is not None):
				sessionlist.append(db.Get_Session(tempsessid))
			else:
				sessionlist.append(None)

		self.returnSuccess(BEACON_DETECTED, {"BeaconList": beaconlist, "SessionList": sessionlist})

		return

	def beaconUndetectedHandle(self, data):

		print "Beacon undetected: " + self.user
		db = DB_Accesser()
		location = None

		location = db.Get_Location(beacon)

		if(location is not None):
			db.Remove_UserFromLocation({"email": self.user, "localizationID": location["BeaconState"]["localizationID"]})

		self.returnSuccess(BEACON_UNDETECTED)

		return


	def createSessionHandle(self, data):

		print "create session: " + self.user
		db = DB_Accesser()
		location = db.Get_Location(data["BeaconState"])

		if db.Get_ActiveSessionFromLocal({"localizationID": location["BeaconState"]["localizationID"]}) is not None:
			self.returnInsucess(CREATE_SESSION, "Beacon already has session active...")
			return

		if(location == None):
			self.returnInsucess("No beacon with that ID exists...")

		if(location["BeaconState"]["state"] == "Occupied"):
			self.returnInsucess("There is already a session in that beacon..")

		session = {"s_begin" : datetime.now().date(), "activeFlag":True ,
		"localizationID": location["BeaconState"]["localizationID"],
		"s_type": data["SessionConfig"]["type"],
		"permissions": data["SessionConfig"]["permissions"],"s_name": data["SessionConfig"]["s_name"]}

		sessioninfo = db.Insert_Session(session)

		if sessioninfo == None:
			self.returnInsucess(CREATE_SESSION, "Beacon nonexisting...")
			return

		db.Insert_UserInSession({"email": self.user, "sessionID": sessioninfo["sessionID"]})
		db.Set_SessionAdmin({"email": self.user, "sessionID": sessioninfo["sessionID"]})

		self.sessionIDi = sessioninfo["sessionID"]
		self.updateUsers(sessioninfo["sessionID"])
		self.returnSuccess(CREATE_SESSION, sessioninfo)
		self.sendSharinfFrame(YOURNEWADMIN, {"sessionID": sessioninfo["sessionID"]})
		return

	def joinSessionHandle(self, data):

		print "join session: " + self.user + " in session " + chr(data["sessionID"])
		db = DB_Accesser()
		sessionid = data["sessionID"]

		pubpriv = db.Get_Session({"sessionID" : sessionid})
		pubpriv = pubpriv["SessionConfig"]["type"]

		if(pubpriv == PRIVATE):
			admin = db.Get_SessionAdmin({"sessionID": sessionid})
			admin = admin["email"]
			self.sendSharinfFrame(REQUESTJOINSESSION, {"sessionID": sessionid, "email": self.user}, usr = admin)
			responseframe = self.getSharinfFrame(admin)

			if responseframe.data == INSUCESS:
				self.returnInsucess(JOIN_SESSION, "Administrar didnt accept")
				return

		result = db.Insert_UserInSession({"email": self.user, "sessionID": sessionid})

		if result is True:
			self.sessionIDi = sessionid
			self.updateUsers(sessionid)
			self.returnSuccess(JOIN_SESSION)
		else:
			self.returnInsucess(JOIN_SESSION, "It was not possible to join sesion probably beacause session does not exists or you are already there...")

		return

	def getOutSessionHandle(self, data):

		print "leave session request: " + self.user + " in session " + chr(data["sessionID"])
		db = DB_Accesser()
		sessionid = data["sessionID"]

		userisadmin = db.Get_UserInSession({"email": self.user , "sessionID": sessionid})

		if(userisadmin is None):
			self.returnInsucess(GETOUT_SESSION, "No user/session to get out...")
			return

		userisadmin = userisadmin["adminFlag"]
		db.Remove_UserFromSession({"email": self.user, "sessionID": sessionid})

		sessionusers = db.Get_AllUsersInSession({"sessionID" : sessionid})

		print "sessionuser: " + str(sessionusers)
		
		if sessionusers  == []:
			db.End_Session({"sessionID": sessionid})
	 	# elif userisadmin:
	 	# 	print "NEW ADMIN"
			# tempusr = sessionusers[0]["email"]
			# self.sendSharinfFrame(YOURNEWADMIN, {"sessionID": sessionid}, usr = tempusr)
			# #responseframe = self.getSharinfFrame(tempusr)
			# #if responseframe.data == SUCCESS:
			# db.Set_SessionAdmin({"email": tempusr, "sessionID": sessionid})

		print "leave session request DONE: " + self.user + " in session " + chr(data["sessionID"])
		self.sessionIDi = None
		self.returnSuccess(GETOUT_SESSION)

		return sessionid

	def loadFileHandle(self, data):

		print "load file: " + self.user + " in session " + chr(data["sessionID"])
		db = DB_Accesser()
		sessionid = data["sessionID"]

		permissions = db.Get_Session({"sessionID" : sessionid})
		permissions = permissions["SessionConfig"]["permissions"]

		admin = db.Get_SessionAdmin({"sessionID": sessionid})
		#admin = admin["email"]

		if permissions == RESTRICTED and self.user is not admin:
			self.returnInsucess(LOAD_FILE_SESSION, "You dont have permissions to share")
			return

		db.Insert_File(data["File"])
		result = db.Insert_FileInSession(data)


		if result is True:
			self.updateFiles(sessionid)
			self.returnSuccess(LOAD_FILE_SESSION, None)
		else:
			self.returnInsucess(LOAD_FILE_SESSION)

		return

	def removeFileHandle(self, data):

		db = DB_Accesser()

		sessionid = data["sessionID"]

		freerestr = db.Get_Session({"sessionID" : sessionid})
		freerestr = freerestr["SessionConfig"]["permissions"]

		owner = db.Get_File(data["File"])
		owner = owner["owneremail"]

		admin = db.Get_SessionAdmin({"sessionID": sessionid})
		admin = admin["email"]

		if (permissions == RESTRICTED) and (self.user is not admin) or (self.user is not owner):
			self.returnInsucess(LOAD_FILE_SESSION, "You dont have permissions to share or you are not owner")
			return

		result  = db.Remove_FileFromSession(data)

		if result is True:
			self.updateFiles(sessionid)
			self.returnSuccess(LOAD_FILE_SESSION, None)
		else:
			self.returnInsucess(LOAD_FILE_SESSION)

		return

	def userListHandle(self,data):

		db = DB_Accesser()

		users = db.Get_AllUsersInSession(data)

		datajson = {"userList": users}

		if users is not None:
			self.returnSuccess(SESSION_USERLIST, datajson)
		else:
			self.returnInsucess(SESSION_USERLIST)

	def fileListHandle(self,data):

		db = DB_Accesser()

		files = db.Get_AllFilesFromSession(data)

		datajson = {"fileList": files }

		if files is not None:
			self.returnSuccess(SESSION_FILELIST, datajson)
		else:
			self.returnInsucess(SESSION_FILELIST)

	def getSharinfFrame(self, usr = None):

		db = DB_Accesser()

		if usr == None:
			usr = self.user

		skt = socketlist[usr][0]
		mtx = socketlist[usr][1]

		try:
			mtx.acquire()
			self.data = skt.recv(5 * 1024)
			mtx.release()
		except:
			print "CONNECTION ABRUPLY TERMINATED BY " + self.user
			return None

		if self.data is None:
			return None

		print "Data received: " + self.data

		if(self.data[:7] != "SHARINF"):
			print "No begin of frame"
			return None

		index = string.find(self.data,'\endF')

		if(index == -1):
			print "No end of frame"
			return None

		try:
			returnfram = sharinfFrame(self.data[7], self.data[8:index])
			return returnfram
		except:
			print "RECEIVED BAD FRAME FROM: " + self.user
			return None

	def sendSharinfFrame(self, cmd, data, usr = None, status = None):

		db = DB_Accesser()

		if usr == None:
			usr = self.user

		cmdstr = unichr(cmd)
		if data is not None:
			datastr = self.encoder.encode(data)

		skt = socketlist[usr][0]
		mtx = socketlist[usr][2]

		try:
			mtx.acquire()
			if(status is None):
				if(data is None):
					skt.send("SHARINF" + cmdstr + "\endf")
				else:
					skt.send("SHARINF" + cmdstr + datastr + "\endf")
			else:
				if(data is None):
					skt.send("SHARINF" + cmdstr + status + "\endf")
				else:
					skt.send("SHARINF" + cmdstr + status + datastr + "\endf")
			mtx.release()
		except:
			return


	def returnSuccess(self, cmd,  data = None):

		print "Returning success"
		self.sendSharinfFrame(cmd, data, status = SUCCESS)
		return

	def returnInsucess(self, cmd, description = "ERROR"):

		print "Returning INSUCESS"
		error = { "error" : description }
		self.sendSharinfFrame(cmd, error, status = INSUCESS)
		return

	def updateUsers(self, sessionid):

		thread1 = usersThread(sessionid)
		thread1.start()
		#thread.start_new_thread(sendFilesUpdateForSession, (sessionid,))

		return

	def updateFiles(self, sessionid):

		thread1 = filesThread(sessionid)
		thread1.start()
		#thread.start_new_thread(sendFilesUpdateForSession, (sessionid,))

		return


	def handle(self):

		self.encoder = json.JSONEncoder()
		self.user = self.getUser()
		self.sessionIDi = None

		if(self.user == None):
			return

		socketlist[self.user] = (self.request, Lock(), Lock()) #(socket for user, Lock for receiving, Lock for sending)

		while(True):

			frame = self.getSharinfFrame()

			if frame is None:
				print "end connection with " + self.user
				if(self.sessionIDi is not None):
					self.getOutSessionHandle({"sessionID": self.sessionIDi})
				socketlist.pop(self.user)
				return

			if(frame.cmd is BEACON_DETECTED):
				self.beaconDetectedHandle(frame.data)
			elif(frame.cmd is CREATE_SESSION):
				self.createSessionHandle(frame.data)
			elif(frame.cmd is GETOUT_SESSION):
				sessionid = self.getOutSessionHandle(frame.data)
				self.updateUsers(sessionid)
			elif(frame.cmd is JOIN_SESSION):
				self.joinSessionHandle(frame.data)
			elif(frame.cmd is LOAD_FILE_SESSION):
				self.loadFileHandle(frame.data)
			elif(frame.cmd is REMOVE_FILE_SESSION):
				self.removeFileHandle(frame.data)
			elif(frame.cmd is SESSION_USERLIST):
				self.userListHandle(frame.data)
			elif(frame.cmd is SESSION_FILELIST):
				self.fileListHandle(frame.data)
			elif(frame.cmd is BEACON_UNDETECTED):
				self.beaconUndetectedHandle(frame.data)
			else:
				pass


class ThreadedTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer): pass

if __name__ == "__main__":

	signal.signal(signal.SIGINT, handler)
	signal.signal(signal.SIGTERM, handler)

	SocketServer.TCPServer.allow_reuse_address = True

	HOST, PORT = socket.gethostbyname(socket.gethostname()), 8888
	server = ThreadedTCPServer((HOST, PORT), MyTCPHandler)

	print server.server_address

	server_thread = threading.Thread(target=server.serve_forever)
    #server_thread.daemon = True
	server_thread.start()

	while(True):
   		pass

	server.shutdown()
	server.server_close()

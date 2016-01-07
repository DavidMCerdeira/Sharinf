<?php
class Users {
	public $tableName = 'Users';
	
	function __construct(){
		//database
		$dbUsername = "sharinf_db"; //Database Username
		$dbPassword = "root"; //Database Password
		$dbServer = "localhost"; //Mysql Hostname
		$dbName = "sharinf_db"; //Database Name
		
		//connect databse
		$con = mysqli_connect($dbServer,$dbUsername,$dbPassword,$dbName);
		if(mysqli_connect_errno()){
			die("Failed to connect with MySQL: ".mysqli_connect_error());
		}else{
			$this->connect = $con;
		}
	}
	
	function checkUser($email){
		$prevQuery = mysqli_query($this->connect,"SELECT COUNT(userMail) as usercount FROM Users WHERE users.userMail = '".$userMail."'") or die(mysqli_error($this->connect));
		$user_count = $prevQuery->fetch_object() -> usercount;
	}
}
?>
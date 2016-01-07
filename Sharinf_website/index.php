<?php
session_start(); //session start

require_once ('libraries/Google/autoload.php');

// Include two files from google-php-client library in controller
include_once ("libraries/Google/Client.php");
include_once ("libraries/Google/Service/Oauth2.php");

//Insert your cient ID and secret 
//You can get it from : https://console.developers.google.com/
$client_id = '452908174319-hg4h9vk2j0sgp67koa69hqskge7tovli.apps.googleusercontent.com';
$client_secret = '59TChC20Wpe-tTOjvEK8yaCT';
$redirect_uri = 'http://localhost:8880/Sharinf/file_manager/';
$simple_api_key = 'AIzaSyAtoCAmq_W5HIOSHU2J5r5wpsUMjKtNS9M';

//database
$db_username = "sharinf_db"; //Database Username
$db_password = "root"; //Database Password
$host_name = "localhost"; //Mysql Hostname
$db_name = "sharinf_db"; //Database Name

$userData = null;

//incase of logout request, just unset the session var
if (isset($_GET['logout'])) {
  unset($_SESSION['access_token']);
}

// Create Client Request to access Google API
$client = new Google_Client();
$client->setApplicationName("PHP Google OAuth Login Example");
$client->setClientId($client_id);
$client->setClientSecret($client_secret);
$client->setRedirectUri($redirect_uri);
$client->setDeveloperKey($simple_api_key);
$client->addScope("https://www.googleapis.com/auth/userinfo.email");
$client->addScope("email");
$client->addScope("profile");

$objOAuthService = new Google_Service_Oauth2($client);
$client->setScopes(array('https://www.googleapis.com/auth/userinfo.email','https://www.googleapis.com/auth/userinfo.profile'));

if (isset($_GET['code'])) {
$client->authenticate($_GET['code']);
$_SESSION['access_token'] = $client->getAccessToken();
header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
}

if (isset($_SESSION['access_token']) && $_SESSION['access_token']) {
  $client->setAccessToken($_SESSION['access_token']);
} else {$authUrl = $client->createAuthUrl();}


//Display user info or display login url as per the info we have.
echo '<div style="margin:20px">';
if (isset($authUrl)){ 
	//show login url
	echo '<div align="center">';
	echo '<h3>Login with Google -- Demo</h3>';
	echo '<div>Please click login button to connect to Google.</div>';
	echo '<a class="login" href="' . $authUrl . '"><img src="images/google-login-button.png" /></a>';
	echo '</div>';
	
	// Send Client Request
	$objOAuthService = new Google_Service_Oauth2($client);
	
	//Open a new connection to the MySQL server
	$mysqli = mysqli_connect($host_name, $db_username, $db_password, $db_name);
	mysqli_select_db($mysqli, "sharinf_db");
	
	//Output any connection error
	if ($mysqli->connect_error) {
		die('Error: ('. $mysqli->connect_errno .') '. $mysqli->connect_error);
	}
	
	// Add Access Token to Session
	if (isset($_GET['code'])) {
	$client->authenticate($_GET['code']);
	$_SESSION['access_token'] = $client->getAccessToken();
	header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
	}
	
	// Set Access Token to make Request
	if (isset($_SESSION['access_token']) && $_SESSION['access_token']) {
	$client->setAccessToken($_SESSION['access_token']);
	}
		
	// Get User Data from Google and store them in $data
	if ($client->getAccessToken()) {
	$userData = $objOAuthService->userinfo->get();
	$data['userData'] = $userData;
	$_SESSION['access_token'] = $client->getAccessToken();
	} else {
	$authUrl = $client->createAuthUrl();
	$data['authUrl'] = $authUrl;
	}
	
	echo '</div>';
	
	}
?>


<?php
// Start session
session_start();

// Include two files from google-php-client library in controller
include_once APPPATH . "libraries/google-api-php-client-master/src/Google/Client.php";
include_once APPPATH . "libraries/google-api-php-client-master/src/Google/Service/Oauth2.php";

// Store values in variables from project created in Google Developer Console
$client_id = '452908174319-hg4h9vk2j0sgp67koa69hqskge7tovli.apps.googleusercontent.com';
$client_secret = '59TChC20Wpe-tTOjvEK8yaCT';
$redirect_uri = 'http://localhost:8880/Sharinf/file_manager/';
$simple_api_key = 'AIzaSyAtoCAmq_W5HIOSHU2J5r5wpsUMjKtNS9M';

// Create Client Request to access Google API
$client = new Google_Client();
$client->setApplicationName("PHP Google OAuth Login Example");
$client->setClientId($client_id);
$client->setClientSecret($client_secret);
$client->setRedirectUri($redirect_uri);
$client->setDeveloperKey($simple_api_key);
$client->addScope("https://www.googleapis.com/auth/userinfo.email");

// Send Client Request
$objOAuthService = new Google_Service_Oauth2($client);

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
// Load view and send values stored in $data
$this->load->view('google_authentication', $data);

?>
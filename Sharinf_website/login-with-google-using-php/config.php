<?php
session_start();
include_once("src/Google_Client.php");
include_once("src/contrib/Google_Oauth2Service.php");
######### edit details ##########
$client_id = '452908174319-hg4h9vk2j0sgp67koa69hqskge7tovli.apps.googleusercontent.com';
$client_secret = '59TChC20Wpe-tTOjvEK8yaCT';
$redirect_uri = 'http://localhost:8880/login-with-google-using-php';
$homeURL = 'http://localhost:8880/login-with-google-using-php';
$simple_api_key = 'AIzaSyAtoCAmq_W5HIOSHU2J5r5wpsUMjKtNS9M';

##################################

$gClient = new Google_Client();
$gClient->setApplicationName('Login to codexworld.com');
$gClient->setClientId($client_id);
$gClient->setClientSecret($client_secret);
$gClient->setRedirectUri($redirect_uri);

$google_oauthV2 = new Google_Oauth2Service($gClient);
?>
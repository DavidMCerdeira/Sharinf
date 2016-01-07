<?php

header("Content-Type: text/json");

$url = "http://".$_SERVER["HTTP_HOST"].dirname($_SERVER["REQUEST_URI"])."/upload_handler.php";

print_r(json_encode(array(
	"parent"	=> "vaultObj",		// container for init, common for all demos
	"uploadUrl"	=> $url,		// html4/html5 upload url
	"swfUrl"	=> $url,		// flash upload url
	"slUrl"		=> $url,		// silverlight upload url, FULL path required
	"swfPath"	=> "dhxvault.swf",	// path to flash uploader
	"slXap"		=> "dhxvault.xap",	// path to silverlight uploader
	"maxFileSize"	=> min(parse_num(ini_get("upload_max_filesize")), parse_num(ini_get("post_max_size")))
)));

function parse_num($k) {
	$p = 0;
	preg_match("/(\d{1,})([kmg]?)/i", trim($k), $r);
	if (isset($r) && isset($r[1])) {
		$p = $r[1];
		if (isset($r[2])) {
			switch(strtolower($r[2])) {
				case "g": $p *= 1024;
				case "m": $p *= 1024;
				case "k": $p *= 1024;
			}
		}
	}
	return $p;
};

?>

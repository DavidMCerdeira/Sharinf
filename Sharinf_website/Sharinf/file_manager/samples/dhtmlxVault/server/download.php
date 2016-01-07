<?php
	
	if (isset($_REQUEST["fileName"])) {
		
		$fname = basename(urldecode($_REQUEST["fileName"]));
		$files = scandir("static");
		
		if (in_array($fname, $files) && is_file("static/".$fname)) {
			
			$data = file_get_contents("static/".$fname);
			
		} else {
			$fname .= ".txt";
			$data = "Your file should be here, but it wasn't uploaded to our server, since it's just a demo.\r\n".
				"To enjoy the full functionality, download dhtmlxVault.";
		}
		
		header("Content-Type: application/octet-stream");
		header("Content-Disposition: attachment; filename=\"".$fname."\"");
		header("Content-Length: ".strlen($data));
		
		print_r($data);
		
	}
	
?>
<!DOCTYPE html>
<html>
<head>
	<title>Download files</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
	<link rel="stylesheet" type="text/css" href="codebase/dhtmlxvault.css"/>
	<script src="codebase/dhtmlxvault.js"></script>
	<script src="codebase/swfobject.js"></script>
	<style>
		div.sample_title {
			font-size: 16px;
			font-family: Tahoma;
			color: #303030;
			font-weight: bold;
			margin: 15px 1px;
		}
		div#maxsize_info {
			font-size: 12px;
			font-family: Tahoma;
			color: #777;
			margin: 16px 1px 20px 1px;
		}
	</style>
	<script>
		var myVault;
		function doOnLoad() {
			window.dhx4.ajax.get("../server/upload_conf.php", function(r){
				var t = window.dhx4.s2j(r.xmlDoc.responseText);
				if (t != null) {
					myVault = new dhtmlXVaultObject(t);
					myVault.setDownloadURL("../server/download.php?fileName={serverName}");
					myVault.load("../server/get_records.php");
					// update max file size notice
					document.getElementById("maxsize_info").innerHTML = "Upload max filesize: "+myVault.readableSize(t.maxFileSize);
				}
			});
		}
	</script>
	
</head>
<body onload="doOnLoad();">
	<div class="sample_title">Download files</div>
	<div id="maxsize_info">&nbsp;</div>
	<div id="vaultObj" style="width:400px; height:250px;"></div>
</body>
</html>
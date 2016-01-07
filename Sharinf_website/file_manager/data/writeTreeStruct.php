<?php
$doc = new SimpleXMLElement('<tree id="0">
  	<item id="session<a>" text="Session<b>" im0="folderClosed.gif"></item>
</tree>');

foreach( $doc->xpath('b[@id="id2"]') as $b ) {
  $b = dom_import_simplexml($b);
  $cdata = $b->ownerDocument->createCDataSection('0<>1');
  $b->appendChild($cdata);
  unset($b);
}

$doc->asxml("treeStruct.xml") 

?>
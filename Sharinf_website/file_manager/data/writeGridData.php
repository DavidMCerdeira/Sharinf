<?php    
    /* create a dom document with encoding utf8 */
    $domtree = new DOMDocument('1.0', 'UTF-8');

    /* create the root element of the xml tree */
    $xmlRoot = $domtree->createElement("xml");
    /* append it to the document created */
    $xmlRoot = $domtree->appendChild($xmlRoot);

    $currentRow = $domtree->createElement("Rows");
    $currentRow = $xmlRoot->appendChild($currentRow);

	$currentRows = $domtree->createElement("row");
    $currentRows = $currentRow->appendChild($currentRows);
	
    /* you should enclose the following two lines in a cicle */
    $currentRows->appendChild($domtree->createElement('cell','../icons/grid_folder.png'));
    $currentRows->appendChild($domtree->createElement('cell','Session1'));
    $currentRows->appendChild($domtree->createElement('cell','File folder'));
    $currentRows->appendChild($domtree->createElement('cell','2013-10-07 18:59'));
	$currentRows->appendChild($domtree->createElement('cell',' '));
	
    /* get the xml printed */
    echo $domtree->saveXML();
	$domtree->save("gridData.xml") 
?>
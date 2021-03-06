<!DOCTYPE html>
<html>
<head>
    <title>File manager</title>
    <script src="codebase/dhtmlx.js"></script>
    <link rel="STYLESHEET" type="text/css" href="codebase/dhtmlx.css">
    <style>
        html, body {
            width: 100%;
            height: 100%;
            overflow: hidden;
            margin: 0px;
            background-color: #EBEBEB;
        }
    </style>

    <script>
       		dhtmlxEvent(window,"load",function(){ 	                         //provides your script as a handler of the 'onload' HTML event
            
			//layout
            var myLayout = new dhtmlXLayoutObject(document.body,"2U"); 	//initializes dhtmlxLayout
            myLayout.cells("a").setWidth(250);                         	//sets the width of the 'tree' column
            myLayout.cells("a").setText("Sessions");                  	//sets the text in the header of the 'tree' column
            myLayout.cells("b").hideHeader();                          	//hides the header of the 'grid' column

            //toolbar
            var myToolbar = myLayout.attachToolbar();             	    //initializes dhtmlxToolbar
            myToolbar.setIconsPath("icons/");                           //sets the path to custom images specified for the toolbar's items
            myToolbar.loadXML("data/toolbarStruct.xml");                //loads items from the 'data/toolbarStruct.xml' file to the toolbar                       	                
			
            //tree
            var myTree = myLayout.cells("a").attachTree();            	//initializes dhtmlxTree
            myTree.setImagePath("codebase/imgs/");                    	//sets the path to the source images
            //myTree.loadXML("data/treeStruct.xml");                   	//loads data from the 'data/treeStruct.xml' file to the tree

            //grid
            var myGrid = myLayout.cells("b").attachGrid();            	//initializes dhtmlxGrid
            myGrid.setImagePath("codebase/imgs/");                     	//sets the path to the source images
            myGrid.setIconsPath("icons/");                             	//sets the path to custom images
            myGrid.setHeader("&nbsp;,Name,Type,Modified,id");          	//sets the header labels
            myGrid.setColTypes("img,ro,ro,ro,ro");                     	//sets the types of columns
            myGrid.setInitWidths("70,250,100,*,0");                    	//sets the initial widths of columns
            myGrid.setColAlign("center,left,left,left");               	//sets the horizontal alignment
            myGrid.init();                                             	//renders  dhtmlxGrid on the page
            myGrid.load("data/gridData.xml");                        	//loads data from the 'data/gridData.xml' file to the grid


            myTree.attachEvent("onSelect",  function(id){              	//attaches a handler function to the 'onSelect' event that fires when the user clicks on a tree's item.
                //myGrid.filterBy(4,id);       
				//myGrid.load("uploadFile.php", function);                          	
                return true;
            });

            myTree.loadXML("data/treeStruct.xml",function(){           	
                myGrid.load("data/gridData.xml", function(){           	
               	})
            });
       		
			myToolbar.attachEvent("onClick", function(id) {
				myGrid.filterBy(4,"session1");
			});
		});
    </script>
</head>
<body >
</body>
</html>
/* --------------------------------------------------------------------------------
 *
 *  specific functions for listDatasetVersions.jsp
 *
 * -------------------------------------------------------------------------------- */

/* * * *
 *  this function sets up all the functionality for this page
/*/

var downloadModal; // modal used for download dataset information/interaction box
var deleteModal; // modal used for delete dataset information/interaction box

function setupPage( ) {

	var itemDetails = createDialog(".itemDetails" , {width: 700, height: 800, title: "Dataset Version Details"});
	$("table[name='items']").find("tr.col_title").find("th").slice(0,1).addClass("headerSortDown");

	var tableRows = getRows();
	stripeAndHoverTable( tableRows );
	clickRadioButton();

	// setup click for Dataset Version row item
	tableRows.each(function(){
		//---> click functionality
		$(this).find("td").slice(0,6).click( function() {
			var version = $(this).parent("tr").attr("id").split( "|||" )[1];
			$("input[name='datasetVersion']").val( version );
			$("form[name='tableList']").submit();
		});

		$(this).find("td.details").click( function() {
			var datasetID = $(this).parent("tr").attr("id").split( "|||" )[0];
			var version = $(this).parent("tr").attr("id").split( "|||" )[1];
			$.get("/web/common/formatParameters.jsp",
				{datasetID: datasetID, 
				datasetVersion: version, 
				parameterType:"datasetVersion"},
				function(data){
					itemDetails.dialog("open").html(data);
					closeDialog(itemDetails);
			});
		});

		//---> center text 
		$(this).find("td").slice(0,1).css({"text-align" : "center"});
		$(this).find("td").slice(2,12).css({"text-align" : "center"});
	});

	setupDeleteButton("/web/datasets/deleteDataset.jsp");
	setupDownloadButton("/web/datasets/downloadDataset.jsp");
}


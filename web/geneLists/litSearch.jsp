<%--
 *  Author: Cheryl Hornbaker
 *  Created: June, 2004
 *  Description:  The web page created by this file allows the user to select values
 *	for searching literature.
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>

<%@ include file="/web/geneLists/include/geneListHeader.jsp"  %> 

<%
	log.info("in litSearch.jsp. user = " + user);

	formName = "litSearch.jsp";
	request.setAttribute( "selectedTabId", "literature" );
        extrasList.add("litSearch.js");
	//extrasList.add("createLitSearch.js");
	optionsList.add("geneListDetails");
	optionsList.add("chooseNewGeneList");
	//optionsListModal.add("createNewLitSearch");

	mySessionHandler.createGeneListActivity("Looked at literature searches", pool);
	int itemID = (request.getParameter("itemID") != null ? Integer.parseInt((String) request.getParameter("itemID")) : -99);
	if (itemID != -99) {
		log.debug("itemID = "+itemID);
		response.sendRedirect("litSearchResults.jsp?geneListID="+selectedGeneList.getGene_list_id()+"&itemID="+itemID);
	}

%>
<%@ include file="/web/common/header_adaptive_menu.jsp" %>


	<%@ include file="/web/geneLists/include/viewingPane.jsp" %>

	<div class="page-intro">
		<p> Click on a literature search name to view it, or run a new search.
		</p>
	</div> <!-- // end page-intro -->

	<%@ include file="/web/geneLists/include/geneListToolsTabs.jsp" %>

	
	<div class="dataContainer" style="padding-bottom: 70px;" >
    <B>This feature is no longer supported. Any previous results will still be available, but we will no longer support new searches.</B>
    <%
        	String header = "";
        	String columnHeader = "";
        	String msg = "";
		String button="";
		String title="";
		String createNew="";

		String type = "litSearch";
		GeneListAnalysis [] myAnalysisResults = 
			myGeneListAnalysis.getGeneListAnalysisResults(userID, selectedGeneList.getGene_list_id(), "LitSearch", pool);

    %>
		<%@ include file="/web/geneLists/include/formatAnalysisResults.jsp" %>
	</div> <!-- dataContainer -->
	<div class="deleteItem"></div>
	<div class="createLitSearch"></div>
	<script type="text/javascript">
		$(document).ready(function() {
			setupPage();
		});
	</script>


<%@ include file="/web/common/footer_adaptive.jsp" %>
  <script type="text/javascript">
    $(document).ready(function() {
	setTimeout("setupMain()", 100); 
    });
  </script>


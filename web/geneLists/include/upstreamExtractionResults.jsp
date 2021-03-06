<%--
 *  Author: Cheryl Hornbaker
 *  Created: Nov, 2006
 *  Description:  This file formats the upstream sequence files.
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>

<%@ include file="/web/common/anon_session_vars.jsp" %>
<jsp:useBean id="myGeneListAnalysis" class="edu.ucdenver.ccp.PhenoGen.data.GeneListAnalysis"/>
<jsp:useBean id="anonU" class="edu.ucdenver.ccp.PhenoGen.data.AnonUser" scope="session"/>

<%
    optionsList.add("geneListDetails");
    optionsList.add("chooseNewGeneList");

    int itemID = Integer.parseInt((String) request.getParameter("itemID"));

    log.debug("in upstreamExtractionResults. itemID = " + itemID);

    GeneListAnalysis thisGeneListAnalysis = null;
    if (userLoggedIn.getUser_name().equals("anon")) {
        thisGeneListAnalysis = myGeneListAnalysis.getAnonGeneListAnalysis(itemID, pool);
    } else {
        thisGeneListAnalysis = myGeneListAnalysis.getGeneListAnalysis(itemID, pool);
    }
    int upstreamLength = Integer.parseInt(thisGeneListAnalysis.getThisParameter("Sequence Length"));

    GeneList thisGeneList = thisGeneListAnalysis.getAnalysisGeneList();
    String upstreamDir = thisGeneList.getUpstreamDir(thisGeneList.getGeneListAnalysisDir(userLoggedIn.getUserMainDir()));
    if (userLoggedIn.getUser_name().equals("anon")) {
                    /*Date start = new Date();
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.setTime(start);
                    String datePart=Integer.toString(gc.get(gc.MONTH)+1)+
                                        Integer.toString(gc.get(gc.DAY_OF_MONTH))+
                                        Integer.toString(gc.get(gc.YEAR))+"_"+
                                        Integer.toString(gc.get(gc.HOUR_OF_DAY))+
                                        Integer.toString(gc.get(gc.MINUTE))+
                                        Integer.toString(gc.get(gc.SECOND));*/
        upstreamDir = userLoggedIn.getUserGeneListsDir() + "/" + anonU.getUUID() + "/" + thisGeneList.getGene_list_id() + "/UpstreamExtraction/";//+datePart+"/";
    }
    String upstreamFileName = thisGeneList.getUpstreamFileName(upstreamDir, upstreamLength, thisGeneListAnalysis.getCreate_date_for_filename());

    log.debug("upstreamDir = " + upstreamDir);
    log.debug("upstreamFileName = " + upstreamFileName);

    String[] upstreamResults = myFileHandler.getFileContents(new File(upstreamFileName), "withSpaces");

    if ((action != null) && action.equals("Download")) {
        String downloadPath = userLoggedIn.getUserGeneListsDownloadDir();
        String downloadFileName = downloadPath +
                thisGeneList.getGene_list_name_no_spaces() +
                "_" + upstreamLength + ".fasta.txt";
        log.debug("downloadFileName = " + downloadFileName);
        myFileHandler.copyFile(new File(upstreamFileName), new File(downloadFileName));

        request.setAttribute("fullFileName", downloadFileName);
        myFileHandler.downloadFile(request, response);
        // This is required to avoid the getOutputStream() has already been called for this response error
        out.clear();
        out = pageContext.pushBody();

        mySessionHandler.createGeneListActivity("Downloaded Upstream Results", pool);
    } else {
        mySessionHandler.createGeneListActivity("Viewed Upstream Results", pool);
    }


%>


<div class="dataContainer" style="overflow:auto;">

    <div class="title">Parameters Used:</div>
    <div class="other_actions" id="download"><img src="<%=imagesDir%>/icons/download_g.png"/><br/>Download</div>
    <table class="list_base" cellpadding="0" cellspacing="3" width="50%">
        <tr class="col_title">
            <th class="noSort">Parameter</th>
            <th class="noSort">Value</th>
        </tr>
        <tr>
            <td> Sequence Length</td>
            <td><%=upstreamLength%>
            </td>
        </tr>
    </table>

    <BR>
    <div class="brClear"></div>
    <table class="fastaTable">
        <%
            for (int i = 0; i < upstreamResults.length; i++) {
        %>
        <tr>
            <td style="font-family:courier, sans-serif; font-size:14px"><%=upstreamResults[i]%>
            </td>
        </tr>
        <%
            }
        %>
    </table>

    <form method="POST"
          action="upstreamExtractionResults.jsp"
          name="upstreamExtractionResults"
          enctype="application/x-www-form-urlencoded">

        <input type="hidden" name="action" value="">
        <input type="hidden" name="itemID" value="<%=itemID%>">
    </form>
</div>

<script type="text/javascript">
    /* * *
     *  Sets up the "Download" link click
    /*/
    $(document).ready(function () {
        $("div#download").click(function () {
            $("input[name='action']").val("Download");
            $("form[name='upstreamExtractionResults']").submit();
        });
    });
</script>


<%--
 *  Author: Spencer Mahaffey
 *  Created: May, 2016
 *  Description:  This file handles the multipart request form submitted from RGD.
 *
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>

<%@ include file="/web/common/anon_session_vars.jsp" %>

<%-- The FileHandler bean handles the multipart request containing the files to be uploaded.  --%>

<jsp:useBean id="thisFileHandler" class="edu.ucdenver.ccp.util.FileHandler" >
        <jsp:setProperty name="thisFileHandler" property="request"
                        value="<%= request %>" />
</jsp:useBean>

<jsp:useBean id="myGeneList" class="edu.ucdenver.ccp.PhenoGen.data.GeneList" > </jsp:useBean>
<jsp:useBean id="myAnonGL" class="edu.ucdenver.ccp.PhenoGen.data.AnonGeneList" > </jsp:useBean>
<jsp:useBean id="myErrorEmail" class="edu.ucdenver.ccp.PhenoGen.web.mail.Email"> </jsp:useBean>
<jsp:useBean id="anonU" class="edu.ucdenver.ccp.PhenoGen.data.AnonUser" scope="session" />

<%
	log.info("in RGD/createGeneList.jsp." );

	String description = "";
	String organism = "Rn";
	String gene_list_name = "";
	String inputGeneList = "";
        String id="";
	boolean manuallyEntered = false;

	//String additionalInfo = ""; 
        java.util.Date cur=new java.util.Date();
        String geneListDir = userFilesRoot+"tmpRGD/GeneLists/uploads/";
        
        log.debug("upload geneList dir = "+geneListDir);

        log.debug("before call to uploadFiles");
        
	// Path must be set before calling uploadFiles
        thisFileHandler.setPath(geneListDir);
	thisFileHandler.uploadFiles();

	Vector parameterNames = thisFileHandler.getParameterNames(); 
	Vector parameterValues = thisFileHandler.getParameterValues(); 
	//
	// Parameters have to be parsed this way because the form
	// is multi-part/data
	//
	for (int i=0; i<parameterNames.size(); i++) {
		String nextParam = (String) parameterNames.elementAt(i);
		String nextParamValue = (String) parameterValues.elementAt(i);
		if (nextParam.equals("id")) {  
			id = nextParamValue.trim(); 
		} 
	}


        int geneListID = -99;

        GeneList newGeneList = null;
       
        newGeneList=new AnonGeneList();
        newGeneList.setCreated_by_user_id(-20);
        
        newGeneList.setGene_list_name(id);	
        newGeneList.setDescription(description);

        newGeneList.setOrganism(organism);	
        newGeneList.setAlternateIdentifierSource("Current");	
        newGeneList.setAlternateIdentifierSourceID(-99);	


        newGeneList.setGene_list_source("RGD");	
        log.debug("About to load gene list\n\n");
        Vector fileNames = thisFileHandler.getFilenames(); 
        log.debug("FILESIZE"+fileNames.size());
        //Vector fileParameterNames = thisFileHandler.getFileParameterNames(); 
        for (int i=0; i<fileNames.size(); i++) {
                //String nextParam = (String) fileParameterNames.elementAt(i);
                String nextFileName = (String) fileNames.elementAt(i);
                log.debug("nextFileName = "+nextFileName);

                //if (nextParam.equals("filename")) {
                        int numberOfLines = 0;
                        String geneListFileName = geneListDir + nextFileName;
                        File geneListFile = new File(geneListFileName);
                        log.debug("geneListFileName = "+geneListFileName);
                        // 
                        // If the length of the file that was created on the server is 0,
                        // the remote file must not have existed.
                        //
                        if (geneListFile.length() == 0) {
                                geneListFile.delete();	
//						newGlDir.delete();
                                //Error - "File does not exist"
                                session.setAttribute("errorMsg", "GL-002");
                                response.sendRedirect(commonDir + "errorMsg.jsp");
                        } else {
                                log.debug("geneListOrigFileName = "+nextFileName);
                                try {
                                        geneListID = newGeneList.loadFromFile(0,geneListFileName, pool); 
                                        log.debug("successfully uploaded gene list");
                                        /*additionalInfo = "The file "+
                                                nextFileName + 
                                                " has been <strong>successfully</strong> uploaded.<br>";*/
                                 
                                        if(userLoggedIn.getUser_name().equals("anon")){
                                            log.debug("link gene list");
                                            myAnonGL.linkRGDListToRGDUser(id,geneListID,pool);
                                        }
                                        /*String genesNotFound = myGeneList.checkGenes(geneListID, pool);
                                        if (genesNotFound.length() > 0) {
                                                additionalInfo = additionalInfo + 
                                                        "<BR>However, the following genes were not recognized in our database "+
                                                        "for the organism you specified ("+organism+"):<BR><BR>"+
                                                        genesNotFound.replaceAll("\n", "<BR>") +
                                                        "<BR><BR>  (Note: The gene identifiers are case-sensitive) <BR><BR>"+
                                                        "A message "+
                                                        "has been sent to the administrator to investigate.";
                                                myErrorEmail.setSubject("Gene identifier(s) not found");
                                                myErrorEmail.setContent(userName + " tried to create or upload the following identifiers "+
                                                                        "which were not found by iDecoder for "+
                                                                                "the organism " + organism + ": \n\n" + 
                                                                                genesNotFound +
                                                                                "\n\n  The gene list ID is: "+geneListID);
                                                try {
                                                        myErrorEmail.sendEmailToAdministrator(adminEmail);
                                                } catch (Exception error) {
                                                        log.error("exception while trying to send message to phenogen.help about "+
                                                                        "genes not found by iDecoder", error);
                                                }
                                        }*/
                                        //Success -- file uploaded
                                        /*session.setAttribute("additionalInfo", additionalInfo);
                                        session.setAttribute("successMsg", "GL-013");
                                        session.setAttribute("selectedGeneList", newGeneList);
                                        session.removeAttribute("errorMsg");
                                        session.removeAttribute("gene_list_name");
                                        session.removeAttribute("description");
                                        session.removeAttribute("organism");
                                        session.removeAttribute("inputGeneList");
                                        response.sendRedirect(geneListsDir + "listGeneLists.jsp");*/
                                } catch (SQLException e) {
                                    e.printStackTrace(System.err);
                                        log.error("did not successfully upload gene list. e.getErrorCode() = " + e.getErrorCode());
                                        
                                        if (e.getErrorCode() == 1) {
                                                log.debug("got duplicate entry error trying to insert genes record.");
                                                //Error - "Duplicate gene identifiers"
                                                session.setAttribute("errorMsg", "GL-003");
                                                response.sendRedirect(commonDir + "errorMsg.jsp");
                                        } else {
                                                throw e;
                                        }
                                }
                        }
                //}
        }
%>


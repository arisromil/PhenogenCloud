


<%--
 *  Author: Spencer Mahaffey
 *  Created: May, 2013
 *  Description:  This file takes the input from an ajax request and returns a json object with genes.
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>

<%@ page language="java"
import="org.json.*" %>

<%@ include file="/web/common/anon_session_vars.jsp"  %>

<jsp:useBean id="gdt" class="edu.ucdenver.ccp.PhenoGen.tools.analysis.GeneDataTools" scope="session"> </jsp:useBean>
<jsp:useBean id="bt" class="edu.ucdenver.ccp.PhenoGen.tools.analysis.BrowserTools" scope="session"> </jsp:useBean>

<%

bt.setSession(session);

int id=-1;
if(request.getParameter("viewID")!=null){
		id=Integer.parseInt(FilterInput.getFilteredInput(request.getParameter("viewID")));
}

if(id>0){
	bt.addViewCount(id);
}

response.setContentType("application/json");
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setDateHeader("Expires", 0);
%>
{"status":"complete"}




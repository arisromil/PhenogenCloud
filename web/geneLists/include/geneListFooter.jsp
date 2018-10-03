<%--
 *  Author: Spencer Mahaffey
 *  Created: Feb, 2016
 *  Description:  This file sets javascript to initialize Gene List pages.
 *
 *  Todo: 
 *  Modification Log:
 *      
--%>
<%
if(userLoggedIn.getUser_name().equals("anon")){
%>
<script type="text/javascript">
    $(document).ready(function() {
        var PhenogenAnonSession = SetupAnonSession();
        PhenogenAnonSession.setupSession(setupGeneLists);
        geneListjs.setupLinkEmail();
    });
</script>
<%}%>


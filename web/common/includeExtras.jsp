<%
	ArrayList extrasAttributes = (ArrayList)request.getAttribute( "extrasList" );

	if ( extrasAttributes != null ) {
		for ( int i = extrasAttributes.size()-1; i >= 0 ; i-- ) {
			String extra = (String) extrasAttributes.get(i);
			if ( extra.indexOf( ".css" ) > 0 ) {
				extra="/css/"+extra;%>
				<style>
					<jsp:include page="<%=extra%>" flush="true" />
				</style>
			<%} else if ( extra.indexOf( ".js" ) > 0 ) {
				extra = "/javascript/" + extra;%>

				<script>
					<jsp:include page="<%=extra%>" flush="true" />
				</script>
			<%}%>

		<%}
	}
%>

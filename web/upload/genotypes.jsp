<html><body><%@ include file="templateHeader.jsp" %><table><%
	edu.ucdenver.ccp.PhenoGen.data.Array.ArrayCount[] myCounts = myArray.getGenotypes("All", "Single", pool);
	for (int i=0; i<myCounts.length; i++) {
		if (!myCounts[i].getCountName().equals("No Value Entered")) {
			%><tr><td><%=myCounts[i].getCountName()%></td></tr><%
		}
	}
	%>
</table></body></html>

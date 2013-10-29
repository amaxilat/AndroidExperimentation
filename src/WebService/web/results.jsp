<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Plugin" %>
<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Result" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>
<%
    String expId=request.getParameter("id");
    Integer eId=null;
    try {
        eId=Integer.valueOf(expId);
    }catch (Exception e){
        eId=null;
    }

%>
<h3>Results for Experiment:<%=eId%></h3>

<div class="datagrid">
    <table border="1">
        <thead>
        <tr>
            <th>Id</th>
            <th>Timestamp</th>
            <th>Reporting Device</th>
            <th>Message</th>

        </tr>
        </thead>
        <%
            List<Result> results = ModelManager.getResults(eId);
            for (Result result : results) {
                out.print("<tr>");
                out.print("<td>" + result.getId() + "</td>");
                out.print("<td>" + result.getTimestamp() + "</td>");
                out.print("<td>" + result.getDeviceId() + "</td>");
                out.print("<td>" + result.getMessage() + "</td>");
                out.print("</tr>");
            }
        %>

    </table>
</div>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
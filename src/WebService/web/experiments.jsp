<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Plugin" %>
<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h3>Plugin List:</h3>
<table border="1">
    <tr>
        <td>Id</td>
        <td>Name</td>
        <td>Context Name</td>
        <td>Url-Filename-Sensor Dependencies</td>
        <td>Dataset</td>
    </tr>
    <%
        List<Experiment> experiments = ModelManager.getExperiments();
        for (Experiment exp : experiments) {
            out.print("<tr>");
            out.print("<td>" + exp.getId() + "</td>");
            out.print("<td>" + exp.getName() + "</td>");
            out.print("<td>" + exp.getContextType() + "</td>");
            out.print("<td>" + exp.getUrl() + "<br>");
            out.print("" + exp.getFilename() + "<br>");
            out.print("" + exp.getSensorDependencies() + "</td>");
            out.print("<td>" + "link to data...." + "</td>");
        }
    %>

</table>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
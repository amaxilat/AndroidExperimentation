<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>
<%@ page import="java.util.List" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h2>Completed Experiments</h2>

<h3>Experiment General Information</h3>

<h3>Plugins used by the experiment</h3>

<div class="datagrid">
    <table border="1">
        <thead>
        <tr>
            <th>Id</th>
            <th>Name</th>
            <th>Context Name</th>
            <th>Url-Filename-Sensor Dependencies</th>
            <th>Dataset</th>
        </tr>
        </thead>
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
                out.print("</tr>");
            }
        %>

    </table>
</div>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
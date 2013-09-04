<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h3>Plugin List:</h3>
<%
    Experiment experiment = ModelManager.getExperiment();
%>
<table border="1">
    <tr>
    <tr></Tr>
    <td>Id</td>
    <td><%= experiment.getId()%>
    </td>
    </tr>
    <tr>
        <td>Name</td>
        <td><%= experiment.getName()%>
        </td>
    </tr>
    <tr>
        <td>ContextType</td>
        <td><%= experiment.getContextType()%>
        </td>
    </tr>
    <tr>
        <td>Filename</td>
        <td><%= experiment.getFilename()%>
        </td>
    </tr>
    <tr>
        <td>Url</td>
        <td><%= experiment.getUrl()%>
        </td>
    </tr>
    <tr>
        <td>Status</td>
        <td><%= experiment.getStatus()%>
        </td>
    </tr>
    <tr>
        <td>SensorDependencies</td>
        <td><%= experiment.getSensorDependencies()%>
        </td>
    </tr>

    </tr>


</table>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
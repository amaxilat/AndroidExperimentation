<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>
<%@ page import="java.util.Date" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h2>Current Experiment</h2>

<%
    Experiment experiment = ModelManager.getExperiment();
    if (experiment != null) {
%>

<h3>Experiment General Information</h3>

<div class="datagrid">
    <table>
        <thead>
        <tr>
            <th>Actual running time</th>
            <td><%= (new Date(System.currentTimeMillis() - experiment.getTimestamp())).toString()%>
            </td>
        </tr>
        <tr>
            <th>Submitted at</th>
            <td><%= experiment.getTimestamp()%>
            </td>
        </tr>
        <tr>
            <th>Number of Reported Readings</th>
            <td><%= ModelManager.getResults(experiment.getId()).size()%>
            </td>
        </tr>
        </thead>
    </table>

</div>


<h3>Experiment Information</h3>

<div class="datagrid">
    <table>
        <thead>
        <tr>
            <th>Id</th>
            <td><%= experiment.getId()%>
            </td>
        </tr>
        <tr>
            <th>Name</th>
            <td><%= experiment.getName()%>
            </td>
        </tr>
        <tr>
            <th>Context Type</th>
            <td><%= experiment.getContextType()%>
            </td>
        </tr>
        <tr>
            <th>Filename</th>
            <td><%= experiment.getFilename()%>
            </td>
        </tr>
        <tr>
            <th>Url</th>
            <td><%= experiment.getUrl()%>
            </td>
        </tr>
        <tr>
            <th>Status</th>
            <td><%= experiment.getStatus()%>
            </td>
        </tr>
        <tr>
            <th>Sensor Dependencies</th>
            <td><%= experiment.getSensorDependencies()%>
            </td>
        </tr>
        <tr>
            <th>Description</th>
            <td><%= experiment.getDescription()%>
            </td>
        </tr>
        </tr>

        </thead>
    </table>
</div>
<%
} else{
%>
<h3>No Active Experiment</h3>

<%
    }
%>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
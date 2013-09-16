<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>

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
%>

<h3>Experiment General Information</h3>

<div class="datagrid">
    <table>
        <thead>
        <tr>
            <th>Description</th>
            <td></td>
        </tr>
        <tr>
            <th>Actual running time</th>
            <td></td>
        </tr>
        <tr>
            <th>Submitted at</th>
            <td></td>
        </tr>
        <tr>
            <th>Data size produced</th>
            <td></td>
        </tr>
        </thead>
     </table>

</div>


<h3>Plugins used by the current experiment</h3>

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

        </tr>

        </thead>
    </table>
</div>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
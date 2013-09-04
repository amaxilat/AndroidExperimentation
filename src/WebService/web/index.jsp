<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Plugin" %>
<%@ page import="java.util.List" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<%
    List<Plugin> plugins = ModelManager.getPluginList();
    for (Plugin plugin : plugins) {
        out.print(plugin.getName() + "<br>");

    }
    out.print("<br>");
    out.print("<br>");

    Experiment experiment = ModelManager.getExperiment();
    if (experiment != null) {
        out.print(experiment.getName());
        out.print(" - ");
        out.print(experiment.getId());
        out.print(" - ");
        out.print(experiment.getContextType());
        out.print(" - ");
    }
%>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
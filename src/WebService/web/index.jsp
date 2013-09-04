<%@ page import="eu.smartsantander.androidExperimentation.entities.Plugin" %>
<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>

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
%><%=plugin.getName() + "<br>"%> <%
    }
%>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
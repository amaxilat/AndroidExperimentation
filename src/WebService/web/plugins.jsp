<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Plugin" %>
<%@ page import="java.util.List" %>

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
        <td>Url-Filename-Factory Class</td>

    </tr>
    <%
        List<Plugin> plugins = ModelManager.getPluginList();
        for (Plugin plugin : plugins) {
            out.print("<tr>");
            out.print("<td>" + plugin.getId() + "</td>");
            out.print("<td>" + plugin.getName() + "</td>");
            out.print("<td>" + plugin.getContextType() + "</td>");
            out.print("<td>" + plugin.getInstallUrl() + "<br>");
            out.print("" + plugin.getFilename() + "<br>");
            out.print("" + plugin.getRuntimeFactoryClass() + "</td>");
        }
    %>

</table>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
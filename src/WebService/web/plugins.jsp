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

<div class="datagrid">
    <table border="1">
        <thead>
        <tr>
            <th>Id</th>
            <th>Name</th>
            <th>Context Name</th>
            <th>Url-Filename-Factory Class</th>

        </tr>
        </thead>
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
                out.print("</tr>");
            }
        %>

    </table>
</div>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
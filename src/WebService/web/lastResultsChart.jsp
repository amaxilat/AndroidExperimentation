<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Reading" %>

<%@ page import="eu.smartsantander.androidExperimentation.entities.Result" %>
<%@ page import="java.util.List" %>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
    <link href="./style/chart.css" rel="stylesheet" type="text/css">
    <!--[if lte IE 8]>
    <script language="javascript" type="text/javascript" src="./js/excanvas.min.js"></script><![endif]-->
    <script language="javascript" type="text/javascript" src="./js/jquery.js"></script>
    <script language="javascript" type="text/javascript" src="./js/jquery.flot.js"></script>
    <script language="javascript" type="text/javascript" src="./js/jquery.flot.time.js"></script>
    <script type="text/javascript">
        <%
            String expId = request.getParameter("id");
            String sensor = request.getParameter("sensor");
            String deviceId = request.getParameter("device");
            Integer eId = null;
            Integer dId = null;
            try {
                eId = Integer.valueOf(expId);
                dId = Integer.valueOf(deviceId);
            } catch (Exception e) {
                eId = null;  dId=null;
            }
            List<Result> results = ModelManager.getLastResults(eId);
            out.print("var d=[]; \n");
            out.print("$(function() {");
                for (Result result : results) {
                    Reading r;
                    try{
                        r = Reading.fromJson(result.getMessage());
                     }catch(Exception e){
                        e.printStackTrace();
                        continue;
                     }
                    if (r.getContext().equals(sensor) && result.getDeviceId()==dId)
                        out.print("d.push(["+ r.getTimestamp()+","+r.getValue() + "]); \n");
                }

                String data="$.plot(\"#placeholder\", [";
                data+="{data: d, lines: { show: true, fill: true }	}";
                data+="],{xaxis: {mode:'time',timeformat: '%y/%m/%d %H:%M:%S'}} );";
                out.print(data);
            out.print("});");
      %>
    </script>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>
<h3>Results for Experiment:<%=eId%>
</h3>

<div class="datagrid">
    <table border="1">
        <thead>
        <tr>
            <th>ExperimentId</th>
            <th>DeviceId</th>
            <th>Sensor</th>
        </tr>
        </thead>
        <%
            out.print("<tr>");
            out.print("<td>" + eId + "</td>");
            out.print("<td>" + dId + "</td>");
            out.print("<td>" + sensor + "</td>");
            out.print("</tr>");
        %>

    </table>

    <div class="demo-container">
        <div id="placeholder" class="demo-placeholder"></div>
    </div>
</div>
<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
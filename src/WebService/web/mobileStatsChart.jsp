<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Flot Examples: Real-time updates</title>
<link href="./style/chart.css" rel="stylesheet" type="text/css">
<script language="javascript" type="text/javascript" src="./js/chart-js.js"></script>
<script language="javascript" type="text/javascript" src="./js/jquery.js"></script>

<script type="text/javascript">

    function httpGet(expId, tstamp, devId) {
        var theUrl = "http://blanco.cti.gr:8080/dataStatsRaw.jsp?&tstamp=" + tstamp + "&devId=" + devId;
        //http://blanco.cti.gr:8080/dataRaw.jsp?id=4&tstamp=0
        var xmlHttp = null;
        xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", theUrl, false);
        xmlHttp.send(null);
        return xmlHttp.responseText;
    }


    function doSomeStuff() {
        var now = new Date();
        var startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        var timestamp = startOfDay / 1000;

    }



</script>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
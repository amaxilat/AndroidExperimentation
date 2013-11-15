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

    function httpGet(tstamp, devId) {
        
		// call dataStatsRaw to get the data for a specific device after the given timestamp
		// example link: http://blanco.cti.gr:8080/dataStatsRaw.jsp?tstamp=0&devId=53
		var theUrl = "http://blanco.cti.gr:8080/dataStatsRaw.jsp?&tstamp=" + tstamp + "&devId=" + devId;
        
		var xmlHttp = null;
        xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", theUrl, false);
        xmlHttp.send(null);
        return xmlHttp.responseText;
    }

	$(function () {


	<%
		String deviceID = request.getParameter("devId");
		String time = request.getParameter("tstamp");

		Integer dId = null;
		Integer tS = null;
	
	// deviceID and timeStamp variables
	
		try {
			dId = Integer.valueOf(deviceID);
			tS = Integer.valueOf(time);
			out.print("var dId=" + dId.toString()+";");
			out.print("var tS=" + tS.toString()+";");
		} catch (Exception e) {
        

		}
	%>
	
		var now = new Date();
		var startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
		var timestamp = startOfDay / 1000;

        var plotStatsData;


		function getData() {

                        var jsonDataArray = httpGet(timestamp, dId);

                        var labelsArray = [];
                        var datasetsArray = [];

                        for (var i=0; i<=12; i+2) {
                            labelsArray[i] = jsonDataArray[i];
                            datasetsArray[i] = jsonDataArray[i+1];
                        }

                        plotStatsData= {
                            labels : labelsArray,

                            datasets : [
                            {
                            fillColor : "rgba(220,220,220,0.5)",
                            strokeColor : "rgba(220,220,220,1)",
                            data : datasetsArray
                            }
                            ]
                        }
                        
						
        }	

    	function update() {
		
			var ctx = document.getElementById("statsChart").getContext("2d");
			var myNewChart = new Chart(ctx).Bar(data);
                   
        }

        update();
    }

    );

</script>
<canvas id="statsChart" width="450" height="450"></canvas>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
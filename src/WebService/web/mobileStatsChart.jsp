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


	$(function () {


	<%

		String time = request.getParameter("tstamp");
		String deviceID = request.getParameter("devId");

		Integer dId = null;
		Long tS = null;
	
	// deviceID and timeStamp variables
	
		try {
			dId = Integer.valueOf(deviceID);
			tS = Long.valueOf(time);
			out.print("var dId=" + dId.toString()+";");
			out.print("var tS=" + tS.toString()+";");
		} catch (Exception e) {
              out.print("alert(\"PROBLEMM \") ");

		}
	%>
	
		var now = new Date();
		var startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
		var timestamp = startOfDay.getTime();

        var plotStatsData;

        function httpGet(tstamp, devId) {


                    // call dataStatsRaw to get the data for a specific device after the given timestamp
                    // example link: http://blanco.cti.gr:8080/dataStatsRaw.jsp?tstamp=0&devId=53
                    var theUrl = "http://localhost:8080/dataStatsRaw.jsp?&tstamp=" + tstamp + "&devId=" + devId;


                    var xmlHttp = null;
                    xmlHttp = new XMLHttpRequest();

                    xmlHttp.open("GET", theUrl, false);
                    xmlHttp.send();

                    return xmlHttp.responseText;
                }

		function getData() {

            var jsonDataArray = httpGet(timestamp, dId);

            var labelsArray = [];
            var datasetsArray = [];

            var dataArray = JSON.parse(jsonDataArray);


            var tc = 0;

            for (var i=0; i<=12; i+=2) {
                labelsArray[tc] = dataArray[i];
                datasetsArray[tc] = dataArray[i+1];
                tc++;
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


            getData();
			var ctx = document.getElementById("statsChart").getContext("2d");
			var myNewChart = new Chart(ctx).Bar(plotStatsData);
                   
        }


        update();
    }

    );

</script>
<canvas id="statsChart" width="450" height="450"></canvas>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
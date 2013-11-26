<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<html>
<head>
    <title>Android Experimentation - SmartSantander</title>
</head>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Flot Examples: Real-time updates</title>
    <link href="./style/chart.css" rel="stylesheet" type="text/css">
    <!--[if lte IE 8]>
    <script language="javascript" type="text/javascript" src="./js/excanvas.min.js"></script><![endif]-->
    <script language="javascript" type="text/javascript" src="./js/jquery.js"></script>
    <script language="javascript" type="text/javascript" src="./js/jquery.flot.js"></script>
    <script language="javascript" type="text/javascript" src="./js/jquery.flot.time.js"></script>

    <script type="text/javascript">

        function httpGet(expId, tstamp, devId) {
            var theUrl = "http://blanco.cti.gr:8080/dataRaw.jsp?id=" + expId + "&tstamp=" + tstamp + "&devId=" + devId;
            //http://blanco.cti.gr:8080/dataRaw.jsp?id=4&tstamp=0
            var xmlHttp = null;
            xmlHttp = new XMLHttpRequest();
            xmlHttp.open("GET", theUrl, false);
            xmlHttp.send(null);
            return xmlHttp.responseText;
        }

        $(function () {

                    var tt = 0;
                    var res = [];

                    <%
                     String expId = request.getParameter("id");
                     String dev = request.getParameter("devId");
                     String sensor = request.getParameter("sensor");

                     Integer eId = null;
                     Integer devId = null;
                     try {
                         eId = Integer.valueOf(expId);
                         devId=Integer.valueOf(dev);
                         out.print("var eId="+eId.toString()+";");
                         out.print("var devId="+devId.toString()+";");
                     } catch (Exception e) {
                         eId = null;
                     }

                 %>
                    var sensor ='<%=sensor%>';

                    function getData() {
                        var jsonDataArray = httpGet(eId, tt, devId);
                        if (jsonDataArray.length == 0) {
                            return res;
                        }
                        var dataArray = JSON.parse(jsonDataArray);
                        if (dataArray.length == res.length) return res;

                        res = [];
                        for (var i = 0; i < dataArray.length; i++) {
                            if (dataArray[i].length == 0) continue;
                            var object = JSON.parse(dataArray[i]);
                            if (object.context!=sensor)continue;
                            if (object.type=='String')continue;
                            res.push([object.timestamp, object.value])
                        }
                        return res;
                    }

                    var updateInterval = 15000;
                    var plot = $.plot("#placeholder", [ getData() ], {
                        series: {
                            shadowSize: 0
                        },
                        yaxis: {
                            min: 0,
                            max: 120
                        },
                        xaxis: {
                            mode: 'time',
                            timeformat: '%y/%m/%d %H:%M:%S'
                        }
                    });

                    function update() {
                        plot.setData([getData()]);
                        plot.setupGrid()
                        plot.draw();
                        setTimeout(update, updateInterval);
                    }

                    update();
                }

        );
    </script>
</head>

<body>
<jsp:include page="./includes/header.html" flush="true"/>
<div id="content">
    <div class="demo-container">
        <div id="placeholder" class="demo-placeholder"></div>
    </div>
</div>
<br>
<br>
<br>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>

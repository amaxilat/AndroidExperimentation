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

                    var data = [], totalPoints = 1000, tt = 0;


   <%
    String expId = request.getParameter("id");
    String dev = request.getParameter("devId");

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

                    function getData() {
                        if (data.length > 0)
                            data = data.slice(1);


                        var jsonDataArray = httpGet(eId, tt, devId);
                        if (jsonDataArray.length==0) return;
                        var dataArray = JSON.parse(jsonDataArray);
                        alert(dataArray.length);
                        for (var i = 0; i < dataArray.length; i++) {
                            var object = JSON.parse(dataArray[i]);
                            data.push(object.value);
                        }
                        // Zip the generated y values with the x values
                        var res = [];
                        for (var i = 0; i < data.length; ++i) {
                            res.push([i, data[i]])
                        }

                        tt = (new Date()).getTime();
                        return res;
                    }

                    // Set up the control widget

                    var updateInterval = 30;
                    $("#updateInterval").val(updateInterval).change(function () {
                        var v = $(this).val();
                        if (v && !isNaN(+v)) {
                            updateInterval = +v;
                            if (updateInterval < 1) {
                                updateInterval = 1;
                            } else if (updateInterval > 5000) {
                                updateInterval = 5000;
                            }
                            $(this).val("" + updateInterval);
                        }
                    });

                    var plot = $.plot("#placeholder", [ getData() ], {
                        series: {
                            shadowSize: 0	// Drawing is faster without shadows
                        },
                        yaxis: {
                            min: 0,
                            max: 2
                        },
                        xaxis: {
                            show: false
                        }
                    });

                    function update() {
                        plot.setData([getData()]);
                        // Since the axes don't change, we don't need to call plot.setupGrid()
                        plot.draw();
                        setTimeout(update, updateInterval);
                    }

                    update();
                    // Add the Flot version string to the footer
                    $("#footer").prepend("Flot " + $.plot.version + " &ndash; ");
                }

        )
        ;
    </script>
</head>

<body>
<jsp:include page="./includes/header.html" flush="true"/>
<div id="content">

    <div class="demo-container">
        <div id="placeholder" class="demo-placeholder"></div>
    </div>
    <p>You can update a chart periodically to get a real-time effect by using a timer to insert the new data in the plot
        and redraw it.</p>

    <p>Time between updates: <input id="updateInterval" type="text" value="" style="text-align: right; width:5em">
        milliseconds</p>
</div>
<br>
<br>
<br>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>

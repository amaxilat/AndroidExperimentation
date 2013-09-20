<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h2>Showcase scenarios</h2>

<h3>Wardriving with smarpthones</h3>

This scenario involves the use of smartphones to record all public access WiFi networks spread out in the center of a city. As the volunteers move throughout the city centre, their smartphones scan for WiFi signals and combine them with GPS data.
Read more about it <a href="./wardriving.jsp">here</a>.

<h3>Monitoring noise levels in the city center</h3>

In this case, we use the microphones integrated in the volunteers' smartphones to gather information about the levels of noise in the center of a city.
Read more about it <a href="./noise.jsp">here</a>.

<h3>Environmental monitoring with Arduino sensors</h3>

Smartphones can communicate with Bluetooth or WiFi-equipped external sensor nodes to gather their readings and then pass them on to the rest of the system.
Read more about it <a href="./environmental.jsp">here</a>.

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Android Experimentation - SmartSantander</title>
</head>
<script src="./js/jquery.js"></script>
<script src="./js/jquery.cycle2.min.js"></script>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<div class="artPpost">

<div class="cycle-slideshow"
	data-cycle-fx="scrollHorz"
    data-cycle-pause-on-hover="true"
    data-cycle-speed="200"
>
    <img width="100%" src="./images/intro.jpg">
    <img width="100%" src="./images/intro2.jpg">
    <img width="100%" src="./images/intro3.jpg">
</div>

<p></p>
Android Experimentation is related to the <a href="http://smartsantander.eu">SmartSantander</a> research project, aiming to provide an Android smartphone
application augmenting the functionality of an existing Future Internet infrastructure.
</p>
<p>
The Android software component distributes executable code on smartphone devices of a network of volunteers. The code
is deployed as Ambient Dynamix plugins, performs calculations and produces sensor results, that are uploaded to a central
server. There is a central software component responsible for distributing the plugins and collecting the results.
</p>

<p>
For more information regarding SmartSantander you can visit:
<br>
<a href="http://smartsantander.eu">http://smartsantander.eu</a>
<br>
<p>
For more information regarding the Ambient Dynamix framework you can visit:
<br>
<a href="http://ambientdynamix.org">http://ambientdynamix.org</a>

</div>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
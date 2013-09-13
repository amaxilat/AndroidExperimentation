<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Android Experimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>
<h2>What is SmartSantander?</h2>
<p>
<a herf="http://smartsantander.eu">SmartSantander</a> proposes a unique in the world city-scale experimental research facility in support of typical applications and services for a smart city. This unique experimental facility will be sufficiently large, open and flexible to enable horizontal and vertical federation with other experimental facilities and stimulates development of new applications by users of various types including experimental advanced research on IoT technologies and realistic assessment of users’ acceptability tests. The project envisions the deployment of 20,000 sensors in Belgrade, Guildford, Lübeck and Santander (12,000), exploiting a large variety of technologies.

<p>
Below you can see an interactive map containing the currently deployed nodes in the center of Santander. You can click on any of the nodes to see live data regarding luminocity, noise levels, etc.
<p>
<iframe src="http://www.smartsantander.eu/map/" style="border: 0; width:980px; height:450px;"></iframe>

<h2>What is Android Experimentation?</h2>
<p>
Android Experimentation is a participatory sensing component for SmartSantander, aiming to augment the functionality of the testbed with new application use-cases and also provide a broader area coverage the one currently provided by the existing infrastructure.
<p>
The main concept is to have a number of volunteers install a smartphone application on their devices, which allows for automatic registration of the devices and their sensing capabilities in the SmartSantander system. The system uses a multi-layer architecture, enabling the core of the SmartSantander system to communicate with Android smartphones for:

<ul>
<li>- sending "experiments" in the form of executable code to the volunteers' smartphones, which utilize the integrated sensors, computing and communication resources,</li>
<li>- producing data traces that are subsequently uploaded to the SmartSantander data repository.</li>
</ul>
<p>
The smartphone software component is partially based on the <a href="http://ambientdynamix.org">Ambient Dynamix</a> framework, which eases the online distribution and update of executable code in the form of plugins. Essentially, our implementation provides a means for developers to build and deploy their experimental applications to a large number of smartphone users in an almost transparent way, bypassing many hurdles and limitations usually associated with the ......


<h2>Can I join in?</h2>
<p>
Yes, you can!
<br>


<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h2><a href="./experiment.jsp">Active Experiment</a></h2>

Find out details about the experiment currently executed.

<h2><a href="./experiments.jsp">Completed Experiments</a></h2>

Find out details regarding experiments completed in the past and graphs depicting the data produced.

<h2><a href="./newExperiment.jsp">Create New Experiment</a></h2>

Create a new experiment to be executed on volunteers' smartphones and submit it to the system.

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
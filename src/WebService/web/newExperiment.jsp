<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>

<h3>Plugin List:</h3>


<h3>upload new experiment ....</h3>
Select a file to upload: <br/>

<form action="fileUpload.jsp" method="post"  enctype="multipart/form-data">

    <table>
        <tr>
            <td>Name</td>
            <td><input type="text" name="name" size="50"/></td>
        </tr>
        <tr>
            <td>contextType</td>
            <td><input type="text" name="contextType" size="50"/></td>
        </tr>
        <tr>
            <td>sensorDependencies</td>
            <td><input type="text" name="sensorDependencies" size="50"/></td>
        </tr>
        <tr>
            <td>ExperimentFile</td>
            <td><input type="file" name="file" size="50"/></td>
        </tr>


    </table>


    <br/>
    <input type="submit" value="Upload File"/>
</form>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
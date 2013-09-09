<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.Utilities" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Experiment" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.hibernate.HibernateException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.UUID" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <title>AndroidExperimentation - SmartSantander</title>
</head>
<body>
<jsp:include page="./includes/header.html" flush="true"/>
<%
    File file;
    int maxFileSize = 5000 * 1024;
    int maxMemSize = 5000 * 1024;
    ServletContext context = pageContext.getServletContext();
    String jspFilePath = context.getRealPath(request.getRequestURI()).replace('\\', '/');
    String uuid = UUID.randomUUID().toString();
    String path = jspFilePath.substring(0, jspFilePath.lastIndexOf("/")) + "/experimentRepository/" + uuid + "/";
    String url = context.getInitParameter("urlBase") + "/experimentRepository/" + uuid + "/";
    String contentType = request.getContentType();

    Boolean parameterCheckOK = true;
    String name = "";
    String contextType = "";
    String dependencies = "";
    Boolean hasFile = false;
    DiskFileItemFactory factory = new DiskFileItemFactory();
    factory.setSizeThreshold(maxMemSize);
    factory.setRepository(new File("./experimentRepository"));
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax(maxFileSize);

    try {
        List fileItems = upload.parseRequest(request);
        Iterator i = fileItems.iterator();
        while (i.hasNext()) {
            FileItem item = (FileItem) i.next();
            if (item.isFormField()) {
                String fieldName = item.getFieldName();
                String fieldValue = item.getString();
                if (fieldName.equals("name")) name = fieldValue;
                else if (fieldName.equals("contextType")) contextType = fieldValue;
                else if (fieldName.equals("sensorDependencies")) dependencies = fieldValue;
            } else {
                hasFile = true;
            }
        }

        if (Utilities.isNullOrEmpty(name) == true || Utilities.isNullOrEmpty(contextType) == true || Utilities.isNullOrEmpty(dependencies) || hasFile == false) {
            out.println("<p>Name, ContextType, sensorDepedencies, and jar-experiment are required!</p>");

        } else {

            if ((contentType.indexOf("multipart/form-data") >= 0)) {


                i = fileItems.iterator();

                File directory = new File(path);
                directory.mkdirs();
                while (i.hasNext()) {
                    FileItem item = (FileItem) i.next();

                    if (item.isFormField()) {
                        continue;
                    }

                    FileItem fi = item;
                    // Get the uploaded file parameters
                    String fieldName = fi.getFieldName();
                    String fileName = fi.getName();
                    boolean isInMemory = fi.isInMemory();
                    long sizeInBytes = fi.getSize();
                    // Write the file

                    if (fileName.lastIndexOf("\\") >= 0) {
                        file = new File(path + fileName.substring(fileName.lastIndexOf("\\")));
                    } else {
                        file = new File(path + fileName.substring(fileName.lastIndexOf("\\") + 1));
                    }
                    fi.write(file);

                    Experiment experiment = new Experiment();
                    experiment.setId(null);
                    experiment.setUserId(1);
                    experiment.setName(name);
                    experiment.setContextType(contextType);
                    experiment.setSensorDependencies(dependencies);
                    experiment.setFilename(fileName);
                    experiment.setUrl(url + fileName);
                    experiment.setFromTime(null);
                    experiment.setToTime(null);
                    experiment.setStatus("active");
                    ModelManager.saveExperiment(experiment);
                    out.println("Uploaded Filename: " + fileName + "<br>");

                }
            }
        }
    } catch (Exception ex) {
        System.out.println(ex);
        File directory = new File(path);
        directory.delete()
        ;
        out.println("Error: " + ex.getMessage() + "<br>");
    }
%>


<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
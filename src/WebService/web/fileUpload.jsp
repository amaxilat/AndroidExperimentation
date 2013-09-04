<%@ page
        import="eu.smartsantander.androidExperimentation.entities.Experiment,org.apache.commons.fileupload.FileItem, org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="javax.servlet.ServletContext" %>
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
    String url = context.getInitParameter("urlBase")+"./experimentRepository/"+uuid+"/";


    String contentType = request.getContentType();
    if ((contentType.indexOf("multipart/form-data") >= 0)) {

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(maxMemSize);
        factory.setRepository(new File("./experimentRepository"));

        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(maxFileSize);
        try {
            List fileItems = upload.parseRequest(request);
            Iterator i = fileItems.iterator();
            out.println("</head>");
            out.println("<body>");
            while (i.hasNext()) {
                FileItem fi = (FileItem) i.next();
                if (!fi.isFormField()) {
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
                    out.println("Uploaded Filename: " + fileName + "<br>");
                    Experiment experiment=new Experiment();
                    experiment.setUserId(1);

                    experiment.setName(request.getParameter("name"));
                    experiment.setContextType(request.getParameter("contextType"));
                    experiment.setSensorDependencies(request.getParameter("sensorDependencies"));

                    experiment.setFilename(fileName);
                    experiment.setUrl(url+fileName);
                    experiment.setFromTime(null);
                    experiment.setToTime(null);

                }
            }
            out.println("</body>");
        } catch (Exception ex) {
            System.out.println(ex);
        }
    } else {
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet upload</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<p>No file uploaded</p>");
        out.println("</body>");

    }
%>


<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>
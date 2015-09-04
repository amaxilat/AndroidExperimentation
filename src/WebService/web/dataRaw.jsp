<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.service.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.model.Result" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String expId = request.getParameter("id");
    String tstamp = request.getParameter("tstamp");
    String dev = request.getParameter("devId");

    Integer eId = null;
    Integer devId = null;
    Long timestamp = null;
    try {
        eId = Integer.valueOf(expId);
        timestamp = Long.valueOf(tstamp);
        devId=Integer.valueOf(dev);
    } catch (Exception e) {
        eId = null;
    }
    String readingArrayJson=(new Gson()).toJson(ModelManager.getResults(eId, timestamp, devId));
    out.print(readingArrayJson );
%>

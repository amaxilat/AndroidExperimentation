<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.service.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.model.Result" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String tstamp = request.getParameter("tstamp");
    String dev = request.getParameter("devId");

    Integer devId = null;
    Long timestamp = null;

    try {
        timestamp = Long.valueOf(tstamp);
        devId=Integer.valueOf(dev);
    } catch (Exception e) {
        out.print("oops");
    }
    out.print( (new Gson()).toJson(ModelManager.getWeeklyStats(timestamp, devId)));
%>

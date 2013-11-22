<%@ page import="org.jfree.chart.ChartFactory"        %>
<%@ page import="org.jfree.chart.ChartFrame"          %>
<%@ page import="org.jfree.chart.JFreeChart"          %>
<%@ page import="org.jfree.chart.plot.PlotOrientation"%>
<%@ page import="org.jfree.data.xy.XYDataset"         %>
<%@ page import="org.jfree.data.xy.XYSeries"          %>
<%@ page import="org.jfree.data.xy.XYSeriesCollection"%>
<%@ page import="org.jfree.chart.ChartUtilities"      %>
<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Result" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.jfree.data.category.DefaultCategoryDataset" %>

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
    String[] data = ModelManager.getWeeklyStats(timestamp, devId);

    System.out.println(data.toString());


    displayGraphI(response, data);

%>

<%!


    public void displayGraphI(HttpServletResponse resp, String[] data ) throws Exception {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (int i=0; i<=12; i+=2) {
            dataset.setValue(Integer.parseInt(data[i+1]), "Readings", data[i]);
                    }

        JFreeChart chart = ChartFactory.createBarChart("Readings created during last 7 days",
                "Date", null, dataset, PlotOrientation.VERTICAL,
                false, true, false);

        ChartUtilities.writeChartAsJPEG(resp.getOutputStream(),chart,650,350);
    }

%>
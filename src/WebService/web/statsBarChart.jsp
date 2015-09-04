<%@ page import="org.jfree.chart.ChartFactory"        %>
<%@ page import="org.jfree.chart.ChartFrame"          %>
<%@ page import="org.jfree.chart.JFreeChart"          %>
<%@ page import="org.jfree.chart.plot.PlotOrientation"%>
<%@ page import="org.jfree.data.xy.XYDataset"         %>
<%@ page import="org.jfree.data.xy.XYSeries"          %>
<%@ page import="org.jfree.data.xy.XYSeriesCollection"%>
<%@ page import="org.jfree.chart.ChartUtilities"      %>
<%@ page import="java.util.List" %>
<%@ page import="eu.smartsantander.androidExperimentation.service.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.model.Result" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.jfree.data.category.DefaultCategoryDataset" %>
<%@ page import="java.awt.*" %>

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
    for(int i=0;i<data.length;i+=2 ){
        data[i]=data[i].substring(0,data[i].length()-3);
    }
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

        chart.getCategoryPlot().getDomainAxis().setLabelPaint(Color.white);

        chart.getCategoryPlot().getRangeAxis().setLabelPaint(Color.white);

        chart.getTitle().setPaint(Color.white);
        chart.setBackgroundPaint(Color.black);

        Font f = new Font("Diagram", Font.BOLD, 22);
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(f);
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(f);
        chart.getCategoryPlot().getRangeAxis().setTickLabelPaint(Color.white);
        chart.getCategoryPlot().getDomainAxis().setTickLabelPaint(Color.white);



        ChartUtilities.writeChartAsJPEG(resp.getOutputStream(),chart,650,350);
    }

%>
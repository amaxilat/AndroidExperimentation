<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Reading" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Result" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.DriverManager" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.text.DateFormat" %>


<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Android Experimentation - SmartSantander</title>
    <script src="https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false"></script>
    <style>
        html, body, #map-canvas {
            height: 100%;
            margin: 0px;
            padding: 0px
        }
    </style>
    <script type="text/javascript">


        <%
        try {
		    Class.forName("com.mysql.jdbc.Driver");
	    } catch (ClassNotFoundException e) {
		    System.out.println("Where is your MySQL JDBC Driver?");
		    e.printStackTrace();
		return;
	}
      Connection  conn = DriverManager.getConnection("jdbc:mysql://193.144.201.33:3306/smartsantander","cti", "cti_1");
      Statement stmt = conn.createStatement ();
      ResultSet rset = stmt.executeQuery ("SELECT x.nodeId, y.longitude, y.latitude, noise FROM smartsantander.LastValues as x ,smartsantander.NodeList as y\n" +
" where noise is not null\n" +
"and x.nodeId=y.nodeId;");

     int ii=0;
     String staticMarkers="";
      while (rset.next()) {
               String deviceID=rset.getString("nodeId");
               String longig=rset.getString("longitude");
               String lat=rset.getString("latitude");
               String noise=rset.getString("noise");
               String slatlng= "var myLatlngS" + ii +"= new google.maps.LatLng("+lat+", "+longig+");\n";
                staticMarkers+=slatlng+"\n";
                staticMarkers+="var markerS"+ii +" = new google.maps.Marker({ position: myLatlngS"+ii+", map: map,title: '"+noise +"',icon: '"+ "https://maps.google.com/mapfiles/ms/icons/caution.png"+ "'});\n";
               ii++;
      }
    %>




        <%
             String expId = request.getParameter("id");
             Integer eId = null;
             try {
             eId = Integer.valueOf(expId);
             } catch (Exception e) {
             eId = null;
             }
             List<Result> results = ModelManager.getResults(eId);
             HashMap<Integer,String> deviceColors=new HashMap<Integer,String>();
             int i=1;
             String latlng="";
             Integer latLongOfDevice=null;
             String finalPrint="";
             int index=1;
             String [] gpsCenter=null;
             String zoomLevel="14";

             for (Result result : results) {
                 Reading r;
                 ModelManager.assignColor(deviceColors,result.getDeviceId());
                 try{
                 if(result.getMessage()==null || result.getMessage().length()==0) continue;
                 r = Reading.fromJson(result.getMessage());
                 String [] gps=new String[2];
                 if (r.getContext().contains("Gps")){
                  if(r.getValue()!=null && r.getValue().length()>0){
                      gps[0]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[0]));
                      gps[1]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[1]));
                      if (gpsCenter==null){
                          gpsCenter=new String[2];
                          gpsCenter[0]=gps[0];
                          gpsCenter[1]=gps[1];
                      }
                   }
                  }
                 }catch(Exception e){
                     e.printStackTrace();
                     continue;
                 }
             }

             if(gpsCenter!=null){
                for (Result result : results) {
                    if(result.getMessage()==null || result.getMessage().length()==0) continue;
                    Reading r;
                    try{
                        r = Reading.fromJson(result.getMessage());
                        String [] gps=new String[2];

                         if (r.getContext().contains("Gps")){
                          if(r.getValue()!=null && r.getValue().length()>0){
                              gps[0]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[0]));
                              gps[1]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[1]));
                              latlng= "var myLatlng" + i +"= new google.maps.LatLng("+gps[0]+", "+gps[1]+");\n";
                              latLongOfDevice=result.getDeviceId();
                              //out.print(latlng);
                              //finalPrint+=latlng;
                              if (gpsCenter==null){
                                  gpsCenter=new String[2];
                                  gpsCenter[0]=gps[0];
                                  gpsCenter[1]=gps[1];
                              }
                              index=i;
                           }  else{
                              latlng="";
                           }
                         } else  if (r.getContext().contains("Wifi")){  //not GPS
                            String val="";
                            if(r.getValue()!=null && r.getValue().length()>0){
                                val=r.getValue();
                            }
                            if (val.equals("null"))val="";
                            val=result.getDeviceId()+":::"+val;
                            if (latlng.length()>0 && latLongOfDevice==result.getDeviceId())      {
                              String color=deviceColors.get(result.getDeviceId());
                              finalPrint+=latlng;
                              finalPrint+="var marker"+i +" = new google.maps.Marker({ position: myLatlng"+index+", map: map,title: '"+val +"',icon: '"+ color+ "'});\n";
                             }
                         }  else  if (r.getContext().contains("Noise")){   //not GPS
                            String val="";
                            if(r.getValue()!=null && r.getValue().length()>0){
                                val=r.getValue();
                            }
                            if (val.equals("null"))val="";


                            Date d=new Date(result.getTimestamp());
                            DateFormat df= new SimpleDateFormat("dd/MM/yy kk:mm:ss");
                            val=result.getDeviceId()+":::"+df.format(d)+":::"+val;
                            if (latlng.length()>0 && latLongOfDevice==result.getDeviceId())      {
                              String color=deviceColors.get(result.getDeviceId());
                              finalPrint+=latlng;
                              finalPrint+="var marker"+i +" = new google.maps.Marker({ position: myLatlng"+index+", map: map,title: '"+val +"',icon: '"+ color+ "'});\n";
                             }
                         }
                        i++;
                     }catch(Exception e){
                        e.printStackTrace();
                        continue;
                     }
                 }
                }else{
                    out.print("alert('No Gps Measurements Found');");
                    gpsCenter=new String[2];
                    gpsCenter[0]="43.4647222";
                    gpsCenter[1]="-3.8044444";
                    zoomLevel="4";
                }

             %>
        function openHeatmap() {
            var url = 'http://blanco.cti.gr:8080/heatmap.jsp?id=' + <%=expId%>;
            window.open(url, '_blank');
        }

        function initialize() {
            var myLatlng = new google.maps.LatLng(<%=gpsCenter[0]%>, <%=gpsCenter[1]%>);

            var mapOptions = {
                zoom: <%=zoomLevel%>,
                center: myLatlng
            }

            var map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

            <%=finalPrint%>
            <%=staticMarkers%>
        }


        google.maps.event.addDomListener(window, 'load', initialize);


    </script>
</head>

<body>
<jsp:include page="./includes/header.html" flush="true"/>
<input type="button" value="View Heatmap" onclick="openHeatmap();"/>

<div id="content">
    <div id="map-canvas"></div>
</div>
<br>
<br>
<br>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>

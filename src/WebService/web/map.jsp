<%@ page import="eu.smartsantander.androidExperimentation.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Reading" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Result" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Random" %>
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
             String expId = request.getParameter("id");
             Integer eId = null;
             try {
             eId = Integer.valueOf(expId);
             } catch (Exception e) {
             eId = null;
             }
             List<Result> results = ModelManager.getResults(eId);
             /*out.print("var s="+results.size() +"; \n");
             out.print("var d=[]; \n");*/
             int i=1;
             String latlng="";
             String finalPrint="";
             int index=1;
             String [] gpsCenter=null;
             String zoomLevel="14";
             for (Result result : results) {
                Reading r;
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
                              //out.print(latlng);
                              finalPrint+=latlng;
                              if (gpsCenter==null){
                                  gpsCenter=new String[2];
                                  gpsCenter[0]=gps[0];
                                  gpsCenter[1]=gps[1];
                              }
                              index=i;
                           }  else{
                              latlng="";
                           }
                         } else{
                            String val="";
                            if(r.getValue()!=null && r.getValue().length()>0){
                                val=r.getValue();
                            }

                            if (latlng.length()>0)      {
                              finalPrint+="var marker"+i +" = new google.maps.Marker({ position: myLatlng"+index+", map: map,title: '"+val +"'});\n";
                              //out.print("var marker"+i +" = new google.maps.Marker({ position: myLatlng"+index+", map: map,title: '"+val +"'});\n");
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
        function initialize() {
            var myLatlng = new google.maps.LatLng(<%=gpsCenter[0]%>, <%=gpsCenter[1]%>);

            var mapOptions = {
                zoom: <%=zoomLevel%>,
                center: myLatlng
            }

            var map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

            <%=finalPrint%>
        }


        google.maps.event.addDomListener(window, 'load', initialize);


    </script>
</head>

<body>
<jsp:include page="./includes/header.html" flush="true"/>
<div id="content">
    <div id="map-canvas"></div>
</div>
<br>
<br>
<br>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>

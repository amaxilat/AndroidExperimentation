<%@ page import="eu.smartsantander.androidExperimentation.service.ModelManager" %>
<%@ page import="eu.smartsantander.androidExperimentation.entities.Reading" %>
<%@ page import="eu.smartsantander.androidExperimentation.model.Result" %>


<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>Android Experimentation - SmartSantander</title>
    <script src="https://maps.googleapis.com/maps/api/js?sensor=false"></script>
    <script type="text/javascript" src="./js/heatmap.js"></script>
    <script type="text/javascript" src="./js/heatmap-gmaps.js"></script>
    <style>
        html, body, #heatmapArea {
            height: 100%;
            margin: 0px;
            padding: 0px
        }
    </style>

    <script type="text/javascript">
        window.onload = function () {
            <%
                 String expId = request.getParameter("id");
                 Integer eId = null;
                 try {
                 eId = Integer.valueOf(expId);
                 } catch (Exception e) {
                 eId = null;
                 }
                 List<Result> results = ModelManager.getResults(eId);
                 int i=1;
                 String latlng="";
                 String finalPrint="";
                 Integer latLongOfDevice=null;
                 String [] gpsCenter=null;
                 String zoomLevel="04";
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


                    String type="";
                    String data="";
                    String [] gps=new String[2];
                    for (Result result : results) {
                        if(result.getMessage()==null || result.getMessage().length()==0) continue;
                        Reading r;
                        try{
                            r = Reading.fromJson(result.getMessage());
                             if (r.getContext().contains("Gps")){
                              if(r.getValue()!=null && r.getValue().length()>0){
                                  gps[0]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[0]));
                                  gps[1]=ModelManager.formatDouble(Float.valueOf(r.getValue().split(",")[1]));
                                  latlng= "var myLatlng" + i +"= new google.maps.LatLng("+gps[0]+", "+gps[1]+");\n";
                                  latLongOfDevice=result.getDeviceId();
                                  if (gpsCenter==null){
                                      gpsCenter=new String[2];
                                      gpsCenter[0]=gps[0];
                                      gpsCenter[1]=gps[1];
                                  }
                               }  else{
                                  latlng="";
                               }
                              } else  if (r.getContext().contains("Wifi")){   //not GPS
                                String val="";
                                if(r.getValue()!=null && r.getValue().length()>0){
                                    val=r.getValue();
                                }
                                if (val.equals("null"))val="";
                                if (latlng.length()>0)      {
                                    if (val.length()>0 && latLongOfDevice==result.getDeviceId()){
                                        int grade= StringUtils.countMatches(val,"\"SSID\":");
                                        grade=grade+1;
                                        data+="{lat:"+ gps[0]+", lng:"+gps[1]+", count:"+ grade+"},";
                                    }   else{
                                        data+="{lat:"+ gps[0]+", lng:"+gps[1]+", count:"+ 1+"},";
                                    }
                                 }
                                 type="wifi";
                              } else  if (r.getContext().contains("Noise")){   //not GPS
                                String val="";
                                if(r.getValue()!=null && r.getValue().length()>0){
                                    val=r.getValue();
                                }
                                if (val.equals("null"))val="";
                                if (latlng.length()>0)      {
                                    if (val.length()>0 && latLongOfDevice==result.getDeviceId()){
                                        Float grade= Float.parseFloat(val);
                                        grade=grade+1;
                                        data+="{lat:"+ gps[0]+", lng:"+gps[1]+", count:"+ grade+"},";
                                    }   else{
                                        data+="{lat:"+ gps[0]+", lng:"+gps[1]+", count:"+ 1+"},";
                                    }
                                 }
                                 type="noise";
                             }
                             i++;
                         }catch(Exception e){
                            e.printStackTrace();
                            continue;
                         }
                     }
                     if(data.length()>20)
                        data=data.substring(0,data.length()-1);
                     data+="]};";

                     if (type.equals("wifi")) type="30";
                     else type="100";

                     String datas="var data={max: "+type+",  data:[ ";
                     finalPrint=datas+data;
                    }else{
                        out.print("alert('No Gps Measurements Found');");
                        gpsCenter=new String[2];
                        gpsCenter[0]="43.4647222";
                        gpsCenter[1]="-3.8044444";
                        zoomLevel="4";
                    }

                 %>

            var myLatlng = new google.maps.LatLng(<%=gpsCenter[0]%>, <%=gpsCenter[1]%>);
            var mapOptions = {
                zoom: <%=zoomLevel%>,
                center: myLatlng,
                mapTypeId: google.maps.MapTypeId.ROADMAP,
                disableDefaultUI: false,
                scrollwheel: true,
                draggable: true,
                navigationControl: true,
                mapTypeControl: false,
                scaleControl: true,
                disableDoubleClickZoom: false
            };

            var map = new google.maps.Map(document.getElementById('heatmapArea'), mapOptions);
            var heatmap = new HeatmapOverlay(map, {
                "radius": 20,
                "visible": true,
                "opacity": 60
            });

            <%=finalPrint%>


            google.maps.event.addListenerOnce(map, "idle", function () {
                heatmap.setDataSet(data);
            });
        };
    </script>
</head>

<body>
<jsp:include page="./includes/header.html" flush="true"/>
<div id="content">
    <div id="heatmapArea"/>
</div>
<br>
<br>
<br>

<jsp:include page="./includes/footer.html" flush="true"/>
</body>
</html>

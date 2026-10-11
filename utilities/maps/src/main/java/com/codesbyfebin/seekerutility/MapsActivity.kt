package com.codesbyfebin.seekerutility
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MapsActivity:ComponentActivity(){
 private fun parse(text:String):List<List<MapPoint>> {
  val elements=JSONObject(text).getJSONArray("elements")
  require(elements.length()<=10000)
  return (0 until elements.length()).mapNotNull { index ->
   val geometry=elements.getJSONObject(index).optJSONArray("geometry") ?: return@mapNotNull null
   require(geometry.length()<=5000)
   (0 until geometry.length()).map { i ->
    val point=geometry.getJSONObject(i)
    MapPoint(point.getDouble("lat"),point.getDouble("lon"))
   }
  }.filter { it.size>=2 }
 }
 override fun onCreate(state:Bundle?){super.onCreate(state);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 var lat by remember{mutableStateOf("9.9312")};var lon by remember{mutableStateOf("76.2673")};var endLat by remember{mutableStateOf("9.934")};var endLon by remember{mutableStateOf("76.27")};var roads by remember{mutableStateOf<List<List<MapPoint>>>(emptyList())};var path by remember{mutableStateOf<List<MapPoint>>(emptyList())};var status by remember{mutableStateOf("Download a small region first")};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val store=remember{EncryptedStore(this@MapsActivity,false)}
 Text("Seeker Maps",style=MaterialTheme.typography.headlineMedium);Text("© OpenStreetMap contributors · ODbL. Offline road viewer and approximate walking path. No GPS guidance, access/turn restrictions or guaranteed safe navigation.",style=MaterialTheme.typography.bodySmall)
 LaunchedEffect(Unit){runCatching{withContext(Dispatchers.IO){val data=store.read();if(data.isBlank())emptyList()else parse(data)}}.onSuccess{roads=it;if(it.isNotEmpty())status="Cached roads loaded; works offline"}.onFailure{status="Cache unavailable"}}
 Row{OutlinedTextField(lat,{lat=it},Modifier.weight(1f),label={Text("Start latitude")});OutlinedTextField(lon,{lon=it},Modifier.weight(1f),label={Text("Start longitude")})}
 Button(enabled=!busy,onClick={scope.launch{busy=true;runCatching{withContext(Dispatchers.IO){val a=lat.toDouble();val b=lon.toDouble();require(a.isFinite() && b.isFinite() && a in -85.0..85.0 && b in -179.0..179.0);val query="[out:json][timeout:20];way[highway][highway!~\"motorway|motorway_link\"](${a-.005},${b-.005},${a+.005},${b+.005});out geom;";val url="https://overpass-api.de/api/interpreter?data="+java.net.URLEncoder.encode(query,"UTF-8");OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build().newCall(Request.Builder().url(url).header("User-Agent","SeekerStore-Maps/0.1 (+https://github.com/CodesbyFebin/SKR-Seeker-Store)").build()).execute().use{response->check(response.isSuccessful);val data=response.body?.string()?:error("No roads");require(data.toByteArray().size<=1_000_000);val parsed=parse(data);store.write(data);parsed}}}.onSuccess{roads=it;path=emptyList();status="Downloaded ${it.size} roads; available offline"}.onFailure{status="Download failed; existing cache retained"};busy=false}}){Text(if(busy)"Downloading…" else "Download nearby roads (Internet)")}
 Row{OutlinedTextField(endLat,{endLat=it},Modifier.weight(1f),label={Text("End latitude")});OutlinedTextField(endLon,{endLon=it},Modifier.weight(1f),label={Text("End longitude")})}
 Button(enabled=roads.isNotEmpty(),onClick={scope.launch{runCatching{withContext(Dispatchers.Default){RoadRouter.route(roads,MapPoint(lat.toDouble(),lon.toDouble()),MapPoint(endLat.toDouble(),endLon.toDouble()))}}.onSuccess{path=it;status="Approximate road path: ${it.zipWithNext().sumOf{pair->RoadRouter.distance(pair.first,pair.second)}.toInt()} m"}.onFailure{status=it.message?:"No route"}}}){Text("Calculate cached path offline")}
 Canvas(Modifier.fillMaxWidth().weight(1f)){val points=roads.flatten();if(points.isNotEmpty()){val minLat=points.minOf{it.lat};val maxLat=points.maxOf{it.lat};val minLon=points.minOf{it.lon};val maxLon=points.maxOf{it.lon};fun project(p:MapPoint):Offset { return Offset(((p.lon-minLon)/(maxLon-minLon).coerceAtLeast(.00001)*size.width).toFloat(),((maxLat-p.lat)/(maxLat-minLat).coerceAtLeast(.00001)*size.height).toFloat()) }
for(road in roads)for(pair in road.zipWithNext())drawLine(Color.Gray,project(pair.first),project(pair.second),2f);for(pair in path.zipWithNext())drawLine(Color(0xFF9945FF),project(pair.first),project(pair.second),6f)}}
 Text(status)
 }}}}
 }
}

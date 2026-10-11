package com.codesbyfebin.seekerutility
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherActivity:ComponentActivity(){
 override fun onCreate(state:Bundle?){super.onCreate(state);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 var lat by remember{mutableStateOf("9.9312")};var lon by remember{mutableStateOf("76.2673")};var report by remember{mutableStateOf("No cached forecast")};var busy by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val store=remember{EncryptedStore(this@WeatherActivity,false)}
 Text("Seeker Weather",style=MaterialTheme.typography.headlineMedium)
 Text("Open-Meteo weather data · CC BY 4.0 attribution. Refresh requires Internet. Cached data remains available offline.")
 LaunchedEffect(Unit){runCatching{withContext(Dispatchers.IO){store.read()}}.onSuccess{if(it.isNotEmpty())report=it}.onFailure{report="Cached weather cannot be read"}}
 OutlinedTextField(lat,{lat=it},label={Text("Latitude")});OutlinedTextField(lon,{lon=it},label={Text("Longitude")})
 Button(enabled=!busy,onClick={scope.launch{busy=true;runCatching{withContext(Dispatchers.IO){val latitude=lat.toDouble();val longitude=lon.toDouble();require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0);val url="https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,wind_speed_10m&timezone=auto";OkHttpClient.Builder().callTimeout(20,TimeUnit.SECONDS).build().newCall(Request.Builder().url(url).build()).execute().use{response->check(response.isSuccessful);val json=JSONObject(response.body?.string()?:error("Empty response"));val current=json.getJSONObject("current");val units=json.getJSONObject("current_units");val value="Location: $latitude, $longitude\nWeather time: ${current.getString("time")} (${json.optString("timezone")})\nFetched: ${java.time.Instant.now()}\nTemperature: ${current.getDouble("temperature_2m")} ${units.getString("temperature_2m")}\nHumidity: ${current.getDouble("relative_humidity_2m")} ${units.getString("relative_humidity_2m")}\nWind: ${current.getDouble("wind_speed_10m")} ${units.getString("wind_speed_10m")}";store.write(value);value}}}.onSuccess{report=it}.onFailure{report+="\nRefresh failed; cached data retained. Check coordinates and network."};busy=false}}){Text(if(busy)"Refreshing…" else "Refresh weather")}
 Text(report)
 }}}}
 }
}

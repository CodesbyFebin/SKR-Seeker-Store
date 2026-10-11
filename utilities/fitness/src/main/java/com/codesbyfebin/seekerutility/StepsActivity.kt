package com.codesbyfebin.seekerutility
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class StepsActivity:ComponentActivity(),SensorEventListener {
 private var steps by mutableIntStateOf(0);private var message by mutableStateOf("Not tracking");private var first:Float?=null;private var tracking by mutableStateOf(false)
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)start()else message="Activity recognition permission denied"}
 private fun start(){val manager=requireNotNull(getSystemService(SensorManager::class.java));val sensor=manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);if(sensor==null){message="No step-counter sensor available";return};first=null;steps=0;tracking=manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);message=if(tracking)"Tracking this foreground session" else "Sensor unavailable"}
 private fun stop(){getSystemService(SensorManager::class.java)?.unregisterListener(this);tracking=false;message="Stopped; session total $steps steps"}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Seeker Steps",style=MaterialTheme.typography.headlineMedium);Text("Actual sensor counts for this foreground session. No background tracking, medical inference or SKR rewards.");Text("$steps steps",style=MaterialTheme.typography.headlineLarge);Text(message);Button(enabled=!tracking,onClick={if(Build.VERSION.SDK_INT>=29 && checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.ACTIVITY_RECOGNITION)else start()}){Text("Start session")};Button(enabled=tracking,onClick={stop()}){Text("Stop")}}}}}}
 override fun onStop(){super.onStop();stop()}
 override fun onSensorChanged(event:SensorEvent){val value=event.values[0];if(first==null || value<first!!)first=value;steps=(value-first!!).toInt().coerceAtLeast(0)}
 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
}

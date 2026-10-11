package com.codesbyfebin.seekerutility
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class CameraActivity:ComponentActivity(){
 private var permitted by mutableStateOf(false);private var status by mutableStateOf("Camera permission required");private var ready by mutableStateOf(false);private var capturing by mutableStateOf(false);private var capture:ImageCapture?=null
 private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permitted=checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT>=29 || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);status=if(permitted)"Starting camera"else "Permission denied"}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);permitted=checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT>=29 || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Text("Seeker Camera",style=MaterialTheme.typography.headlineMedium);Text("Native CameraX capture. Photos save to your gallery. SHA-256 identifies bytes; it does not prove capture time, authenticity or on-chain inclusion.")
 if(!permitted)Button(onClick={permission.launch(if(Build.VERSION.SDK_INT<29)arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)else arrayOf(Manifest.permission.CAMERA))}){Text("Enable camera")}
 else AndroidView(modifier=Modifier.weight(1f),factory={context->PreviewView(context).also{view->val future=ProcessCameraProvider.getInstance(context);future.addListener({try{val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();provider.unbindAll();provider.bindToLifecycle(this@CameraActivity,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture);ready=true;status="Camera ready"}catch(_:Exception){ready=false;status="Camera unavailable"}},ContextCompat.getMainExecutor(context))}})
 Button(enabled=ready && !capturing,onClick={val image=capture?:return@Button;capturing=true;val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"Seeker-${System.currentTimeMillis()}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Seeker")};val options=ImageCapture.OutputFileOptions.Builder(contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values).build();image.takePicture(options,ContextCompat.getMainExecutor(this@CameraActivity),object:ImageCapture.OnImageSavedCallback{override fun onError(e:ImageCaptureException){capturing=false;status="Capture failed"};override fun onImageSaved(result:ImageCapture.OutputFileResults){capturing=false;val uri=result.savedUri;status="Photo saved";if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val digest=MessageDigest.getInstance("SHA-256");contentResolver.openInputStream(uri)?.use{val buffer=ByteArray(8192);while(true){val n=it.read(buffer);if(n<0)break;digest.update(buffer,0,n)}}?:error("Cannot read capture");digest.digest().joinToString(""){"%02x".format(it)}}}.onSuccess{status="Saved SHA-256: $it"}.onFailure{status="Photo saved; checksum unavailable"}}}})}){Text(if(capturing)"Capturing…" else "Capture photo")};Text(status)
 }}}}
 }
}

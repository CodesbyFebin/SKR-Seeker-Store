package com.codesbyfebin.seekerutility

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class UtilityActivity:ComponentActivity() {
 private var unlocked by mutableStateOf(BuildConfig.MODE!="vault")
 private var status by mutableStateOf("")
 private var selected by mutableStateOf<Uri?>(null)
 private var folder by mutableStateOf<Uri?>(null)
 private var player:MediaPlayer?=null
 private var backupPassword=""
 private var pending:ByteArray?=null
 private val unlock=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){unlocked=it.resultCode==Activity.RESULT_OK;status=if(unlocked)"Unlocked for 30 seconds" else "Locked"}
 private val pick=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  selected=uri
  if(uri!=null) {
   runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
   if(BuildConfig.MODE=="music") play(uri)
  }
 }
 private val tree=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->
  folder=uri
  if(uri!=null)runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}.onFailure{status="Folder permission unavailable"}
 }
 private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
  val data=pending;pending=null
  if(uri!=null && data!=null) lifecycleScope.launch { status=runCatching{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri)?.use{it.write(data)}?:error("Cannot write file")};"File saved"}.getOrElse{"Save failed"};data.fill(0) }
 }
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF9945FF))) { Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text(BuildConfig.TITLE,style=MaterialTheme.typography.headlineMedium)
    Text("Local utility · No Google account",style=MaterialTheme.typography.bodySmall)
    if(!unlocked) Button(onClick={authenticate()}){Text("Unlock with device credentials")}
    else when(BuildConfig.MODE){
     "notes","vault"->Editor()
     "tasks"->Tasks()
     "calculator"->Calc()
     "files"->Files()
     "music"->Music()
     "gallery"->Gallery()
     "device"->Device()
     "backup"->Backup()
    }
    if(status.isNotEmpty())Text(status)
   }
  } } }
 }
 override fun onStop(){super.onStop();player?.release();player=null;if(BuildConfig.MODE=="vault")unlocked=false;backupPassword=""}
 private fun authenticate(){val km=requireNotNull(getSystemService(KeyguardManager::class.java));if(!km.isDeviceSecure){status="Set a device PIN or password first";return};val intent=km.createConfirmDeviceCredentialIntent("Seeker Vault","Unlock local encrypted records");if(intent!=null)unlock.launch(intent)else status="Device authentication unavailable"}
 private fun play(uri:Uri){lifecycleScope.launch{player?.release();player=null;runCatching{withContext(Dispatchers.IO){MediaPlayer().apply{try{setDataSource(this@UtilityActivity,uri);prepare()}catch(e:Exception){release();throw e}}}}.onSuccess{player=it;it.start();status="Playing selected audio"}.onFailure{status="Audio cannot be played"}}}
 @Composable private fun Editor(){
  val store=remember{EncryptedStore(this,BuildConfig.MODE=="vault")};val scope=rememberCoroutineScope();var text by remember{mutableStateOf("")};var ready by remember{mutableStateOf(false)};var visible by remember{mutableStateOf(false)}
  LaunchedEffect(Unit){runCatching{withContext(Dispatchers.IO){store.read()}}.onSuccess{text=it;ready=true}.onFailure{status="Encrypted storage unavailable; unlock again if expired"}}
  Text(if(BuildConfig.MODE=="vault")"Encrypted private records. No autofill, Seed Vault integration or recovery service." else "Private notes and journal. Saved only when you tap Save.")
  if(BuildConfig.MODE=="vault")TextButton(onClick={visible=!visible}){Text(if(visible)"Hide records" else "Reveal records")}
  if(BuildConfig.MODE!="vault" || visible)OutlinedTextField(text,{text=it},Modifier.fillMaxWidth().heightIn(min=180.dp),label={Text("Records")},enabled=ready)
  Button(enabled=ready,onClick={scope.launch{runCatching{withContext(Dispatchers.IO){store.write(text)}}.onSuccess{status="Encrypted records saved"}.onFailure{status="Save failed; unlock again if expired"}}}){Text("Save")}
 }
 @Composable private fun Tasks(){
  val store=remember{EncryptedStore(this,false)};val scope=rememberCoroutineScope();var tasks by remember{mutableStateOf<List<Pair<String,Boolean>>>(emptyList())};var input by remember{mutableStateOf("")};var ready by remember{mutableStateOf(false)}
  LaunchedEffect(Unit){runCatching{withContext(Dispatchers.IO){store.read()}}.onSuccess{s->val a=JSONArray(s.ifEmpty{"[]"});tasks=(0 until a.length()).map{a.getJSONObject(it).run{getString("text") to getBoolean("done")}};ready=true}.onFailure{status="Could not load tasks"}}
  fun save(next:List<Pair<String,Boolean>>){scope.launch{runCatching{withContext(Dispatchers.IO){val a=JSONArray();next.forEach{a.put(JSONObject().put("text",it.first).put("done",it.second))};store.write(a.toString())}}.onSuccess{tasks=next;status="Saved locally"}.onFailure{status="Save failed"}}}
  OutlinedTextField(input,{input=it.take(1000)},label={Text("New task")})
  Button(enabled=ready && input.isNotBlank(),onClick={save(tasks+(input.trim() to false));input=""}){Text("Add")}
  LazyColumn {items(tasks.indices.toList()){i->Row{Checkbox(tasks[i].second,{save(tasks.toMutableList().apply{this[i]=tasks[i].first to it})});Text(tasks[i].first,Modifier.weight(1f));TextButton(onClick={save(tasks.filterIndexed{j,_->j!=i})}){Text("Delete")}}}}
 }
 @Composable private fun Calc(){var expression by remember{mutableStateOf("")};OutlinedTextField(expression,{expression=it.take(256)},label={Text("Expression: + − * / ( )")});Button(onClick={status=runCatching{Calculator.evaluate(expression).toString()}.getOrElse{it.message?:"Invalid expression"}}){Text("Calculate")}}
 @Composable private fun Files(){
  var entries by remember{mutableStateOf<List<DocumentFile>>(emptyList())};var revision by remember{mutableIntStateOf(0)};val scope=rememberCoroutineScope();var confirm by remember{mutableStateOf<DocumentFile?>(null)}
  Button(onClick={tree.launch(null)}){Text("Choose accessible folder")};Text("Only the folder you grant. Android sandbox restrictions apply.")
  LaunchedEffect(folder,revision){runCatching{withContext(Dispatchers.IO){folder?.let{DocumentFile.fromTreeUri(this@UtilityActivity,it)?.listFiles()?.toList()}?:emptyList()}}.onSuccess{entries=it}.onFailure{status="Cannot read folder"}}
  LazyColumn{items(entries){file->Row{Text(file.name?:"Unnamed",Modifier.weight(1f));if(file.isDirectory)Text("Folder")else TextButton(onClick={runCatching{startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(file.uri,file.type?:"application/octet-stream").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{status="No application can open this file"}}){Text("Open")};if(file.canWrite())TextButton(onClick={confirm=file}){Text("Delete")}}}}
  confirm?.let{file->AlertDialog(onDismissRequest={confirm=null},title={Text("Delete ${file.name}?")},text={Text("This deletes the selected file or folder from its provider.")},confirmButton={TextButton(onClick={scope.launch{status=if(withContext(Dispatchers.IO){file.delete()})"Deleted" else "Delete failed";revision++;confirm=null}}){Text("Delete")}},dismissButton={TextButton(onClick={confirm=null}){Text("Cancel")}})}
 }
 @Composable private fun Music(){Button(onClick={pick.launch(arrayOf("audio/*"))}){Text("Choose local audio")};Row{Button(onClick={player?.pause()}){Text("Pause")};Spacer(Modifier.width(8.dp));Button(onClick={player?.start()}){Text("Play")}};Text("Playback stops when this app leaves the foreground.")}
 @Composable private fun Gallery(){
  var bitmap by remember{mutableStateOf<android.graphics.Bitmap?>(null)};Button(onClick={pick.launch(arrayOf("image/*"))}){Text("Choose local photo")}
  LaunchedEffect(selected){val uri=selected?:return@LaunchedEffect;runCatching{withContext(Dispatchers.IO){val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)};val opts=BitmapFactory.Options().apply{inSampleSize=1};while(bounds.outWidth/opts.inSampleSize>1600 || bounds.outHeight/opts.inSampleSize>1600)opts.inSampleSize*=2;contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}}}.onSuccess{bitmap=it}.onFailure{status="Cannot decode photo"}}
  bitmap?.let{Image(it.asImageBitmap(),"Selected local photo",Modifier.fillMaxWidth().heightIn(max=450.dp))}
 }
 @Composable private fun Device(){var report by remember{mutableStateOf("")};Button(onClick={val memory=ActivityManager.MemoryInfo();requireNotNull(getSystemService(ActivityManager::class.java)).getMemoryInfo(memory);val battery=requireNotNull(getSystemService(BatteryManager::class.java));val disk=StatFs(filesDir.path);report="${Build.MANUFACTURER} ${Build.MODEL}\nAndroid API ${Build.VERSION.SDK_INT}\nABIs: ${Build.SUPPORTED_ABIS.joinToString()}\nRAM available: ${memory.availMem/1048576} MiB / ${memory.totalMem/1048576} MiB\nBattery: ${battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%\nApp storage available: ${disk.availableBytes/1048576} MiB"}){Text("Read device measurements")};Text(report);Text("Does not claim per-app battery attribution or measured CPU temperature.")}
 @Composable private fun Backup(){
  var password by remember{mutableStateOf("")};val scope=rememberCoroutineScope()
  Text("Encrypt or restore one selected file, up to 10 MB. Keep the passphrase separately; losing it loses access.")
  OutlinedTextField(password,{password=it;backupPassword=it},label={Text("Passphrase (12+ characters)")},visualTransformation=PasswordVisualTransformation())
  Button(onClick={pick.launch(arrayOf("*/*"))}){Text("Choose source file")};Text(selected?.lastPathSegment?:"No file selected")
  fun process(decrypt:Boolean){val uri=selected?:return;val chars=password.toCharArray();scope.launch{status="Processing…";runCatching{withContext(Dispatchers.IO){val data=contentResolver.openInputStream(uri)?.use{val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val count=it.read(buffer);if(count<0)break;require(out.size()+count<=10_000_048);out.write(buffer,0,count)};out.toByteArray()}?:error("Cannot read file");require(data.size<=10_000_048);if(decrypt)PortableBackup.decrypt(data,chars)else PortableBackup.encrypt(data,chars)}}.onSuccess{pending=it;export.launch(if(decrypt)"restored.bin" else "backup.ssb")}.onFailure{status="Failed: invalid file, passphrase or unavailable provider"};chars.fill('\u0000')}}
  Row{Button(enabled=password.length>=12 && selected!=null,onClick={process(false)}){Text("Encrypt")};Spacer(Modifier.width(8.dp));Button(enabled=password.length>=12 && selected!=null,onClick={process(true)}){Text("Restore")}}
 }
}

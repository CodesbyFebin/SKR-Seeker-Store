package com.codesbyfebin.seekerutility
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
import androidx.lifecycle.lifecycleScope
import com.codesbyfebin.skrdstore.LocalAgent
import kotlinx.coroutines.launch

class TranslationActivity:ComponentActivity(){
 private lateinit var agent:LocalAgent;private var ready by mutableStateOf(false);private var busy by mutableStateOf(false);private var status by mutableStateOf("Model required")
 private val import=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)lifecycleScope.launch{busy=true;runCatching{agent.importModel(uri)}.onSuccess{ready=agent.ready();status="Model checksum verified; works offline"}.onFailure{status="Model import failed or checksum mismatch"};busy=false}}
 override fun onCreate(state:Bundle?){super.onCreate(state);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);agent=LocalAgent(this);ready=agent.ready()
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 var source by remember{mutableStateOf("")};var language by remember{mutableStateOf("Malayalam")};var result by remember{mutableStateOf("")};val scope=rememberCoroutineScope()
 Text("Seeker Translate",style=MaterialTheme.typography.headlineMedium);Text("Local Qwen translation · CPU only · No Internet permission. Import the official Qwen2.5 1.5B Q4_K_M GGUF separately. Translation may be inaccurate; verify the meaning.")
 Button(enabled=!busy,onClick={import.launch(arrayOf("application/octet-stream","*/*"))}){Text("Import verified model")};Text(status)
 OutlinedTextField(language,{language=it.take(80)},label={Text("Target language")});OutlinedTextField(source,{source=it.take(1000)},label={Text("Text to translate")},modifier=Modifier.fillMaxWidth())
 Button(enabled=ready && !busy && source.isNotBlank() && language.isNotBlank(),onClick={scope.launch{busy=true;runCatching{agent.translate(source,language)}.onSuccess{result=it;status="Offline inference complete"}.onFailure{status="Local inference failed"};busy=false}}){Text(if(busy)"Processing…" else "Translate offline")};Text(result)
 }}}}
 }
}

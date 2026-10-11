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
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class BrowserActivity:ComponentActivity(){
 companion object{private var runtime:GeckoRuntime?=null}
 private lateinit var session:GeckoSession
 override fun onCreate(state:Bundle?){super.onCreate(state);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
 if(runtime==null)runtime=GeckoRuntime.create(applicationContext,GeckoRuntimeSettings.Builder().build())
 session=GeckoSession(GeckoSessionSettings.Builder().usePrivateMode(true).build());session.setContentDelegate(object:GeckoSession.ContentDelegate{});session.open(requireNotNull(runtime))
 setContent{MaterialTheme(colorScheme=darkColorScheme()){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 var address by remember{mutableStateOf("https://solana.com/developers")};var status by remember{mutableStateOf("Private session; no app analytics. Visited sites can still track you.")}
 Text("Seeker Browser",style=MaterialTheme.typography.headlineSmall)
 OutlinedTextField(address,{address=it.take(4096)},label={Text("HTTPS address")},singleLine=true,modifier=Modifier.fillMaxWidth())
 Row{Button(onClick={runCatching{val uri=android.net.Uri.parse(address);require(uri.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null);session.loadUri(address)}.onFailure{status="Enter a valid HTTPS URL"}}){Text("Open")};TextButton(onClick={session.goBack()}){Text("Back")};TextButton(onClick={session.goForward()}){Text("Forward")};TextButton(onClick={session.reload()}){Text("Reload")}}
 Text(status,style=MaterialTheme.typography.bodySmall)
 AndroidView(factory={GeckoView(it).apply{setSession(session)}},modifier=Modifier.weight(1f))
 }}}}
 }
 override fun onDestroy(){session.close();super.onDestroy()}
}

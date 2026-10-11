package com.codesbyfebin.skrdstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Job

class MainActivity: ComponentActivity() {
 private lateinit var sender: ActivityResultSender
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  sender = ActivityResultSender(this)
  val dao = DStoreDb.get(this).endorsements()
  val wallet = WalletFlow(sender,SecureStore(this),dao)
  val agent = LocalAgent(this)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF9945FF),secondary=Color(0xFF14F195),background=Color(0xFF090A12),surface=Color(0xFF171824))) { DStore(dao,wallet,agent) } }
 }
 @OptIn(ExperimentalMaterial3Api::class)
 @Composable private fun DStore(dao: EndorsementDao, wallet: WalletFlow, agent: LocalAgent) {
  val rows by dao.observe().collectAsState(initial=emptyList())
  var tab by remember { mutableIntStateOf(0) }
  var busy by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf("Browse ten demo listings. Endorsements use devnet SOL.") }
  var review by remember { mutableStateOf<WalletFlow.Prepared?>(null) }
  var question by remember { mutableStateOf("") }
  var answer by remember { mutableStateOf("") }
  var modelReady by remember { mutableStateOf(agent.ready()) }
  var modelBusy by remember { mutableStateOf(false) }
  var balance by remember { mutableStateOf<Long?>(null) }
  val catalogSource=remember { CatalogSource(this@MainActivity) }
  val installer=remember { ApkInstaller(this@MainActivity) }
  var releases by remember { mutableStateOf(catalogSource.cached()) }
  val history=remember { InstallHistory(this@MainActivity) }
  var favorites by remember { mutableStateOf(history.favorites()) }
  var search by remember { mutableStateOf("") }
  var libraryFilter by remember { mutableStateOf("All") }
  var sortName by remember { mutableStateOf(false) }
  var installedTick by remember { mutableIntStateOf(0) }
  var downloadText by remember { mutableStateOf("") }
  var installJob by remember { mutableStateOf<Job?>(null) }
  DisposableEffect(Unit) {
   val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) {installedTick++;releases=catalogSource.cached()} }
   lifecycle.addObserver(observer)
   onDispose { lifecycle.removeObserver(observer) }
  }
  var installReview by remember { mutableStateOf<ApkRelease?>(null) }
  var exportRow by remember { mutableStateOf<Endorsement?>(null) }
  val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
   val row = exportRow
   if(uri != null && row != null) lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
    try {
     val envelope = org.json.JSONObject().put("payloadUtf8",row.receipt).put("signatureBase64",row.receiptSignature).put("publicKeySpkiBase64",row.receiptPublicKey).put("observedStatus",row.status)
     contentResolver.openOutputStream(uri)!!.use { it.write(envelope.toString(2).toByteArray(Charsets.UTF_8)) }
     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { status="Signed receipt exported." }
    } catch(e: Exception) {
     if(e is kotlinx.coroutines.CancellationException) throw e
     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { status="Receipt export failed." }
    }
   }
  }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
   if(uri != null) lifecycleScope.launch {
    modelBusy=true
    try { agent.importModel(uri); modelReady=agent.ready(); status="Model imported and checksum verified." }
    catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; status=e.message ?: "Import failed" }
    finally { modelBusy=false }
   }
  }
  fun endorse(app: DApp) {
   if(busy) return
   lifecycleScope.launch {
    busy=true; status="Connecting wallet and preparing exact-message review…"
    try { val prepared = wallet.prepare(app); balance=prepared.balance; review=prepared; status="Review the destination, amount and message hash." }
    catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; status=e.message ?: "Connection or network error" }
    finally { busy=false }
   }
  }
  Scaffold(topBar={ Column(Modifier.padding(horizontal=20.dp,vertical=16.dp)) {
   Text("Seeker Store",fontSize=24.sp,fontWeight=FontWeight.Bold)
   Text("Discover. Endorse. Keep the receipt.",color=MaterialTheme.colorScheme.secondary)
   Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.15f)) { Text("Genesis ✓ demo | TEEPIN ✓ demo | Devnet",Modifier.padding(horizontal=10.dp,vertical=5.dp),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
  } },bottomBar={ NavigationBar {
   val labels=listOf("Discover","DePIN","Endorsements","Agent")
   val icons=listOf(Icons.Default.Explore,Icons.Default.Hub,Icons.Default.Verified,Icons.Default.SmartToy)
   labels.forEachIndexed { i,label -> NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],contentDescription=label)},label={Text(label,fontSize=10.sp)}) }
  } }) { padding -> Column(Modifier.padding(padding).fillMaxSize()) {
   Text(status,Modifier.padding(horizontal=20.dp,vertical=8.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(downloadText.isNotBlank()) Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
    Text(downloadText,Modifier.weight(1f),fontSize=12.sp)
    if(installJob?.isActive==true) TextButton(onClick={installer.cancel();installJob?.cancel();downloadText="Download canceled"}) {Text("Cancel")}
   }
   if(busy || modelBusy) Text("Working…",Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.secondary)
   when(tab) {
    0,1 -> {
     val depin=tab==1
     Text(if(depin) "DePIN / Edge" else "Discover apps",Modifier.padding(horizontal=20.dp,vertical=8.dp),fontSize=22.sp,fontWeight=FontWeight.SemiBold)
     Text(if(depin) "Demo synergy: 2× boost display; no SKR rewards issued." else "Ranked by demo SKR weight · 10 curated examples",Modifier.padding(horizontal=20.dp),fontSize=12.sp)
     OutlinedTextField(value=search,onValueChange={search=it.take(100)},singleLine=true,label={Text("Search apps or contribution types")},modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp))
     Row(Modifier.padding(horizontal=20.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      listOf("All","Installed","Updates","Favorites").forEach { filter -> FilterChip(selected=libraryFilter==filter,onClick={libraryFilter=filter},label={Text(filter)}) }
      FilterChip(selected=sortName,onClick={sortName=!sortName},label={Text(if(sortName) "Name A–Z" else "SKR weight")})
     }
     OutlinedButton(onClick={ lifecycleScope.launch {
      busy=true
      try { releases=catalogSource.refresh(); status="Catalog refreshed: ${releases.size} reviewed APK releases available." }
      catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; status=e.message ?: "Catalog unavailable; using cached releases." }
      finally { busy=false }
     } },enabled=!busy,modifier=Modifier.padding(horizontal=20.dp)) { Text("Refresh install catalog") }
     LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      val apps = if(depin) Catalog.apps.filter { it.category=="DePIN" }.sortedWith(compareByDescending<DApp> {it.id=="edgeore"}.thenByDescending {Catalog.weight(it,rows)}) else Catalog.apps.sortedByDescending {Catalog.weight(it,rows)}
      val visible=apps.filter {app ->
       val action=releases[app.id]?.let { installer.action(it) } ?: "Install"
       (search.isBlank() || "${app.name} ${app.category} ${app.contribution}".contains(search,true)) &&
       when(libraryFilter) {"Installed" -> action=="Open" || action=="Update"; "Updates" -> action=="Update"; "Favorites" -> app.id in favorites; else -> true}
      }.let { if(sortName) it.sortedWith(compareByDescending<DApp> {depin && it.id=="edgeore"}.thenBy {it.name}) else it }
      if(visible.isEmpty()) item {Text("No apps match this view.",Modifier.padding(20.dp))}
      if(!depin) {
       item { Text("Seeker utilities · build validation pending",Modifier.padding(20.dp),fontWeight=FontWeight.Bold) }
       items(UtilityCatalog.apps.filter { utility ->
        val action=releases[utility.first]?.let{installer.action(it)} ?: "APK metadata not supplied"
        (search.isBlank() || utility.second.contains(search,true)) && when(libraryFilter){"Installed"->action=="Open" || action=="Update";"Updates"->action=="Update";"Favorites"->utility.first in favorites;else->true}
       }.let{if(sortName)it.sortedBy{entry->entry.second}else it}) { utility ->
        Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=4.dp)) { Column(Modifier.padding(16.dp)) {
         Text("Seeker ${utility.second}",fontWeight=FontWeight.Bold)
         Text("Independent Android utility · source implemented · APK validation pending",fontSize=12.sp)
         TextButton(onClick={history.favorite(utility.first);favorites=history.favorites()}){Text(if(utility.first in favorites) "★ Saved" else "☆ Save app")}
         val release=releases[utility.first]
         val action=remember(release,installedTick) {release?.let {installer.action(it)} ?: "APK metadata not supplied"}
         OutlinedButton(enabled=release!=null && !busy && action!="Signer mismatch",onClick={if(release!=null){if(action=="Open")runCatching{installer.open(release)}.onFailure{status="Cannot open utility"}else installReview=release}}){Text(action)}
         TextButton(onClick={val launch=packageManager.getLaunchIntentForPackage("com.codesbyfebin.seeker.${utility.first}");if(launch!=null) startActivity(launch) else status="Install a validated utility APK first. Publisher hashes and signing pins are not yet configured."}) {Text("Open installed app")}
        } }
       }
      }

      items(visible,key={it.id}) { app -> Card(shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=.15f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) { Text(app.name.take(1),color=MaterialTheme.colorScheme.primary,fontSize=22.sp,fontWeight=FontWeight.Bold) }
        Column(Modifier.padding(start=12.dp).weight(1f)) { Text(app.name,fontWeight=FontWeight.Bold,fontSize=18.sp); Text(app.contribution,fontSize=12.sp) }
        Text("%.1f×".format(Locale.US,Catalog.weight(app,rows)),color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
       }
       Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        BadgeText(app.category)
        if(app.guardian) BadgeText("Verified ✓ demo")
        if(app.native) BadgeText("Native demo")
       }
       Text("${app.rating} · ${app.updated}",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
       if(depin) Text("SKR boost: 2× demo",fontSize=12.sp,color=MaterialTheme.colorScheme.secondary)
       if(app.id=="edgeore") {
        Text("Built on Seeker Store · demo integration",fontSize=11.sp)
        Text("Stake SKR to boost proof rewards 2× — planned",fontSize=11.sp)
        TextButton(onClick={ try { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("solanadappstore://details?id=com.yourapp.edgeore"))) } catch(e: android.content.ActivityNotFoundException) { status="Solana dApp Store unavailable. EdgeORE package ID needs configuration." } }) { Text("Open EdgeORE in dApp Store") }
       }
       val release=releases[app.id]
       val action=remember(release,installedTick) {release?.let {installer.action(it)} ?: "APK metadata not supplied"}
       TextButton(onClick={history.favorite(app.id);favorites=history.favorites()}) {Text(if(app.id in favorites) "★ Saved" else "☆ Save app")}
       if(release!=null) {
        Text("Version ${release.versionName.ifBlank {release.versionCode.toString()}} · ${release.assetName}",fontSize=11.sp)
        BadgeText("Catalog-pinned APK")
        if(release.sourceRepo.isNotBlank()) TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(release.sourceRepo)))}) {Text("View source repository")}
        if(release.changelog.isNotBlank()) Text(release.changelog.take(400),fontSize=11.sp)
       }
       OutlinedButton(onClick={if(release!=null) { if(action=="Open") {try {installer.open(release)} catch(e: Exception) {status=e.message ?: "Cannot open app"}} else installReview=release }},enabled=release!=null && !busy && action!="Signer mismatch",modifier=Modifier.fillMaxWidth()) { Text(action) }
       OutlinedButton(onClick={endorse(app)},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("Endorse · 0.001 SOL devnet") }
      } } }
     }
    }
    2 -> {
     Text("My endorsements",Modifier.padding(20.dp),fontSize=22.sp,fontWeight=FontWeight.Bold)
     val confirmed=rows.filter {it.status=="CONFIRMED"}
     balance?.let { Text("Wallet balance at review: %.6f SOL".format(Locale.US,it/1e9),Modifier.padding(horizontal=20.dp),fontSize=12.sp) }
     Text("Transfers are not locked or refundable SKR stakes.",Modifier.padding(20.dp),fontSize=12.sp)
     Button(onClick={tab=0},modifier=Modifier.padding(horizontal=20.dp)) { Text("Endorse an app") }
     if(rows.isEmpty()) Text("No endorsements yet. Your signed receipts will appear here.",Modifier.padding(20.dp))
     LazyColumn(modifier=Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { items(rows,key={it.tx}) { row -> Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Text(Catalog.apps.first {it.id==row.appId}.name,fontWeight=FontWeight.Bold)
      Text("0.001 SOL · ${row.status} · ${if(row.status=="CONFIRMED") "+0.1× demo weight" else "No weight yet"}",fontSize=12.sp)
      Text(SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date(row.created)),fontSize=12.sp)
      Text("Message SHA-256: ${row.messageHash}",fontSize=10.sp)
      Text("P-256 receipt signed locally",fontSize=12.sp,color=MaterialTheme.colorScheme.secondary)
      TextButton(onClick={exportRow=row; exporter.launch("skr-dstore-${row.appId}-${row.created}.json")}) { Text("Export signed receipt") }
     } } } }
     Text("Total demo stake: %.3f SOL (devnet)".format(Locale.US,confirmed.sumOf {it.lamports}/1e9),Modifier.padding(horizontal=20.dp,vertical=12.dp),fontWeight=FontWeight.Bold)
    }
    3 -> Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
     Text("Local agent",fontSize=24.sp,fontWeight=FontWeight.Bold)
     Text(if(modelReady) "● Works offline · Qwen 1.5B · CPU only" else "Model required · import Qwen Q4_K_M GGUF",color=if(modelReady) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
     Text("One thread · 2048-token context · no cloud requests",fontSize=12.sp)
     OutlinedButton(onClick={picker.launch(arrayOf("*/*"))},enabled=!modelBusy && !busy) { Text("Import verified GGUF") }
     OutlinedTextField(value=question,onValueChange={question=it.take(1000)},placeholder={Text("What should I endorse?")},modifier=Modifier.fillMaxWidth(),enabled=!busy,label={Text("Ask about the demo catalog")})
     Button(onClick={ lifecycleScope.launch {
      busy=true; val started=android.os.SystemClock.elapsedRealtime()
      try { answer=agent.ask(question,rows); status="Local inference completed in ${android.os.SystemClock.elapsedRealtime()-started} ms." }
      catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; status=e.message ?: "Local inference failed" }
      finally { busy=false }
     } },enabled=modelReady && !busy && !modelBusy && question.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text("Ask offline") }
     if(answer.isNotBlank()) Card(shape=RoundedCornerShape(12.dp)) { Text(answer,Modifier.padding(16.dp)) }
     Text("Recommendations are informational. Every endorsement requires a separate wallet review.",fontSize=12.sp)
    }
   }
  } }
  installReview?.let { release -> AlertDialog(onDismissRequest={if(!busy) installReview=null},title={Text("Review APK installation")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("${release.packageName} · version ${release.versionCode}")
   Text("Download: ${release.url}",fontSize=11.sp)
   Text("APK SHA-256: ${release.sha256}",fontSize=10.sp)
   Text("Signer certificate SHA-256: ${release.signerSha256}",fontSize=10.sp)
   Text("Downloaded bytes, package, version and signing certificate are checked. Android asks you to confirm installation.",fontSize=12.sp)
  }},confirmButton={TextButton(enabled=!busy,onClick={installJob=lifecycleScope.launch {
   busy=true; installReview=null; downloadText="Preparing download…"
   try {
    val latest=catalogSource.refresh(); releases=latest
    require(latest[release.appId]==release) { "Release metadata changed. Review again." }
    if(!packageManager.canRequestPackageInstalls()) {
     startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName")))
     status="Enable installation for Seeker Store, then return and review again."
    } else { val file=installer.download(release) { done,total -> runOnUiThread {downloadText=if(total>0) "Downloading ${done*100/total}% · ${done/1048576} MiB" else "Downloading ${done/1048576} MiB"} }; history.pending(release); installer.launch(file); downloadText="Awaiting Android installation confirmation"; status="Verified APK handed to Android installer. Installation awaits your confirmation." }
    installReview=null
   } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) {status="Download canceled";installReview=null;throw e}; status=e.message ?: "APK installation failed"; downloadText=status; installReview=null }
   finally { busy=false }
  }}) { Text("Verify and install") }},dismissButton={TextButton(enabled=!busy,onClick={installReview=null}) { Text("Cancel") }}) }
  review?.let { prepared -> AlertDialog(onDismissRequest={if(!busy) review=null},title={Text("Endorse ${prepared.app.name} (devnet demo)")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   Text("Send 0.001 SOL + network fee. This is a demo transfer, not SKR staking.")
   Text("From: ${Base58.encode(prepared.payer)}",fontSize=11.sp)
   Text("To: ${prepared.recipient}",fontSize=11.sp)
   Text("Message SHA-256: ${prepared.binding.hash}",fontSize=11.sp)
   Text("Exact message (base64): ${android.util.Base64.encodeToString(prepared.binding.message(),android.util.Base64.NO_WRAP)}",fontSize=9.sp)
   Text("Review expires after 60 seconds. Wallet will ask for signing approval.",fontSize=12.sp)
  }},confirmButton={TextButton(enabled=!busy,onClick={lifecycleScope.launch {
   busy=true
   try { status=wallet.approve(prepared); review=null; tab=2 }
   catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; status=e.message ?: "Wallet or network error"; review=null }
   finally { busy=false }
  }}) { Text("Approve exact message") }},dismissButton={TextButton(enabled=!busy,onClick={review=null}) { Text("Cancel") }}) }
 }
 @Composable private fun BadgeText(text: String) { Surface(color=MaterialTheme.colorScheme.primary.copy(alpha=.14f),shape=RoundedCornerShape(8.dp)) { Text(text,Modifier.padding(horizontal=7.dp,vertical=4.dp),fontSize=9.sp) } }
}

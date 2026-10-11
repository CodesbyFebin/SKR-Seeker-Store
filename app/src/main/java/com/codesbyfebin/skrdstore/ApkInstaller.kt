package com.codesbyfebin.skrdstore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Single APK only. Metadata is trusted via the configured HTTPS catalog; no silent install. */
class ApkInstaller(private val context: Context) {
 companion object { private val downloadMutex=Mutex() }
 @Volatile private var activeCall: okhttp3.Call?=null
 fun cancel() {activeCall?.cancel()}
 private fun fetch(url: String): okhttp3.Response {
  var current=url; val original=java.net.URI(url).host
  repeat(6) {
   val call=http.newCall(Request.Builder().url(current).build());activeCall=call
   val response=call.execute()
   if(response.code !in listOf(301,302,303,307,308)) return response
   val location=response.header("Location");response.close();check(location!=null) {"Redirect has no location"}
   val next=java.net.URI(current).resolve(location); val githubHosts=setOf("github.com","objects.githubusercontent.com","release-assets.githubusercontent.com","github-releases.githubusercontent.com")
   require(next.scheme=="https" && next.userInfo==null && (next.host==original || original=="github.com" && next.host in githubHosts)) {"APK redirect host is not allowed"}
   current=next.toString()
  }
  error("Too many APK redirects")
 }
 private val http=OkHttpClient.Builder().followRedirects(false).connectTimeout(20,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).build()
 @Suppress("DEPRECATION")
 private val flags get()=if(Build.VERSION.SDK_INT>=28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
 @Suppress("DEPRECATION")
 private fun code(p: PackageInfo)=if(Build.VERSION.SDK_INT>=28) p.longVersionCode else p.versionCode.toLong()
 @Suppress("DEPRECATION")
 private fun certificates(p: PackageInfo): List<String> {
  val signs=if(Build.VERSION.SDK_INT>=28) p.signingInfo?.apkContentsSigners else p.signatures
  return signs?.map { sha(it.toByteArray()) } ?: emptyList()
 }
 private fun sha(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
 @Suppress("DEPRECATION")
 fun action(r: ApkRelease): String = try {
  val existing=context.packageManager.getPackageInfo(r.packageName,flags)
  if(certificates(existing)!=listOf(r.signerSha256)) "Signer mismatch"
  else { InstallHistory(context).observed(r.appId,code(existing)); if(code(existing)>=r.versionCode) "Open" else "Update" }
 } catch(e: PackageManager.NameNotFoundException) { "Install" }
 @Suppress("DEPRECATION")
 suspend fun download(r: ApkRelease, progress: (Long,Long) -> Unit = { _,_ -> }): File = withContext(Dispatchers.IO) { downloadMutex.withLock {
  val dir=File(context.cacheDir,"apks").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
  val part=File(dir,"download.partial"); val dest=File(dir,"${r.sha256}.apk")
  try {
   fetch(r.url).use { response ->
    check(response.isSuccessful) { "APK HTTP ${response.code}" }
    val body=response.body ?: error("No APK response"); require(body.contentLength() <= 500_000_000L)
    val digest=MessageDigest.getInstance("SHA-256"); var count=0L; var lastProgress=0L
    body.byteStream().use { input -> part.outputStream().use { output ->
     val buffer=ByteArray(65536)
     while(true) { coroutineContext.ensureActive(); val n=input.read(buffer); if(n<0) break; count+=n; val now=System.currentTimeMillis(); if(now-lastProgress>=100 || count==body.contentLength()) {progress(count,body.contentLength());lastProgress=now}; require(count<=500_000_000L) { "APK too large" }; digest.update(buffer,0,n); output.write(buffer,0,n) }
    } }
    require(digest.digest().joinToString("") { "%02x".format(it) }==r.sha256) { "APK checksum mismatch" }
   }
   check(part.renameTo(dest))
   require(ApkArchitecture.compatible(ApkArchitecture.archiveAbis(dest),Build.SUPPORTED_ABIS.toList())) { "APK native libraries do not support this device" }
   val info=context.packageManager.getPackageArchiveInfo(dest.absolutePath,flags) ?: error("Invalid standalone APK")
   require(info.packageName==r.packageName && code(info)==r.versionCode) { "APK package or version mismatch" }
   require(certificates(info)==listOf(r.signerSha256)) { "APK signing certificate mismatch" }
   require((info.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE)<=Build.VERSION.SDK_INT) { "Android version unsupported" }
   val installed=try { context.packageManager.getPackageInfo(r.packageName,flags) } catch(e: PackageManager.NameNotFoundException) { null }
   if(installed!=null) {
    require(code(info)>code(installed)) { "Same or newer version already installed" }
    require(certificates(installed)==certificates(info)) { "Update signer differs from installed app" }
   }
   dest
  } catch(e: Exception) { dest.delete(); throw e } finally { activeCall=null; part.delete() }
 } }
 fun open(r: ApkRelease) {
  check(action(r)=="Open") { "Installed package identity needs review" }
  val intent=context.packageManager.getLaunchIntentForPackage(r.packageName) ?: error("App has no launcher")
  context.startActivity(intent)
 }
 fun launch(file: File) {
  check(context.packageManager.canRequestPackageInstalls()) { "Enable this app in Android's Install unknown apps settings, then retry." }
  val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
  context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
 }
}

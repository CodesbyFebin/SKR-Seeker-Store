package com.codesbyfebin.skrdstore

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApkRelease(val appId: String, val packageName: String, val versionCode: Long, val url: String, val sha256: String, val signerSha256: String, val assetName: String = "release.apk", val versionName: String = "", val sourceRepo: String = "", val changelog: String = "")
class CatalogSource(context: Context) {
 private val secure = SecureStore(context)
 private val http = OkHttpClient.Builder().followRedirects(false).connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
 fun cached(): Map<String,ApkRelease> = runCatching { parse(secure.get("catalogJson") ?: "{\"apps\":[]}") }.getOrDefault(emptyMap())
 suspend fun refresh(): Map<String,ApkRelease> = withContext(Dispatchers.IO) {
  require(BuildConfig.CATALOG_URL.startsWith("https://")) { "Configure CATALOG_URL to your reviewed HTTPS Worker /api/catalog endpoint." }
  http.newCall(Request.Builder().url(BuildConfig.CATALOG_URL).build()).execute().use { response ->
   check(response.isSuccessful) { "Catalog HTTP ${response.code}" }
   val body = response.body ?: error("Empty catalog")
   require(body.contentLength() <= 2_000_000L)
   val bytes = body.byteStream().use { it.readBytesLimited(2_000_000) }
   val json = bytes.toString(Charsets.UTF_8); val parsed = parse(json)
   secure.put("catalogJson",json); parsed
  }
 }
 private fun parse(json: String): Map<String,ApkRelease> {
  val root=JSONObject(json); val apps=root.getJSONArray("apps"); require(apps.length() <= 100)
  val out=mutableMapOf<String,ApkRelease>()
  for(i in 0 until apps.length()) {
   val item=apps.getJSONObject(i); val id=item.getString("id")
   if(Catalog.apps.none {it.id==id} && id !in UtilityCatalog.ids) continue
   val variants=item.optJSONArray("variants")
   val options=if(variants!=null) (0 until variants.length()).map {variants.getJSONObject(it)} else listOfNotNull(item.optJSONObject("apk"))
   require(options.size<=20)
   val apk=options.sortedBy { ApkArchitecture.rank(it.optString("assetName","release.apk"),android.os.Build.SUPPORTED_ABIS.toList()) }.firstOrNull { ApkArchitecture.rank(it.optString("assetName","release.apk"),android.os.Build.SUPPORTED_ABIS.toList())!=Int.MAX_VALUE } ?: continue
   val r=ApkRelease(id,apk.getString("packageName"),apk.getLong("versionCode"),apk.getString("url"),apk.getString("sha256").lowercase(),apk.getString("signerSha256").lowercase(),apk.optString("assetName","release.apk"),item.optString("versionName").take(80),item.optString("sourceRepo"),item.optString("changelog").take(3000))
   if(id in UtilityCatalog.ids) require(r.packageName=="com.codesbyfebin.seeker.$id") { "Utility package identity mismatch" }
   require(r.assetName.endsWith(".apk",true))
   require(r.sourceRepo.isEmpty() || r.sourceRepo.matches(Regex("https://github.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))
   require(r.packageName.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")) && r.versionCode > 0)
   require(r.sha256.matches(Regex("[a-f0-9]{64}")) && r.signerSha256.matches(Regex("[a-f0-9]{64}")))
   val uri=java.net.URI(r.url); require(uri.scheme=="https" && uri.host!=null && uri.userInfo==null && uri.fragment==null)
   require(out.put(id,r)==null) { "Duplicate app release" }
  }
  return out
 }
}
fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
 val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(65536)
 while(true) { val n=read(buffer); if(n<0) break; require(out.size()+n <= max) { "Response too large" }; out.write(buffer,0,n) }
 return out.toByteArray()
}

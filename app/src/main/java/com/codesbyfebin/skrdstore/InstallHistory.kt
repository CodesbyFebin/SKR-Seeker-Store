package com.codesbyfebin.skrdstore

import android.content.Context
import org.json.JSONObject
/** Encrypted handoff history. Only PackageManager observation is labeled installed. */
class InstallHistory(context: Context) {
 private val secure=SecureStore(context)
 fun pending(r: ApkRelease) {
  val data=JSONObject(secure.get("installHistory") ?: "{}")
  data.put(r.appId,JSONObject().put("packageName",r.packageName).put("versionCode",r.versionCode).put("signerSha256",r.signerSha256).put("state","AWAITING_ANDROID").put("time",System.currentTimeMillis()))
  secure.put("installHistory",data.toString())
 }
 fun observed(appId: String, version: Long) {
  val data=JSONObject(secure.get("installHistory") ?: "{}"); val row=data.optJSONObject(appId) ?: return
  if(version>=row.getLong("versionCode")) {row.put("state","INSTALLED");secure.put("installHistory",data.toString())}
 }
 fun favorites(): Set<String> = secure.get("favorites")?.split(',')?.filter {it.isNotEmpty()}?.toSet() ?: emptySet()
 fun favorite(id: String) { val set=favorites().toMutableSet();if(!set.add(id)) set.remove(id);secure.put("favorites",set.sorted().joinToString(",")) }
 fun updates(ids: List<String>) { secure.put("availableUpdates",ids.joinToString(",")) }
}

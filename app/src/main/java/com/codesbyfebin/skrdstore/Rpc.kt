package com.codesbyfebin.skrdstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Rpc {
 private val client = OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).build()
 private suspend fun call(method: String, params: JSONArray): JSONObject = withContext(Dispatchers.IO) {
  require(BuildConfig.RPC_URL.startsWith("https://"))
  val body = JSONObject().put("jsonrpc","2.0").put("id",1).put("method",method).put("params",params).toString()
  client.newCall(Request.Builder().url(BuildConfig.RPC_URL).post(body.toRequestBody("application/json".toMediaType())).build()).execute().use {
   check(it.isSuccessful) { "RPC HTTP ${it.code}" }
   val json = JSONObject(it.body?.string() ?: error("Empty RPC response"))
   check(!json.has("error")) { "RPC rejected $method" }; json
  }
 }
 suspend fun verifyDevnet() { check(call("getGenesisHash",JSONArray()).getString("result") == "EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG") { "RPC is not devnet" } }
 suspend fun balance(wallet: String): Long = call("getBalance",JSONArray().put(wallet).put(JSONObject().put("commitment","confirmed"))).getJSONObject("result").getLong("value")
 suspend fun blockhash(): String = call("getLatestBlockhash",JSONArray().put(JSONObject().put("commitment","confirmed"))).getJSONObject("result").getJSONObject("value").getString("blockhash")
 suspend fun send(bytes: ByteArray): String = call("sendTransaction",JSONArray().put(android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)).put(JSONObject().put("encoding","base64").put("skipPreflight",false).put("preflightCommitment","confirmed"))).getString("result")
 suspend fun status(tx: String, messageHash: String): String {
  val value = call("getSignatureStatuses",JSONArray().put(JSONArray().put(tx)).put(JSONObject().put("searchTransactionHistory",true))).getJSONObject("result").getJSONArray("value")
  if(value.isNull(0)) return "PENDING"
  val row = value.getJSONObject(0)
  if(!row.isNull("err")) return "FAILED"
  if(row.optString("confirmationStatus") !in listOf("confirmed","finalized")) return "PENDING"
  // A status alone cannot bind a signature to this receipt. Read confirmed bytes too.
  val confirmed = call("getTransaction",JSONArray().put(tx).put(JSONObject().put("encoding","base64").put("commitment","confirmed").put("maxSupportedTransactionVersion",0)))
  if(confirmed.isNull("result")) return "PENDING"
  val result = confirmed.getJSONObject("result")
  if(!result.getJSONObject("meta").isNull("err")) return "FAILED"
  val bytes = android.util.Base64.decode(result.getJSONArray("transaction").getString(0),android.util.Base64.NO_WRAP)
  return if(SolanaWire.matchesConfirmedReceipt(bytes,tx,messageHash)) "CONFIRMED" else "FAILED"
 }
}

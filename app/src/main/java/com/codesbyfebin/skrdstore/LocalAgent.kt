package com.codesbyfebin.skrdstore

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class LocalAgent(private val context: Context) {
 companion object { private val mutex = Mutex() }
 private val model get() = File(context.filesDir,"qwen2.5-1.5b-instruct-q4_k_m.gguf")
 private val nativeReady = runCatching { System.loadLibrary("skr_llama") }.isSuccess
 fun ready() = nativeReady && model.exists() && BuildConfig.MODEL_SHA256.length == 64
 suspend fun importModel(uri: Uri) = withContext(Dispatchers.IO) { mutex.withLock {
  require(BuildConfig.MODEL_SHA256.matches(Regex("[a-fA-F0-9]{64}"))) { "Configure trusted MODEL_SHA256 before importing" }
  val temp = File(context.filesDir,"model.partial")
  try {
   val digest = MessageDigest.getInstance("SHA-256"); var size = 0L
   context.contentResolver.openInputStream(uri)!!.use { input -> temp.outputStream().use { output ->
    val buffer = ByteArray(65536)
    while(true) { val n = input.read(buffer); if(n < 0) break; size += n; require(size <= 2_000_000_000L); digest.update(buffer,0,n); output.write(buffer,0,n) }
   } }
   require(digest.digest().joinToString("") { "%02x".format(it) }.equals(BuildConfig.MODEL_SHA256,true)) { "Model checksum mismatch" }
   temp.inputStream().use { val header = ByteArray(4); require(it.read(header) == 4 && header.contentEquals("GGUF".toByteArray())) }
   check(temp.renameTo(model)) { "Unable to store model" }
  } finally { temp.delete() }
 } }
 suspend fun ask(question: String, rows: List<Endorsement>): String = withContext(Dispatchers.Default) { mutex.withLock {
  check(ready()) { "Import the verified Qwen model and build the native runtime first." }
  require(question.length in 1..1000)
  val catalog = Catalog.apps.joinToString("\n") { "${it.name} | ${it.category} | weight ${Catalog.weight(it,rows)} | ${it.contribution}" }
  val profile = rows.filter { it.status == "CONFIRMED" }.joinToString { "${it.appId}: ${it.lamports} demo lamports" }.take(2000)
  val system = "You are a DePIN app recommender on the Solana Seeker. Recommend exactly 3 distinct apps from the supplied list, with one-line reasons. Output precisely three lines: 1. APP_NAME: reason, 2. APP_NAME: reason, 3. APP_NAME: reason. APP_NAME must be an exact catalog name, never a category. Prefer DePIN apps for a general endorsement question. Tie reasons to their listed contribution types and demo weights. Never claim verified security or actual token returns. Be concise. No cloud calls. SKR weights are demo values, and endorsements are devnet SOL transfers, not SKR stakes.\nApps:\n$catalog\nUser endorsements: $profile"
  val result = generate(model.absolutePath,system,question)
  val lines = result.trim().lines()
  val names = lines.mapIndexed { i, line ->
   require(line.startsWith("${i+1}. ") && line.contains(": ")) { "Model output incomplete. Try again." }
   val name = line.substringAfter(". ").substringBefore(": ")
   require(Catalog.apps.any { it.name == name } && line.substringAfter(": ").length >= 8) { "Invalid model recommendation. Try again." }
   name
  }
  require(lines.size == 3 && names.distinct().size == 3) { "Model output needs three different apps. Try again." }
  result
 } }
 private external fun generate(path: String, system: String, question: String): String
}

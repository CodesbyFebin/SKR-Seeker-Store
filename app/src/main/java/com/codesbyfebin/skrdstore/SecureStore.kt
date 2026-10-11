package com.codesbyfebin.skrdstore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import android.util.Base64

class SecureStore(context: Context) {
 private val prefs = EncryptedSharedPreferences.create(context, "secure", MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(), EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
 fun get(key: String) = prefs.getString(key, null)
 fun put(key: String, value: String) { check(prefs.edit().putString(key,value).commit()) }
 fun dbKey(): ByteArray {
  val existing = get("dbKey")
  if(existing != null) return Base64.decode(existing, Base64.NO_WRAP)
  val bytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
  put("dbKey", Base64.encodeToString(bytes, Base64.NO_WRAP)); return bytes
 }
 fun sign(payload: ByteArray): Pair<String,String> {
  val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  if(!ks.containsAlias("receipt")) KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore").apply {
   initialize(KeyGenParameterSpec.Builder("receipt", KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).build()); generateKeyPair()
  }
  val signature = Signature.getInstance("SHA256withECDSA").apply { initSign(ks.getKey("receipt",null) as java.security.PrivateKey); update(payload) }.sign()
  return Base64.encodeToString(signature,Base64.NO_WRAP) to Base64.encodeToString(ks.getCertificate("receipt").publicKey.encoded,Base64.NO_WRAP)
 }
}

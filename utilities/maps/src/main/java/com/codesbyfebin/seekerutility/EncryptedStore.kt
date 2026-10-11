package com.codesbyfebin.seekerutility

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** App-private Android Keystore encryption. No Seed Vault or TEE attestation claim. */
class EncryptedStore(private val context: Context, private val authenticated: Boolean) {
 private val file = AtomicFile(File(context.filesDir,"records.enc"))
 private fun key(): SecretKey {
  val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (ks.getKey("utility-v1",null) as? SecretKey)?.let { return it }
  val generator = KeyGenerator.getInstance("AES","AndroidKeyStore")
  val spec = KeyGenParameterSpec.Builder("utility-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
  if(authenticated) spec.setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(30)
  generator.init(spec.build());return generator.generateKey()
 }
 @Synchronized fun read(): String {
  if(!file.baseFile.exists()) return ""
  val bytes = file.readFully(); require(bytes.size in 28..2_000_000)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
  return cipher.doFinal(bytes.copyOfRange(12,bytes.size)).toString(Charsets.UTF_8)
 }
 @Synchronized fun write(value: String) {
  require(value.toByteArray().size<=1_000_000)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
  val bytes=cipher.iv+cipher.doFinal(value.toByteArray())
  val stream=file.startWrite()
  try { stream.write(bytes);file.finishWrite(stream) } catch(e:Exception){file.failWrite(stream);throw e}
 }
}

/** Portable backup is passphrase-based; device Keystore keys are never exported. */
object PortableBackup {
 private val magic="SSB1".toByteArray()
 private fun key(password:CharArray,salt:ByteArray):SecretKeySpec {
  require(password.size>=12) { "Use at least 12 characters" }
  val spec=PBEKeySpec(password,salt,210_000,256)
  return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") } finally {spec.clearPassword()}
 }
 fun encrypt(data:ByteArray,password:CharArray):ByteArray {
  require(data.size<=10_000_000)
  val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt));cipher.updateAAD(magic)
  return magic+salt+cipher.iv+cipher.doFinal(data)
 }
 fun decrypt(data:ByteArray,password:CharArray):ByteArray {
  require(data.size in 48..10_000_048 && data.copyOfRange(0,4).contentEquals(magic)) { "Unsupported backup" }
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,data.copyOfRange(4,20)),GCMParameterSpec(128,data.copyOfRange(20,32)));cipher.updateAAD(magic)
  return cipher.doFinal(data.copyOfRange(32,data.size))
 }
}

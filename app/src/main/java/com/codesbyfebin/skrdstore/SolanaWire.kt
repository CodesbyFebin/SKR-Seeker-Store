package com.codesbyfebin.skrdstore

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

object Base58 {
 private const val chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
 fun decode(s: String): ByteArray {
  require(s.isNotEmpty() && s.length <= 128)
  var n = BigInteger.ZERO
  s.forEach { val i = chars.indexOf(it); require(i >= 0); n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(i.toLong())) }
  val raw = if (n == BigInteger.ZERO) byteArrayOf() else n.toByteArray().let { if(it[0] == 0.toByte()) it.drop(1).toByteArray() else it }
  return ByteArray(s.takeWhile { it == '1' }.length) + raw
 }
 fun encode(bytes: ByteArray): String {
  var n = BigInteger(1, bytes); val result = StringBuilder()
  while(n > BigInteger.ZERO) { val qr = n.divideAndRemainder(BigInteger.valueOf(58)); result.append(chars[qr[1].toInt()]); n = qr[0] }
  repeat(bytes.takeWhile { it == 0.toByte() }.size) { result.append('1') }
  return result.reverse().toString()
 }
}
object SolanaWire {
 const val LAMPORTS = 1_000_000L
 fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
 fun message(payer: ByteArray, recipient: ByteArray, blockhash: ByteArray): ByteArray {
  require(payer.size == 32 && recipient.size == 32 && blockhash.size == 32)
  require(!payer.contentEquals(recipient)); require(recipient.any { it != 0.toByte() })
  // Legacy header: 1 required signer, 0 readonly signers, 1 readonly unsigned.
  return ByteArrayOutputStream().apply {
   write(byteArrayOf(1,0,1,3)); write(payer); write(recipient); write(ByteArray(32)); write(blockhash)
   write(byteArrayOf(1,2,2,0,1,12))
   write(ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(2).putLong(LAMPORTS).array())
  }.toByteArray()
 }
 fun unsigned(message: ByteArray) = byteArrayOf(1) + ByteArray(64) + message
 fun matchesConfirmedReceipt(signed: ByteArray, tx: String, messageHash: String): Boolean =
  signed.size == 215 && signed[0] == 1.toByte() && Base58.encode(signed.copyOfRange(1,65)) == tx && hash(signed.copyOfRange(65,signed.size)) == messageHash
 fun checkedSigned(signed: ByteArray, expected: ByteArray): ByteArray {
  require(signed.size == 65 + expected.size && signed[0] == 1.toByte()) { "Invalid signed transaction" }
  require(signed.copyOfRange(65, signed.size).contentEquals(expected)) { "MESSAGE_CHANGED" }
  val signature = signed.copyOfRange(1,65)
  require(signature.any { it != 0.toByte() }) { "Missing signature" }
  return signature
 }
}
class ReviewBinding(message: ByteArray) {
 private val frozen = message.copyOf()
 val hash = SolanaWire.hash(frozen)
 fun message() = frozen.copyOf()
 fun assertUnchanged(candidate: ByteArray) { require(candidate.contentEquals(frozen) && SolanaWire.hash(candidate) == hash) { "MESSAGE_CHANGED" } }
}

package com.codesbyfebin.skrdstore
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
class SolanaWireTest {
 @Test fun exactTransferWireFormat() {
  val message=SolanaWire.message(ByteArray(32){1},ByteArray(32){2},ByteArray(32){3})
  assertEquals(150,message.size)
  assertEquals(1,message[0].toInt()); assertEquals(3,message[3].toInt())
  val data=ByteBuffer.wrap(message.copyOfRange(138,150)).order(ByteOrder.LITTLE_ENDIAN)
  assertEquals(2,data.int); assertEquals(1_000_000L,data.long)
 }
 @Test fun bindingRejectsMutationAndDefensiveCopies() {
  val bytes=ByteArray(150){1}; val binding=ReviewBinding(bytes); bytes[0]=2
  assertEquals(1,binding.message()[0].toInt())
  val returned=binding.message(); returned[10]=3
  assertThrows(IllegalArgumentException::class.java) { binding.assertUnchanged(returned) }
 }
 @Test fun rejectWalletMessageChange() {
  val msg=SolanaWire.message(ByteArray(32){1},ByteArray(32){2},ByteArray(32){3})
  val signed=SolanaWire.unsigned(msg); signed[1]=9
  assertEquals(64,SolanaWire.checkedSigned(signed,msg).size)
  signed[signed.lastIndex]=42
  assertThrows(IllegalArgumentException::class.java) { SolanaWire.checkedSigned(signed,msg) }
 }
 @Test fun base58PreservesLeadingZeros() {
  val bytes=byteArrayOf(0,0,1,127,-1)
  assertArrayEquals(bytes,Base58.decode(Base58.encode(bytes)))
  assertArrayEquals(ByteArray(32),Base58.decode("11111111111111111111111111111111"))
 }
 @Test fun refuseSystemProgramAsRecipient() {
  assertThrows(IllegalArgumentException::class.java) { SolanaWire.message(ByteArray(32){1},ByteArray(32),ByteArray(32){3}) }
 }
 @Test fun confirmedBytesBindSignatureAndMessage() {
  val msg=SolanaWire.message(ByteArray(32){1},ByteArray(32){2},ByteArray(32){3})
  val signed=SolanaWire.unsigned(msg); signed[1]=9
  val tx=Base58.encode(signed.copyOfRange(1,65)); val hash=SolanaWire.hash(msg)
  assertTrue(SolanaWire.matchesConfirmedReceipt(signed,tx,hash))
  signed[70]=42
  assertFalse(SolanaWire.matchesConfirmedReceipt(signed,tx,hash))
 }
 @Test fun rejectUnrelatedConfirmedSignature() {
  val msg=SolanaWire.message(ByteArray(32){1},ByteArray(32){2},ByteArray(32){3})
  val signed=SolanaWire.unsigned(msg); signed[1]=9
  val tx=Base58.encode(ByteArray(64){7})
  assertFalse(SolanaWire.matchesConfirmedReceipt(signed,tx,SolanaWire.hash(msg)))
 }
}

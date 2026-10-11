package com.codesbyfebin.skrdstore

import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.*
import org.json.JSONObject
import kotlinx.coroutines.delay

class WalletFlow(private val sender: ActivityResultSender, private val secure: SecureStore, private val dao: EndorsementDao) {
 private val adapter = MobileWalletAdapter(ConnectionIdentity(Uri.parse("https://skr-dstore.local"), Uri.parse("favicon.ico"), "Seeker Store")).apply { blockchain = Solana.Devnet; authToken = secure.get("walletAuth") }
 private val rpc = Rpc()
 data class Prepared(val app: DApp, val payer: ByteArray, val recipient: String, val binding: ReviewBinding, val balance: Long, val created: Long)
 suspend fun prepare(app: DApp): Prepared {
  require(BuildConfig.ENDORSEMENT_RECIPIENT.isNotBlank()) { "Configure a reviewed devnet endorsement recipient first." }
  val recipient = Base58.decode(BuildConfig.ENDORSEMENT_RECIPIENT)
  require(recipient.size == 32 && recipient.any { it != 0.toByte() }) { "Invalid recipient" }
  rpc.verifyDevnet()
  val connection = adapter.connect(sender)
  val payer = when(connection) {
   is TransactionResult.Success -> connection.authResult.accounts.first().publicKey.copyOf()
   is TransactionResult.NoWalletFound -> error("No compatible wallet installed")
   is TransactionResult.Failure -> error("Wallet connection declined or failed")
  }
  adapter.authToken?.let { secure.put("walletAuth",it) }
  val balance = rpc.balance(Base58.encode(payer))
  require(balance >= SolanaWire.LAMPORTS + 5000) { "Insufficient devnet SOL for transfer and fee" }
  return Prepared(app,payer,BuildConfig.ENDORSEMENT_RECIPIENT,ReviewBinding(SolanaWire.message(payer,recipient,Base58.decode(rpc.blockhash()))),balance,System.currentTimeMillis())
 }
 suspend fun approve(prepared: Prepared): String {
  require(System.currentTimeMillis() - prepared.created < 60_000) { "Review expired. Prepare and review again." }
  rpc.verifyDevnet()
  val message = prepared.binding.message()
  prepared.binding.assertUnchanged(message)
  val signed = when(val result = adapter.transact(sender) { auth ->
   require(auth.accounts.first().publicKey.contentEquals(prepared.payer)) { "Wallet account changed. Review again." }
   prepared.binding.assertUnchanged(message)
   // MOCK SKR staking: exact 0.001 devnet SOL SystemProgram transfer only.
   signTransactions(arrayOf(SolanaWire.unsigned(message)))
  }) {
   is TransactionResult.Success -> result.payload.signedPayloads.single()
   is TransactionResult.NoWalletFound -> error("No compatible wallet installed")
   is TransactionResult.Failure -> error("Signing rejected or wallet unavailable")
  }
  val signatureBytes = SolanaWire.checkedSigned(signed,message)
  val tx = Base58.encode(signatureBytes)
  val now = System.currentTimeMillis()
  val receipt = JSONObject().put("schema","skr-dstore-receipt/v1").put("appId",prepared.app.id).put("chain","solana:devnet").put("wallet",Base58.encode(prepared.payer)).put("recipient",prepared.recipient).put("lamports",SolanaWire.LAMPORTS).put("messageSha256",prepared.binding.hash).put("transaction",tx).put("created",now).put("staking","MOCK_SOL_TRANSFER").toString()
  val (sig,pub) = secure.sign(receipt.toByteArray(Charsets.UTF_8))
  // Persist before broadcast so a network timeout cannot lose a submitted receipt.
  dao.insert(Endorsement(tx,prepared.app.id,Base58.encode(prepared.payer),SolanaWire.LAMPORTS,now,prepared.binding.hash,receipt,sig,pub))
  val sent = rpc.send(signed); check(sent == tx) { "RPC signature mismatch" }
  repeat(12) {
   val status = rpc.status(tx,prepared.binding.hash); dao.status(tx,status)
   if(status == "CONFIRMED") return "Confirmed: ${prepared.app.name} endorsement recorded."
   if(status == "FAILED") error("Devnet transaction failed; no weight added")
   delay(2000)
  }
  return "Submitted; confirmation pending. Weight updates after confirmation."
 }
}

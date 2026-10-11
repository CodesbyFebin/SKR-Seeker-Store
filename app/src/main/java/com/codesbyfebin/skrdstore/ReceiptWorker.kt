package com.codesbyfebin.skrdstore

import android.app.Application
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class DStoreApp: Application() {
 override fun onCreate() {
  super.onCreate()
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("refresh-catalog",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<CatalogWorker>(24,TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("confirm-receipts",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<ReceiptWorker>(15,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
 }
}
class ReceiptWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
 override suspend fun doWork(): Result = try {
  val dao = DStoreDb.get(applicationContext).endorsements(); val rpc = Rpc(); rpc.verifyDevnet()
  dao.pending().forEach { dao.status(it.tx,rpc.status(it.tx,it.messageHash)) }; Result.success()
 } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; Result.retry() }
}

class CatalogWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
 override suspend fun doWork(): Result {
  if(BuildConfig.CATALOG_URL.isBlank()) return Result.success()
  return try { val releases=CatalogSource(applicationContext).refresh(); val installer=ApkInstaller(applicationContext); InstallHistory(applicationContext).updates(releases.values.filter {installer.action(it)=="Update"}.map {it.appId}); Result.success() } catch(e: Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; Result.retry() }
 }
}

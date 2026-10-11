package com.codesbyfebin.skrdstore

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Entity data class Endorsement(@PrimaryKey val tx: String, val appId: String, val wallet: String, val lamports: Long, val created: Long, val messageHash: String, val receipt: String, val receiptSignature: String, val receiptPublicKey: String, val status: String = "PENDING")
@Dao interface EndorsementDao {
 @Query("SELECT * FROM Endorsement ORDER BY created DESC") fun observe(): Flow<List<Endorsement>>
 @Query("SELECT * FROM Endorsement WHERE status = 'PENDING'") suspend fun pending(): List<Endorsement>
 @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(row: Endorsement)
 @Query("UPDATE Endorsement SET status = :status WHERE tx = :tx") suspend fun status(tx: String, status: String)
}
@Database(entities=[Endorsement::class],version=1,exportSchema=false)
abstract class DStoreDb: RoomDatabase() {
 abstract fun endorsements(): EndorsementDao
 companion object {
  @Volatile private var instance: DStoreDb? = null
  fun get(context: Context): DStoreDb = instance ?: synchronized(this) {
   instance ?: run {
    SQLiteDatabase.loadLibs(context)
    Room.databaseBuilder(context.applicationContext,DStoreDb::class.java,"endorsements.db").openHelperFactory(SupportFactory(SecureStore(context).dbKey())).build().also { instance = it }
   }
  }
 }
}

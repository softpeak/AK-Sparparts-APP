package com.akspareparts.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akspareparts.app.data.dao.BillDao
import com.akspareparts.app.data.dao.CustomerDao
import com.akspareparts.app.data.dao.CustomerPartDao
import com.akspareparts.app.data.dao.PartDao
import com.akspareparts.app.data.dao.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Customer::class, Part::class, CustomerPart::class, Bill::class, BillItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun partDao(): PartDao
    abstract fun customerPartDao(): CustomerPartDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ak_spareparts.db"
                ).addCallback(SeedCallback(context)).build()
                INSTANCE = db
                db
            }
        }
    }

    private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    // Seed users
                    database.userDao().insert(User(username = "immy", password = "117", fullName = "Imran"))
                    database.userDao().insert(User(username = "bagni", password = "118", fullName = "Sulman"))
                    // Seed global parts catalog
                    database.partDao().insertAll(SeedData.PARTS)
                }
            }
        }
    }
}

object SeedData {
    val PARTS: List<Part> = listOf(
        Part(partNumber = "90919-01217", price = 24.5),
        Part(partNumber = "04466-78020", price = 102.0),
        Part(partNumber = "04466-30230", price = 108.0),
        Part(partNumber = "31210-26172", price = 266.0),
        Part(partNumber = "90919-01275", price = 31.0),
        Part(partNumber = "88440-25070", price = 88.0),
        Part(partNumber = "16620-36030", price = 210.0),
        Part(partNumber = "43512-60180", price = 270.0),
        Part(partNumber = "04466-60161", price = 120.0),
        Part(partNumber = "90919-01191", price = 25.5),
        Part(partNumber = "04465-12610", price = 149.0),
        Part(partNumber = "23300-31100", price = 88.0),
        Part(partNumber = "08880-83714", price = 47.0),
        Part(partNumber = "04465-42190", price = 170.0),
        Part(partNumber = "04465-33480", price = 165.0),
        Part(partNumber = "04465-35290", price = 180.0),
        Part(partNumber = "2220475020", price = 150.0),
        Part(partNumber = "1301138120", price = 300.0),
        Part(partNumber = "781100K030", price = 145.0),
        Part(partNumber = "1630750012", price = 280.0),
        Part(partNumber = "4246060030", price = 380.0),
        Part(partNumber = "0446660080", price = 115.0),
        Part(partNumber = "0446535290", price = 180.0),
        Part(partNumber = "0446526440", price = 185.0),
        Part(partNumber = "4356060010", price = 185.0),
        Part(partNumber = "9091901176", price = 4.3),
        Part(partNumber = "4243112310", price = 200.0),
        Part(partNumber = "04466-33200", price = 109.0)
    )
}

package com.akspareparts.app

import android.app.Application
import com.akspareparts.app.data.AppDatabase
import com.akspareparts.app.data.Repository
import com.akspareparts.app.data.SeedData
import com.akspareparts.app.prefs.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Simple service locator so ViewModels can grab the repository/session without DI framework. */
class AKApplication : Application() {

    lateinit var repository: Repository
        private set
    lateinit var session: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = Repository(
            userDao = db.userDao(),
            customerDao = db.customerDao(),
            partDao = db.partDao(),
            customerPartDao = db.customerPartDao(),
            billDao = db.billDao()
        )
        session = SessionManager(this)

        // Top up the global catalog with any seed parts that aren't present yet.
        // insertAll() ignores conflicts (partNumber is unique), so this is safe to
        // run on every launch and adds newly-shipped parts to existing installs.
        // When SeedData.VERSION is bumped, seed prices are re-applied once and
        // removed part numbers are cleaned up.
        val seedPrefs = getSharedPreferences("seed_prefs", MODE_PRIVATE)
        CoroutineScope(Dispatchers.IO).launch {
            db.partDao().insertAll(SeedData.PARTS)
            if (seedPrefs.getInt("seed_version", 1) < SeedData.VERSION) {
                SeedData.PARTS.forEach {
                    db.partDao().updatePriceByPartNumber(it.partNumber, it.price)
                }
                SeedData.REMOVED.forEach {
                    db.partDao().deleteByPartNumber(it)
                }
                seedPrefs.edit().putInt("seed_version", SeedData.VERSION).apply()
            }
        }
    }
}

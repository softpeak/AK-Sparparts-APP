package com.akspareparts.app

import android.app.Application
import com.akspareparts.app.data.AppDatabase
import com.akspareparts.app.data.Repository
import com.akspareparts.app.prefs.SessionManager

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
    }
}

package com.pisheblok.app

import android.app.Application
import androidx.room.Room
import com.pisheblok.app.auth.AuthManager
import com.pisheblok.app.data.local.AppDatabase
import com.pisheblok.app.data.repo.Repository
import com.pisheblok.app.sync.NetworkObserver
import com.pisheblok.app.sync.SyncManager
import com.pisheblok.app.widget.refreshWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Simple manual dependency container. */
@OptIn(FlowPreview::class)
class AppContainer(app: Application) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "pisheblok.db")
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()

    val repository = Repository(database.locationDao(), database.itemDao(), database.shoppingDao())
    val network = NetworkObserver(app)
    val auth = AuthManager(app)
    val sync = SyncManager(appScope, repository, auth, network)

    init {
        sync.start()
        // keep the home-screen widget in sync with local data changes
        appScope.launch {
            repository.changes.debounce(3000).collect {
                refreshWidget(app)
            }
        }
    }
}

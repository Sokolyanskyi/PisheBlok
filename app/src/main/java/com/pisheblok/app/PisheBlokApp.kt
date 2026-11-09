package com.pisheblok.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.pisheblok.app.reminders.RemindersWorker
import java.util.concurrent.TimeUnit

class PisheBlokApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        RemindersWorker.ensureChannel(this)
        initFirebase()
        container = AppContainer(this)
        scheduleReminders()
    }

    private fun scheduleReminders() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RemindersWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RemindersWorker>(12, TimeUnit.HOURS).build(),
        )
    }

    /**
     * Manual Firebase init: values come from res/values/firebase_config.xml.
     * Empty values -> the app runs in local-only mode without the cloud.
     */
    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        val apiKey = getString(R.string.firebase_api_key).trim()
        val projectId = getString(R.string.firebase_project_id).trim()
        val appId = getString(R.string.firebase_app_id).trim()
        if (apiKey.isEmpty() || projectId.isEmpty() || appId.isEmpty()) return
        try {
            val builder = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .setApplicationId(appId)
            val bucket = getString(R.string.firebase_storage_bucket).trim()
            if (bucket.isNotEmpty()) builder.setStorageBucket(bucket)
            FirebaseApp.initializeApp(this, builder.build())
        } catch (_: Exception) {
            // fall back to local mode
        }
    }
}

package com.pisheblok.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pisheblok.app.MainActivity
import com.pisheblok.app.PisheBlokApp
import com.pisheblok.app.R
import com.pisheblok.app.widget.computeStats
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/** Daily check: expired / expiring soon / running low / to-buy counts. */
class RemindersWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PisheBlokApp ?: return Result.success()
        val repo = app.container.repository
        val items = repo.observeAllItems().first()
        val shopping = repo.observeShopping().first()
        val stats = computeStats(items, shopping, ZoneId.systemDefault(), LocalDate.now())
        if (stats.expiredSoon > 0 || stats.low > 0 || stats.toBuy > 0) {
            showNotification(stats)
        }
        com.pisheblok.app.widget.refreshWidget(applicationContext)
        return Result.success()
    }

    private fun showNotification(stats: com.pisheblok.app.widget.Stats) {
        val context = applicationContext
        val text = context.getString(
            R.string.notif_text, stats.expired, stats.expiringSoon, stats.low
        ) + (if (stats.toBuy > 0) "\n" + context.getString(R.string.notif_text_buy, stats.toBuy) else "")
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val WORK_NAME = "pisheblok_reminders"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
    }
}

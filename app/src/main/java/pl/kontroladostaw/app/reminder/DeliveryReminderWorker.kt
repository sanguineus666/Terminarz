package pl.kontroladostaw.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import pl.kontroladostaw.app.MainActivity

class DeliveryReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        ensureChannel()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val supplier = inputData.getString(KEY_SUPPLIER).orEmpty()
        val item = inputData.getString(KEY_ITEM).orEmpty()
        val message = inputData.getString(KEY_MESSAGE).orEmpty()
        val orderId = inputData.getString(KEY_ORDER_ID).orEmpty()

        val openApp = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            orderId.hashCode(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Dostawa: $item")
            .setContentText("$supplier • $message")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$supplier • $message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify((orderId + message).hashCode(), notification)
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Terminy dostaw",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Przypomnienia o planowanych terminach dostaw"
            }
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "delivery_reminders"
        const val KEY_ORDER_ID = "order_id"
        const val KEY_SUPPLIER = "supplier"
        const val KEY_ITEM = "item"
        const val KEY_MESSAGE = "message"
    }
}

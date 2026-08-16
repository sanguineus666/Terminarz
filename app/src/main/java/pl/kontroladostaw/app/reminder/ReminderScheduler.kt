package pl.kontroladostaw.app.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import pl.kontroladostaw.app.data.Order
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG_PREFIX = "delivery_order_"

    fun reschedule(context: Context, order: Order) {
        cancel(context, order.id)
        if (order.status.isClosed) return

        scheduleOne(
            context = context,
            order = order,
            date = order.dueDate.minusDays(1),
            message = "Dostawa jest jutro",
            suffix = "day_before",
        )
        scheduleOne(
            context = context,
            order = order,
            date = order.dueDate,
            message = "Dostawa powinna być dzisiaj",
            suffix = "due_day",
        )
    }

    fun cancel(context: Context, orderId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_PREFIX + orderId)
    }

    private fun scheduleOne(
        context: Context,
        order: Order,
        date: LocalDate,
        message: String,
        suffix: String,
    ) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var target = LocalDateTime.of(date, LocalTime.of(9, 0))

        // If the event is for today and 09:00 has already passed, remind shortly after saving.
        if (date == LocalDate.now(zone) && target.isBefore(now)) {
            target = now.plusSeconds(10)
        }
        if (!target.isAfter(now)) return

        val delay = Duration.between(now, target).toMillis().coerceAtLeast(1_000)
        val data = Data.Builder()
            .putString(DeliveryReminderWorker.KEY_ORDER_ID, order.id)
            .putString(DeliveryReminderWorker.KEY_SUPPLIER, order.supplier)
            .putString(DeliveryReminderWorker.KEY_ITEM, order.item)
            .putString(DeliveryReminderWorker.KEY_MESSAGE, message)
            .build()

        val work = OneTimeWorkRequestBuilder<DeliveryReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG_PREFIX + order.id)
            .addTag(TAG_PREFIX + order.id + "_" + suffix)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}

package pl.kontroladostaw.app.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import pl.kontroladostaw.app.data.Order
import java.time.ZoneId

object CalendarUtils {
    fun openInsertEvent(context: Context, order: Order) {
        val zone = ZoneId.systemDefault()
        val start = order.dueDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = order.dueDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val description = buildString {
            append("Dostawca: ${order.supplier}\n")
            append("Zamówienie: ${order.item}\n")
            if (order.orderNumber.isNotBlank()) append("Nr: ${order.orderNumber}\n")
            append("Kwota: ${order.totalAmount}\n")
            append("Zapłacono: ${order.paidAmount}\n")
            if (order.notes.isNotBlank()) append("Notatka: ${order.notes}")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.Events.TITLE, "Dostawa: ${order.item}")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}

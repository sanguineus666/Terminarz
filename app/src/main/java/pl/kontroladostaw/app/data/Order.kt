package pl.kontroladostaw.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class OrderStatus(val label: String) {
    ORDERED("Zamówione"),
    CONFIRMED("Potwierdzone"),
    IN_TRANSIT("W drodze"),
    DELIVERED("Dostarczone"),
    CANCELLED("Anulowane");

    val isClosed: Boolean
        get() = this == DELIVERED || this == CANCELLED
}

data class Order(
    val id: String,
    val supplier: String,
    val item: String,
    val orderNumber: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val orderDateEpochDay: Long,
    val dueDateEpochDay: Long,
    val status: OrderStatus,
    val notes: String,
    val deliveredDateEpochDay: Long? = null,
) {
    val orderDate: LocalDate get() = LocalDate.ofEpochDay(orderDateEpochDay)
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(dueDateEpochDay)
    val deliveredDate: LocalDate? get() = deliveredDateEpochDay?.let(LocalDate::ofEpochDay)
}

object OrderLogic {
    fun isOverdue(order: Order, today: LocalDate = LocalDate.now()): Boolean =
        !order.status.isClosed && order.dueDate.isBefore(today)

    fun isDueSoon(order: Order, today: LocalDate = LocalDate.now()): Boolean {
        if (order.status.isClosed || order.dueDate.isBefore(today)) return false
        val days = ChronoUnit.DAYS.between(today, order.dueDate)
        return days in 0..2
    }

    fun daysLate(order: Order, today: LocalDate = LocalDate.now()): Long =
        if (isOverdue(order, today)) ChronoUnit.DAYS.between(order.dueDate, today) else 0

    fun frozenAmount(order: Order): Double =
        if (order.status.isClosed) 0.0 else order.paidAmount.coerceAtLeast(0.0)
}

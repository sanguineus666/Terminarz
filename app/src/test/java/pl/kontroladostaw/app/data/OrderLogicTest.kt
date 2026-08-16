package pl.kontroladostaw.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OrderLogicTest {
    private val today = LocalDate.of(2026, 8, 16)

    private fun order(
        due: LocalDate,
        status: OrderStatus = OrderStatus.ORDERED,
        paid: Double = 1000.0,
    ) = Order(
        id = "1",
        supplier = "Dostawca",
        item = "Materiał",
        orderNumber = "A-1",
        totalAmount = 1200.0,
        paidAmount = paid,
        orderDateEpochDay = today.minusDays(7).toEpochDay(),
        dueDateEpochDay = due.toEpochDay(),
        status = status,
        notes = "",
    )

    @Test fun overdue_is_detected() {
        assertTrue(OrderLogic.isOverdue(order(today.minusDays(3)), today))
        assertEquals(3, OrderLogic.daysLate(order(today.minusDays(3)), today))
    }

    @Test fun delivered_is_not_overdue() {
        assertFalse(OrderLogic.isOverdue(order(today.minusDays(3), OrderStatus.DELIVERED), today))
    }

    @Test fun due_soon_is_zero_to_two_days() {
        assertTrue(OrderLogic.isDueSoon(order(today), today))
        assertTrue(OrderLogic.isDueSoon(order(today.plusDays(2)), today))
        assertFalse(OrderLogic.isDueSoon(order(today.plusDays(3)), today))
    }

    @Test fun frozen_amount_is_paid_amount_until_closed() {
        assertEquals(1000.0, OrderLogic.frozenAmount(order(today.plusDays(3))), 0.001)
        assertEquals(0.0, OrderLogic.frozenAmount(order(today.plusDays(3), OrderStatus.CANCELLED)), 0.001)
    }
}

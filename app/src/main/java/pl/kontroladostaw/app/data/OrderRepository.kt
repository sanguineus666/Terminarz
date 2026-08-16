package pl.kontroladostaw.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class OrderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("orders_store", Context.MODE_PRIVATE)

    fun load(): List<Order> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Order(
                            id = o.getString("id"),
                            supplier = o.getString("supplier"),
                            item = o.getString("item"),
                            orderNumber = o.optString("orderNumber"),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            orderDateEpochDay = o.getLong("orderDateEpochDay"),
                            dueDateEpochDay = o.getLong("dueDateEpochDay"),
                            status = runCatching { OrderStatus.valueOf(o.getString("status")) }
                                .getOrDefault(OrderStatus.ORDERED),
                            notes = o.optString("notes"),
                            deliveredDateEpochDay = if (o.isNull("deliveredDateEpochDay")) null
                            else o.getLong("deliveredDateEpochDay"),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(orders: List<Order>) {
        val array = JSONArray()
        orders.forEach { order ->
            array.put(JSONObject().apply {
                put("id", order.id)
                put("supplier", order.supplier)
                put("item", order.item)
                put("orderNumber", order.orderNumber)
                put("totalAmount", order.totalAmount)
                put("paidAmount", order.paidAmount)
                put("orderDateEpochDay", order.orderDateEpochDay)
                put("dueDateEpochDay", order.dueDateEpochDay)
                put("status", order.status.name)
                put("notes", order.notes)
                if (order.deliveredDateEpochDay == null) put("deliveredDateEpochDay", JSONObject.NULL)
                else put("deliveredDateEpochDay", order.deliveredDateEpochDay)
            })
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "orders_json_v1"
    }
}

package pl.kontroladostaw.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.kontroladostaw.app.data.Order
import pl.kontroladostaw.app.data.OrderLogic
import pl.kontroladostaw.app.data.OrderRepository
import pl.kontroladostaw.app.data.OrderStatus
import pl.kontroladostaw.app.reminder.ReminderScheduler
import pl.kontroladostaw.app.util.CalendarUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DeliveryApp()
                }
            }
        }
    }
}

@Composable
private fun DeliveryApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { OrderRepository(context) }
    val orders = remember { mutableStateListOf<Order>().apply { addAll(repository.load()) } }
    var editing by remember { mutableStateOf<Order?>(null) }
    var adding by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        orders.forEach { ReminderScheduler.reschedule(context, it) }
    }

    fun persist() = repository.save(orders.toList())

    val today = LocalDate.now()
    val openOrders = orders.filterNot { it.status.isClosed }
    val overdue = openOrders.count { OrderLogic.isOverdue(it, today) }
    val dueSoon = openOrders.count { OrderLogic.isDueSoon(it, today) }
    val frozen = openOrders.sumOf { OrderLogic.frozenAmount(it) }

    val sorted = orders.sortedWith(
        compareBy<Order> { it.status.isClosed }
            .thenBy { it.dueDateEpochDay }
    )

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zamówienie")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Kontrola Dostaw", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Dzisiaj widzisz tylko to, co wymaga uwagi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                DashboardCards(overdue = overdue, dueSoon = dueSoon, frozen = frozen)
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Zamówienia", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (sorted.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Brak zamówień", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("Naciśnij + i wpisz pierwsze zamówienie. Terminów nie musisz już trzymać w głowie.")
                        }
                    }
                }
            }

            items(sorted, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    onCalendar = { CalendarUtils.openInsertEvent(context, order) },
                    onDelivered = {
                        val index = orders.indexOfFirst { it.id == order.id }
                        if (index >= 0) {
                            orders[index] = order.copy(
                                status = OrderStatus.DELIVERED,
                                deliveredDateEpochDay = LocalDate.now().toEpochDay(),
                            )
                            ReminderScheduler.cancel(context, order.id)
                            persist()
                        }
                    },
                    onEdit = { editing = order },
                    onDelete = {
                        orders.removeAll { it.id == order.id }
                        ReminderScheduler.cancel(context, order.id)
                        persist()
                    },
                )
            }
            item { Spacer(Modifier.height(92.dp)) }
        }
    }

    if (adding) {
        OrderEditorDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { newOrder ->
                orders.add(newOrder)
                persist()
                ReminderScheduler.reschedule(context, newOrder)
                adding = false
            },
        )
    }

    editing?.let { original ->
        OrderEditorDialog(
            initial = original,
            onDismiss = { editing = null },
            onSave = { updated ->
                val index = orders.indexOfFirst { it.id == original.id }
                if (index >= 0) orders[index] = updated
                persist()
                ReminderScheduler.reschedule(context, updated)
                editing = null
            },
        )
    }
}

@Composable
private fun DashboardCards(overdue: Int, dueSoon: Int, frozen: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            label = "PO TERMINIE",
            value = overdue.toString(),
            supporting = if (overdue == 0) "Brak zaległych dostaw" else "Wymagają kontaktu dzisiaj",
            background = if (overdue > 0) Color(0xFFFFE8E6) else Color.White,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                MetricCard(
                    label = "DO 2 DNI",
                    value = dueSoon.toString(),
                    supporting = "Bliski termin",
                    background = if (dueSoon > 0) Color(0xFFFFF4D6) else Color.White,
                )
            }
            Box(Modifier.weight(1f)) {
                MetricCard(
                    label = "ZAMROŻONE",
                    value = money(frozen),
                    supporting = "Zapłacone, niedostarczone",
                    background = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, supporting: String, background: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(supporting, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onCalendar: () -> Unit,
    onDelivered: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val overdue = OrderLogic.isOverdue(order)
    val dueSoon = OrderLogic.isDueSoon(order)
    val bg = when {
        overdue -> Color(0xFFFFE8E6)
        dueSoon -> Color(0xFFFFF4D6)
        order.status == OrderStatus.DELIVERED -> Color(0xFFEAF7EE)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(order.item, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(order.supplier, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (overdue) {
                    Text(
                        "${OrderLogic.daysLate(order)} dni po terminie",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB42318),
                    )
                } else {
                    Text(order.status.label, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Termin: ${formatDate(order.dueDate)}")
            Text("Zapłacono: ${money(order.paidAmount)} / ${money(order.totalAmount)}")
            if (order.orderNumber.isNotBlank()) Text("Nr: ${order.orderNumber}")
            if (order.notes.isNotBlank()) Text("Notatka: ${order.notes}")

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                    IconButton(onClick = onCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Dodaj do kalendarza")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń")
                    }
                }
                if (!order.status.isClosed) {
                    FilledTonalButton(onClick = onDelivered) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(" Dostarczone")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderEditorDialog(
    initial: Order?,
    onDismiss: () -> Unit,
    onSave: (Order) -> Unit,
) {
    var supplier by remember(initial?.id) { mutableStateOf(initial?.supplier.orEmpty()) }
    var item by remember(initial?.id) { mutableStateOf(initial?.item.orEmpty()) }
    var orderNumber by remember(initial?.id) { mutableStateOf(initial?.orderNumber.orEmpty()) }
    var total by remember(initial?.id) { mutableStateOf(initial?.totalAmount?.toEditableAmount().orEmpty()) }
    var paid by remember(initial?.id) { mutableStateOf(initial?.paidAmount?.toEditableAmount().orEmpty()) }
    var orderDate by remember(initial?.id) { mutableStateOf(initial?.orderDate ?: LocalDate.now()) }
    var dueDate by remember(initial?.id) { mutableStateOf(initial?.dueDate ?: LocalDate.now().plusDays(7)) }
    var status by remember(initial?.id) { mutableStateOf(initial?.status ?: OrderStatus.ORDERED) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var showOrderDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nowe zamówienie" else "Edytuj zamówienie") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Dostawca *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = item,
                        onValueChange = { item = it },
                        label = { Text("Co zamówiono *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        label = { Text("Nr zamówienia / faktury") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = total,
                        onValueChange = { total = it },
                        label = { Text("Kwota zamówienia *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = paid,
                        onValueChange = { paid = it },
                        label = { Text("Ile już zapłacono") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showOrderDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text("Zamówiono\n${formatDate(orderDate)}")
                        }
                        OutlinedButton(onClick = { showDueDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text("Dostawa\n${formatDate(dueDate)}")
                        }
                    }
                }
                item {
                    Box {
                        OutlinedButton(onClick = { statusMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Status: ${status.label}")
                        }
                        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                            OrderStatus.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        status = option
                                        statusMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notatka") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val totalValue = parseAmount(total)
                val paidValue = parseAmount(paid).coerceAtLeast(0.0)
                when {
                    supplier.isBlank() -> error = "Wpisz dostawcę."
                    item.isBlank() -> error = "Wpisz, co zostało zamówione."
                    totalValue <= 0.0 -> error = "Wpisz poprawną kwotę zamówienia."
                    paidValue > totalValue -> error = "Kwota zapłacona nie może być większa niż kwota zamówienia."
                    dueDate.isBefore(orderDate) -> error = "Termin dostawy nie może być wcześniejszy niż data zamówienia."
                    else -> {
                        onSave(
                            Order(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                supplier = supplier.trim(),
                                item = item.trim(),
                                orderNumber = orderNumber.trim(),
                                totalAmount = totalValue,
                                paidAmount = paidValue,
                                orderDateEpochDay = orderDate.toEpochDay(),
                                dueDateEpochDay = dueDate.toEpochDay(),
                                status = status,
                                notes = notes.trim(),
                                deliveredDateEpochDay = when {
                                    status == OrderStatus.DELIVERED -> initial?.deliveredDateEpochDay ?: LocalDate.now().toEpochDay()
                                    else -> null
                                },
                            )
                        )
                    }
                }
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )

    if (showOrderDatePicker) {
        SimpleDatePickerDialog(
            initial = orderDate,
            onDismiss = { showOrderDatePicker = false },
            onSelected = {
                orderDate = it
                if (dueDate.isBefore(it)) dueDate = it
                showOrderDatePicker = false
            }
        )
    }
    if (showDueDatePicker) {
        SimpleDatePickerDialog(
            initial = dueDate,
            onDismiss = { showDueDatePicker = false },
            onSelected = {
                dueDate = it
                showDueDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit,
) {
    val initialMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: initialMillis
                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onSelected(date)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    ) {
        DatePicker(state = state)
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private fun formatDate(date: LocalDate): String = date.format(dateFormatter)
private fun parseAmount(value: String): Double = value.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
private fun Double.toEditableAmount(): String = if (this == 0.0) "" else String.format(Locale.US, "%.2f", this)
private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(value)

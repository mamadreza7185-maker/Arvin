package ir.arvin.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val navy = Color(0xFF12243E)
private val gold = Color(0xFFB88B4D)
private val muted = Color(0xFF64748B)
private val bg = Color(0xFFF4F7FB)

@Composable
fun ArvinNativeDashboard(
    name: String,
    username: String,
    role: String,
    onRequest: (String, JSONObject, Boolean, (JSONObject) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    var tab by remember { mutableStateOf("خانه") }
    var busy by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultOk by remember { mutableStateOf(true) }
    var adminData by remember { mutableStateOf<JSONObject?>(null) }
    // Session-only history: updated only after server confirms successful save.
    var lastAttendance by remember { mutableStateOf<String?>(null) }
    var lastRoute by remember { mutableStateOf<String?>(null) }
    val pages: List<Pair<String, ImageVector>> = when (role) {
        "admin" -> listOf("خانه" to Icons.Filled.Home, "حضور" to Icons.Filled.AccessTime,
            "مدیریت" to Icons.Filled.Dashboard, "خروج ساعتی" to Icons.Filled.EventNote)
        "visitor" -> listOf("خانه" to Icons.Filled.Home, "حضور" to Icons.Filled.AccessTime,
            "مسیر" to Icons.Filled.LocationOn, "گزارش" to Icons.Filled.Assessment,
            "خروج ساعتی" to Icons.Filled.EventNote)
        else -> listOf("خانه" to Icons.Filled.Home, "حضور" to Icons.Filled.AccessTime,
            "خروج ساعتی" to Icons.Filled.EventNote)
    }
    val send: (String, JSONObject, Boolean) -> Unit = { action, data, gps ->
        if (!busy) {
            busy = true
            resultOk = true
            resultMessage = if (gps) "در حال دریافت GPS و ثبت اطلاعات..." else "در حال ارسال..."
            onRequest(action, data, gps) { result ->
                busy = false
                resultOk = result.optBoolean("ok")
                if (action == "adminLoad" && resultOk) {
                    adminData = result
                    resultMessage = "اطلاعات واقعی دریافت شد."
                } else {
                    if (resultOk && action == "attendance") lastAttendance = data.optString("action")
                    if (resultOk && action == "visitorRoute") lastRoute = data.optString("action")
                    resultMessage = result.optString("message",
                        if (resultOk) "عملیات با موفقیت ثبت شد." else "درخواست انجام نشد.")
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFFCF9F4),
        topBar = { ModernArvinHeader(name, role, onLogout) },
        bottomBar = {
            ModernArvinBottomBar(pages, tab) { selected ->
                tab = selected
                resultMessage = null
            }
        }
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = gold)
            if (resultMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    color = if (resultOk) Color(0xFFE9F7EE) else Color(0xFFFFE8E8),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(resultMessage.orEmpty(), modifier = Modifier.padding(12.dp),
                        color = if (resultOk) Color(0xFF186C42) else Color(0xFFAC2D2D),
                        fontSize = 13.sp)
                }
            }
            when (tab) {
                "خانه" -> ModernHome(
                    name, role, username,
                    onNavigate = { tab = it },
                    onRefresh = { send("adminLoad", JSONObject(), false) },
                    adminData = adminData
                )
                "حضور" -> ModernAttendance(busy, role, send, lastAttendance, lastRoute)
                "مسیر" -> ModernRoute(busy, send, lastRoute)
                "گزارش" -> VisitorReportPanel(busy, send)
                "خروج ساعتی" -> LeavePanel(busy, send)
                "مدیریت" -> AdminPanel(adminData, busy, send)
            }
        }
    }
}

@Composable
private fun PanelTitle(title: String, hint: String) {
    ModernSectionHeader(title, hint,
        if (title.contains("گزارش")) Icons.Filled.Assignment
        else Icons.Filled.EventNote)
}

@Composable
private fun HomePanel(role: String, username: String, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = navy)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("پنل کارکنان پخش آروین", color = Color.White,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("حضور و غیاب، مسیر ویزیتور و گزارش روزانه",
                    color = Color(0xFFDAE6F3))
                Text("حساب: $username", color = Color(0xFFF3DFB9), fontSize = 12.sp)
            }
        }
        ActionCard("حضور و غیاب", "ثبت چهار زمان کاری", Icons.Filled.AccessTime) {
            onNavigate("حضور")
        }
        if (role == "visitor") {
            ActionCard("مسیر ویزیتور", "ثبت شروع و پایان مسیر با GPS", Icons.Filled.LocationOn) {
                onNavigate("مسیر")
            }
            ActionCard("گزارش روزانه", "ثبت فاکتور، فروش و وصول", Icons.Filled.Assessment) {
                onNavigate("گزارش")
            }
        }
        ActionCard("خروج ساعتی", "درخواست خروج جهت تأیید مدیر", Icons.Filled.EventNote) {
            onNavigate("خروج ساعتی")
        }
        if (role == "admin") {
            ActionCard("پنل مدیریت", "مشاهده گزارش‌ها، درخواست‌ها و کاربران",
                Icons.Filled.Dashboard) { onNavigate("مدیریت") }
        }
        Text("اطلاعات فقط پس از پاسخ موفق سرور در Google Sheets ذخیره می‌شود.",
            color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun ActionCard(title: String, caption: String, icon: ImageVector, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = navy, fontWeight = FontWeight.Bold)
                Text(caption, color = muted, fontSize = 12.sp)
            }
            TextButton(onClick = onClick) { Text("باز کردن") }
        }
    }
}

@Composable
private fun AttendancePanel(busy: Boolean, send: (String, JSONObject, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelTitle("حضور و غیاب", "زمان واقعی و GPS هنگام فشار دادن ثبت ذخیره می‌شود.")
        listOf("ورود صبح" to "07:00", "خروج ظهر" to "12:30",
            "ورود عصر" to "16:00", "پایان کار" to "19:00").forEach { (title, hour) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = gold)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = navy, fontWeight = FontWeight.Bold)
                        Text("شیفت $hour", color = muted, fontSize = 12.sp)
                    }
                    Button(
                        enabled = !busy,
                        onClick = { send("attendance", JSONObject().put("action", title), true) }
                    ) { Text("ثبت") }
                }
            }
        }
    }
}

@Composable
private fun RoutePanel(busy: Boolean, send: (String, JSONObject, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PanelTitle("مسیر ویزیتور", "موقعیت مکانی فقط هنگام ثبت مسیر گرفته می‌شود.")
        Button(onClick = { send("visitorRoute", JSONObject().put("action", "شروع مسیر"), true) },
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("شروع مسیر")
        }
        OutlinedButton(onClick = { send("visitorRoute", JSONObject().put("action", "پایان مسیر"), true) },
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("پایان مسیر")
        }
        Text("این نسخه رهگیری دائمی در پس‌زمینه ندارد.",
            color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit,
                  number: Boolean = false) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(), value = value, onValueChange = onValue,
        label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = gold,
            unfocusedBorderColor = Color(0xFFADB7C2),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        keyboardOptions = KeyboardOptions(keyboardType =
            if (number) KeyboardType.Decimal else KeyboardType.Text)
    )
}

@Composable
private fun VisitorReportPanel(busy: Boolean, send: (String, JSONObject, Boolean) -> Unit) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var customer by remember { mutableStateOf("") }
    var visits by remember { mutableStateOf("") }
    var invoices by remember { mutableStateOf("") }
    var sales by remember { mutableStateOf("") }
    var collections by remember { mutableStateOf("") }
    var orders by remember { mutableStateOf("") }
    var noSale by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelTitle("گزارش روزانه", "مبالغ را به تومان وارد کنید.")
        Field("تاریخ YYYY-MM-DD", date, { date = it })
        Field("مشتری یا محدوده ویزیت *", customer, { customer = it })
        Field("تعداد ویزیت", visits, { visits = it }, true)
        Field("تعداد فاکتور موفق", invoices, { invoices = it }, true)
        Field("مبلغ فروش (تومان)", sales, { sales = it }, true)
        Field("مبلغ وصول (تومان)", collections, { collections = it }, true)
        Field("تعداد سفارش", orders, { orders = it }, true)
        Field("دلیل عدم فروش", noSale, { noSale = it })
        Field("توضیحات", notes, { notes = it })
        Button(
            enabled = !busy && customer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                send("visitorReport", JSONObject()
                    .put("workDate", date).put("customer", customer)
                    .put("visitsCount", visits.toIntOrNull() ?: 0)
                    .put("invoicesCount", invoices.toIntOrNull() ?: 0)
                    .put("invoiceAmount", sales.toDoubleOrNull() ?: 0.0)
                    .put("collectionAmount", collections.toDoubleOrNull() ?: 0.0)
                    .put("ordersCount", orders.toIntOrNull() ?: 0)
                    .put("noSaleReason", noSale)
                    .put("notes", notes), true)
            }
        ) { Text("ارسال گزارش با GPS") }
    }
}

@Composable
private fun LeavePanel(busy: Boolean, send: (String, JSONObject, Boolean) -> Unit) {
    var department by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelTitle("درخواست خروج ساعتی", "تأیید یا رد درخواست با مدیر است.")
        Field("بخش (انبار / دفتر / ویزیت)", department, { department = it })
        Field("ساعت خروج (مثل 10:00)", start, { start = it })
        Field("ساعت برگشت (مثل 11:00)", end, { end = it })
        Field("علت درخواست", reason, { reason = it })
        Button(
            enabled = !busy && listOf(department, start, end, reason).all { it.isNotBlank() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = { send("leaveRequest", JSONObject()
                .put("department", department).put("start", start.trim())
                .put("end", end.trim()).put("reason", reason), false) }
        ) { Text("ارسال درخواست") }
    }
}

@Composable
private fun AdminPanel(
    info: JSONObject?,
    busy: Boolean,
    send: (String, JSONObject, Boolean) -> Unit
) {
    var view by remember { mutableStateOf("leaveRequests") }
    Column(Modifier.fillMaxSize().background(Color(0xFFFCF9F4))
        .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernManagerSummary(info)
        Button(onClick = { send("adminLoad", JSONObject(), false) },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("دریافت و به‌روزرسانی اطلاعات")
        }
        if (info == null) {
            Text("برای مشاهده گزارش‌ها و درخواست‌ها، اطلاعات را دریافت کنید.",
                color = muted)
        } else {
            Text("بخش موردنظر را انتخاب کنید", color = navy,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            listOf(
                listOf("leaveRequests" to "درخواست‌ها", "attendance" to "حضور", "routes" to "مسیر"),
                listOf("reports" to "فروش", "users" to "کاربران")
            ).forEach { chips ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { (key, label) ->
                        FilterChip(selected = view == key, onClick = { view = key },
                            shape = RoundedCornerShape(15.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, if (view == key) Color(0xFFE8B64B) else Color(0xFFDED5C6)),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFE6A6),
                                containerColor = Color.White,
                                selectedLabelColor = navy
                            ),
                            label = { Text(label) })
                    }
                }
            }
            val records: JSONArray = info.optJSONArray(view) ?: JSONArray()
            Text("تعداد: " + records.length(), color = muted, fontSize = 12.sp)
            for (i in 0 until minOf(records.length(), 40)) {
                val item = records.optJSONObject(i) ?: continue
                AdminRow(view, item, busy, send)
            }
            if (records.length() > 40) Text("فقط ۴۰ مورد آخر این بخش نشان داده می‌شود.",
                fontSize = 12.sp, color = muted)
        }
        if (view == "users" && info != null) {
            HorizontalDivider()
            UserForm(busy, send, info.optJSONArray("users"))
        }
        Text("برای دیدن نتیجه تغییرات، دکمه به‌روزرسانی را دوباره بزنید.",
            color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun CountCard(label: String, value: Int, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), color = navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(label, color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdminRow(type: String, item: JSONObject, busy: Boolean,
                     send: (String, JSONObject, Boolean) -> Unit) {
    fun f(key: String) = item.optString(key, "")
    val heading: String
    val description: String
    val details: String
    when (type) {
        "attendance", "routes" -> {
            heading = f("Name") + " • " + f("Action")
            description = f("Timestamp")
            details = f("Username")
        }
        "reports" -> {
            heading = f("Name") + " • " + f("Customer")
            description = "فاکتور: " + f("InvoicesCount") + " | فروش: " + f("InvoiceAmount")
            details = "وصول: " + f("CollectionAmount") + " | " + f("Timestamp")
        }
        "leaveRequests" -> {
            heading = f("Name") + " • " + f("Status")
            description = f("Department") + " | " + f("LeaveStart") + " تا " + f("LeaveEnd")
            details = f("Reason")
        }
        else -> {
            heading = f("name") + " • " + f("username")
            description = f("role")
            details = if (item.optBoolean("active")) "فعال" else "غیرفعال"
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0D9AC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(heading, color = navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, color = muted, fontSize = 12.sp)
            Text(details, color = muted, fontSize = 12.sp)
            if (type == "leaveRequests" && f("Status") == "در انتظار بررسی") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        send("adminUpdateLeave", JSONObject().put("id", f("ID"))
                            .put("status", "تأیید شد").put("note", ""), false)
                    }, enabled = !busy) { Text("تأیید") }
                    OutlinedButton(onClick = {
                        send("adminUpdateLeave", JSONObject().put("id", f("ID"))
                            .put("status", "رد شد").put("note", ""), false)
                    }, enabled = !busy) { Text("رد") }
                }
            }
        }
    }
}

@Composable
private fun UserForm(
    busy: Boolean,
    send: (String, JSONObject, Boolean) -> Unit,
    users: JSONArray?
) {
    var originalUsername by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    Text("مدیریت حساب ویزیتورها", color = navy, fontWeight = FontWeight.Bold)
    Text(
        "برای تغییر نام کاربری، ابتدا حساب ویزیتور را از فهرست زیر انتخاب کنید.",
        color = muted, fontSize = 12.sp
    )
    val visitorAccounts = remember(users) {
        (0 until (users?.length() ?: 0))
            .mapNotNull { users?.optJSONObject(it) }
            .filter { it.optString("role") == "visitor" }
    }
    if (visitorAccounts.isEmpty()) {
        Text("ابتدا اطلاعات کاربران را به‌روزرسانی کنید.",
            color = muted, fontSize = 12.sp)
    }
    visitorAccounts.chunked(2).forEach { entries ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            entries.forEach { u ->
                val login = u.optString("username")
                val label = u.optString("name", login)
                FilterChip(
                    selected = originalUsername == login,
                    onClick = {
                        originalUsername = login
                        username = login
                        fullName = label
                        active = u.optBoolean("active", true)
                        password = ""
                    },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
    }
    OutlinedButton(
        onClick = {
            originalUsername = ""
            username = ""
            fullName = ""
            password = ""
            active = true
        },
        enabled = !busy
    ) { Text("ایجاد ویزیتور جدید") }

    if (originalUsername.isNotBlank()) {
        Text("در حال ویرایش حساب: $originalUsername",
            color = navy, fontWeight = FontWeight.Medium)
    }
    Field("نام کاربری جدید (انگلیسی)", username, { username = it })
    Field("نام و نام خانوادگی", fullName, { fullName = it })
    OutlinedTextField(
        value = password, onValueChange = { password = it },
        label = { Text(if (originalUsername.isNotBlank())
            "رمز جدید (خالی = حفظ رمز قبلی)" else "رمز عبور (حداقل ۸ کاراکتر)") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = active, onCheckedChange = { active = it })
        Text("حساب فعال باشد", color = navy)
    }
    val validUsername = Regex("^[a-zA-Z0-9_.-]{2,40}$").matches(username.trim())
    val validPassword = if (originalUsername.isNotBlank())
        password.isEmpty() || password.length >= 8 else password.length >= 8
    Button(
        onClick = {
            val payload = JSONObject()
                .put("username", username.trim())
                .put("name", fullName.trim())
                .put("password", password)
                .put("active", active)
            if (originalUsername.isNotBlank()) {
                payload.put("oldUsername", originalUsername)
                // The old API never recognizes this action, preventing accidental
                // creation of duplicate users before the server is upgraded.
                send("adminRenameVisitor", payload, false)
            } else {
                send("adminSaveVisitor", payload, false)
            }
        },
        enabled = !busy && validUsername && fullName.isNotBlank() && validPassword,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (originalUsername.isNotBlank()) "ذخیره تغییرات ویزیتور"
        else "ساخت حساب جدید")
    }
    Text(
        "پس از تغییر نام کاربری، ویزیتور باید خارج شود و با نام کاربری جدید دوباره وارد شود. " +
        "رمز قبلی حفظ می‌شود مگر اینکه رمز جدید ثبت کنید.",
        color = muted, fontSize = 12.sp
    )
}

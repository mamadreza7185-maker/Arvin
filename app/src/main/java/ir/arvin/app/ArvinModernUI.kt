package ir.arvin.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import android.content.Context
import org.json.JSONArray
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private val modernNavy = Color(0xFF0C1931)
private val modernBlue = Color(0xFF183355)
private val modernGold = Color(0xFFE8B64B)
private val modernLightGold = Color(0xFFFFE7A6)
private val modernCream = Color(0xFFFCF9F4)
private val modernGray = Color(0xFF64748B)

@Composable
fun ModernArvinHeader(name: String, role: String, onLogout: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(143.dp)
            .background(Brush.horizontalGradient(listOf(modernNavy, modernBlue, modernNavy)))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val wave = Path().apply {
                moveTo(0f, h * 0.89f)
                cubicTo(w * 0.30f, h * 0.99f, w * 0.72f, h * 0.98f, w, h * 0.89f)
            }
            drawPath(wave, Brush.horizontalGradient(listOf(
                Color(0xFF9D671F), modernLightGold, Color(0xFFD49A30)
            )), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            val upper = Path().apply {
                moveTo(0f, h * 0.27f)
                cubicTo(w * 0.30f, h * 0.02f, w * 0.62f, h * 0.08f, w, 0f)
            }
            drawPath(upper, Color(0xFFE2AD45), style = Stroke(2.dp.toPx()))
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arvin_logo),
                contentDescription = "نشان پخش آروین",
                modifier = Modifier.size(66.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Column(Modifier.weight(1f)) {
                Text("پخش آروین", color = Color.White, fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("ARVIN DISTRIBUTION", color = modernLightGold,
                    fontSize = 10.sp, letterSpacing = 1.sp)
                Text("پخش مواد غذایی، بهداشتی و شوینده",
                    color = Color(0xFFE5EAF1), fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onLogout, modifier = Modifier
                .background(Color(0xFF273B58), RoundedCornerShape(13.dp))) {
                Icon(Icons.Filled.Logout, contentDescription = "خروج از حساب",
                    tint = modernLightGold)
            }
        }
        Text(
            text = "خوش آمدید، " + name,
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = 22.dp, end = 22.dp, bottom = 21.dp),
            color = Color.White, fontSize = 12.sp, maxLines = 1
        )
    }
}

@Composable
fun ModernArvinBottomBar(
    pages: List<Pair<String, ImageVector>>,
    current: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = modernNavy,
        tonalElevation = 6.dp,
    ) {
        pages.forEach { (title, icon) ->
            NavigationBarItem(
                selected = current == title,
                onClick = { onNavigate(title) },
                icon = {
                    Icon(icon, contentDescription = title,
                        modifier = Modifier.size(23.dp))
                },
                label = { Text(title, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = modernNavy,
                    selectedTextColor = modernLightGold,
                    indicatorColor = modernGold,
                    unselectedIconColor = Color(0xFFF3F5FA),
                    unselectedTextColor = Color(0xFFE1E7F1)
                )
            )
        }
    }
}

@Composable
fun ModernSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(46.dp)
                .background(Brush.linearGradient(listOf(modernBlue, modernNavy)),
                    RoundedCornerShape(14.dp))
                .padding(11.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = modernGold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = modernNavy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = modernGray, fontSize = 11.sp)
        }
    }
}

@Composable
fun ModernPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF3DCB1))
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun ModernHome(
    name: String,
    role: String,
    username: String,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit,
    adminData: JSONObject?
) {
    Column(
        Modifier.fillMaxSize().background(modernCream)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModernPanel {
            Text("سلام، " + name, color = modernNavy,
                fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (role == "admin") "به پنل مدیریت پخش آروین خوش آمدید"
                else "به سامانه مدیریت کارکنان پخش آروین خوش آمدید",
                color = modernGray, fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null,
                    tint = Color(0xFF39805A), modifier = Modifier.size(17.dp))
                Text(
                    (if (role == "admin") "مدیر" else
                        if (role == "visitor") "ویزیتور" else "کارمند") + " • " + username,
                    color = modernBlue, fontSize = 12.sp
                )
            }
        }
        ModernSectionHeader(
            "دسترسی سریع", "امکانات اختصاصی حساب شما",
            Icons.Filled.Apps
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModernActionTile("حضور و غیاب", "ثبت ورود و خروج",
                Icons.Filled.EventAvailable, Modifier.weight(1f)) { onNavigate("حضور") }
            if (role == "admin") {
                ModernActionTile("پنل مدیر", "گزارش‌های کارکنان",
                    Icons.Filled.Dashboard, Modifier.weight(1f)) { onNavigate("مدیریت") }
            } else if (role == "visitor") {
                ModernActionTile("مسیر ویزیتور", "شروع و پایان مسیر",
                    Icons.Filled.Route, Modifier.weight(1f)) { onNavigate("مسیر") }
            } else {
                ModernActionTile("خروج ساعتی", "درخواست مرخصی",
                    Icons.Filled.ScheduleSend, Modifier.weight(1f)) { onNavigate("خروج ساعتی") }
            }
        }
        if (role == "visitor") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernActionTile("گزارش روزانه", "فروش و وصول",
                    Icons.Filled.Assignment, Modifier.weight(1f)) { onNavigate("گزارش") }
                ModernActionTile("خروج ساعتی", "درخواست مرخصی",
                    Icons.Filled.ScheduleSend, Modifier.weight(1f)) { onNavigate("خروج ساعتی") }
            }
        } else if (role == "admin") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernActionTile("درخواست‌ها", "تأیید یا رد",
                    Icons.Filled.MarkEmailUnread, Modifier.weight(1f)) { onNavigate("مدیریت") }
                ModernActionTile("خروج ساعتی", "درخواست شخصی",
                    Icons.Filled.ScheduleSend, Modifier.weight(1f)) { onNavigate("خروج ساعتی") }
            }
        }
        if (role == "admin") {
            ModernSectionHeader("نمای کلی سامانه",
                "آمار واقعی، پس از دریافت از سرور", Icons.Filled.Insights)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ModernMetricTile("ثبت حضور",
                    adminData?.optJSONArray("attendance")?.length()?.toString() ?: "—",
                    Icons.Filled.EventAvailable, Modifier.weight(1f))
                ModernMetricTile("گزارش‌ها",
                    adminData?.optJSONArray("reports")?.length()?.toString() ?: "—",
                    Icons.Filled.Assessment, Modifier.weight(1f))
                ModernMetricTile("درخواست‌ها",
                    adminData?.optJSONArray("leaveRequests")?.length()?.toString() ?: "—",
                    Icons.Filled.AssignmentTurnedIn, Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, modernGold)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("به‌روزرسانی آمار")
            }
        } else {
            ModernPanel {
                ModernSectionHeader("ساعت کار شرکت",
                    "شیفت‌های رسمی پخش آروین", Icons.Filled.Schedule)
                Text("صبح: ۰۷:۰۰ تا ۱۲:۳۰", color = modernBlue)
                Text("عصر: ۱۶:۰۰ تا ۱۹:۰۰", color = modernBlue)
                Text("ساعت و مکان واقعی هنگام ثبت در سرور ذخیره می‌شود.",
                    color = modernGray, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ModernActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(141.dp).shadow(5.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFF0D9A6))
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(43.dp).background(
                Brush.linearGradient(listOf(modernBlue, modernNavy)),
                RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = modernGold,
                    modifier = Modifier.size(25.dp))
            }
            Text(title, color = modernNavy, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 18.sp)
            Text(subtitle, color = modernGray, fontSize = 10.sp,
                maxLines = 2, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun ModernMetricTile(
    title: String,
    count: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Surface(
        modifier = modifier, shape = RoundedCornerShape(19.dp),
        color = Color.White, border = BorderStroke(1.dp, Color(0xFFF0D9AC)),
        tonalElevation = 2.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFB78329),
                modifier = Modifier.size(25.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = modernNavy, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(count, color = modernBlue, fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ModernAttendance(
    busy: Boolean,
    role: String,
    onSend: (String, JSONObject, Boolean) -> Unit,
    lastAttendance: String?,
    lastRoute: String?
) {
    Column(
        Modifier.fillMaxSize().background(modernCream)
            .verticalScroll(rememberScrollState()).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        ModernPanel {
            ModernSectionHeader("حضور و غیاب",
                "ثبت ساعت واقعی و موقعیت مکانی", Icons.Filled.EventAvailable)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth()) {
                ModernAttendanceButton("ورود صبح", "شروع ۰۷:۰۰",
                    Icons.Filled.Login, Modifier.weight(1f), busy) {
                    onSend("attendance", JSONObject().put("action", "ورود صبح"), true)
                }
                ModernAttendanceButton("خروج ظهر", "پایان ۱۲:۳۰",
                    Icons.Filled.Logout, Modifier.weight(1f), busy) {
                    onSend("attendance", JSONObject().put("action", "خروج ظهر"), true)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth()) {
                ModernAttendanceButton("ورود عصر", "شروع ۱۶:۰۰",
                    Icons.Filled.Login, Modifier.weight(1f), busy) {
                    onSend("attendance", JSONObject().put("action", "ورود عصر"), true)
                }
                ModernAttendanceButton("پایان کار", "پایان ۱۹:۰۰",
                    Icons.Filled.Flag, Modifier.weight(1f), busy) {
                    onSend("attendance", JSONObject().put("action", "پایان کار"), true)
                }
            }
            HorizontalDivider(color = Color(0xFFEDE4D9))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, null, tint = modernGold,
                    modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Text("آخرین ثبت این نشست: " + (lastAttendance ?: "هنوز ثبت نشده"),
                    color = modernGray, fontSize = 12.sp)
            }
        }
        if (role == "visitor") {
            ModernRouteSection(busy, onSend, lastRoute)
        }
        ModernPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = Color(0xFF278556))
                Spacer(Modifier.width(7.dp))
                Text("برای ثبت دقیق، اینترنت و GPS را روشن نگه دارید.",
                    color = modernBlue, fontSize = 11.sp)
            }
            Text("روشن‌بودن GPS در این صفحه بررسی نشده؛ هنگام ثبت، برنامه موقعیت را دریافت می‌کند.",
                color = modernGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ModernAttendanceButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    busy: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(110.dp).clickable(enabled = !busy, onClick = onClick),
        color = Color.White, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5C486)),
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = Color(0xFFB88431), modifier = Modifier.size(24.dp))
            Text(title, color = modernNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = modernGray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ModernRouteSection(
    busy: Boolean,
    send: (String, JSONObject, Boolean) -> Unit,
    lastRoute: String?
) {
    val ctx = LocalContext.current
    var statusTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            statusTick++
        }
    }
    val gpsPrefs = remember(statusTick) {
        ctx.getSharedPreferences("arvin_tracking_queue_v16", Context.MODE_PRIVATE)
    }
    val trackingActive = gpsPrefs.getBoolean("active", false)
    val lastFix = gpsPrefs.getLong("lastGps", 0L)
    val lastUploaded = gpsPrefs.getLong("lastUpload", 0L)
    val trackingError = gpsPrefs.getString("lastError", "").orEmpty()
    val pendingCount = try {
        JSONArray(gpsPrefs.getString("queue", "[]") ?: "[]").length()
    } catch (_: Exception) { 0 }
    val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
    ModernPanel {
        ModernSectionHeader("مسیر ویزیتور",
            "ثبت مسیر زنده با GPS و اعلان فعال", Icons.Filled.Route)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (trackingActive) Color(0xFFE8F6EC) else Color(0xFFF2F3F6),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (trackingActive) "● GPS مسیر فعال است"
                    else "● GPS مسیر روی این گوشی فعال نیست",
                    color = if (trackingActive) Color(0xFF177642) else modernGray,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (lastFix > 0L) Text(
                    "آخرین موقعیت دریافت‌شده: " + clock.format(Date(lastFix)),
                    color = modernBlue, fontSize = 11.sp)
                if (lastUploaded > 0L) Text(
                    "آخرین ارسال موفق به سرور: " + clock.format(Date(lastUploaded)),
                    color = modernBlue, fontSize = 11.sp)
                if (pendingCount > 0) Text(
                    "$pendingCount نقطه در انتظار ارسال به سرور",
                    color = Color(0xFFA46C13), fontSize = 11.sp)
                if (trackingError.isNotBlank()) Text(
                    trackingError, color = Color(0xFFB42318), fontSize = 11.sp)
                if (!trackingActive && lastRoute != "پایان مسیر") Text(
                    "برای آغاز ردیابی، «شروع مسیر» را بزنید و فعال‌شدن اعلان GPS را بررسی کنید.",
                    color = modernGray, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    send("visitorRoute", JSONObject().put("action", "شروع مسیر"), true)
                },
                enabled = !busy,
                modifier = Modifier.weight(1f).height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = modernGold,
                    contentColor = modernNavy
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("شروع مسیر", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    send("visitorRoute", JSONObject().put("action", "پایان مسیر"), true)
                },
                enabled = !busy,
                modifier = Modifier.weight(1f).height(55.dp),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, modernGold)
            ) {
                Icon(Icons.Filled.Stop, null, tint = Color(0xFFC33D44))
                Spacer(Modifier.width(4.dp))
                Text("پایان مسیر", color = modernNavy, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = Color(0xFF218554))
            Spacer(Modifier.width(7.dp))
            Text("آخرین ثبت مسیر این نشست: " + (lastRoute ?: "ثبت نشده"),
                color = modernGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ModernRoute(
    busy: Boolean,
    send: (String, JSONObject, Boolean) -> Unit,
    lastRoute: String?
) {
    Column(
        Modifier.fillMaxSize().background(modernCream)
            .verticalScroll(rememberScrollState()).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModernRouteSection(busy, send, lastRoute)
        ModernPanel {
            ModernSectionHeader(
                "حریم خصوصی مسیر", "ثبت موقعیت فقط هنگام عملیات",
                Icons.Filled.Security
            )
            Text("GPS فقط پس از زدن «شروع مسیر» و تا زمان «پایان مسیر» یا توقف از اعلان گوشی فعال است. در این مدت با اعلان قابل مشاهده، موقعیت‌های مسیر در پس‌زمینه نیز ثبت می‌شوند.",
                color = modernGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ModernManagerSummary(info: JSONObject?) {
    ModernSectionHeader(
        "نمای کلی مدیریت",
        "آمار واقعی مطابق داده‌های بازیابی‌شده از سرور",
        Icons.Filled.Leaderboard
    )
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ModernManagerMetric("حضور", info?.optJSONArray("attendance")?.length()?.toString() ?: "—",
            Icons.Filled.EventAvailable, Modifier.weight(1f), dark = true)
        ModernManagerMetric("گزارش‌ها", info?.optJSONArray("reports")?.length()?.toString() ?: "—",
            Icons.Filled.Assessment, Modifier.weight(1f), dark = false)
    }
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        ModernManagerMetric("درخواست‌ها", info?.optJSONArray("leaveRequests")?.length()?.toString() ?: "—",
            Icons.Filled.AssignmentTurnedIn, Modifier.weight(1f), dark = false)
        ModernManagerMetric("مسیرها", info?.optJSONArray("routes")?.length()?.toString() ?: "—",
            Icons.Filled.Route, Modifier.weight(1f), dark = true)
    }
}

@Composable
private fun ModernManagerMetric(
    label: String,
    count: String,
    icon: ImageVector,
    modifier: Modifier,
    dark: Boolean
) {
    Surface(
        modifier = modifier.height(98.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (dark) modernNavy else Color.White,
        border = BorderStroke(1.dp, modernGold),
        shadowElevation = 3.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (dark) modernLightGold else Color(0xFFB98329),
                modifier = Modifier.size(29.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = if (dark) Color.White else modernNavy,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(count, color = if (dark) modernLightGold else modernBlue,
                    fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

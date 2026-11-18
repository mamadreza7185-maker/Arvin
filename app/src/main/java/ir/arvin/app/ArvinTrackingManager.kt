package ir.arvin.app

import android.app.DatePickerDialog
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val trackingNavy = Color(0xFF12243E)
private val trackingGold = Color(0xFFE4B24F)
private val trackingMuted = Color(0xFF64748B)
private val trackingBg = Color(0xFFFCF9F4)
private val tehran = TimeZone.getTimeZone("Asia/Tehran")

private data class TrackingSessionItem(
    val id: String,
    val username: String,
    val name: String,
    val status: String,
    val start: String,
    val end: String,
    val lastSeen: String,
    val lat: Double,
    val lng: Double
)

private fun showTehranTime(iso: String): String {
    return try {
        val millis = java.time.Instant.parse(iso).toEpochMilli()
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tehran }
            .format(Date(millis))
    } catch (_: Exception) { "—" }
}

@Composable
fun ArvinTrackingManager(
    onRequest: (String, JSONObject, Boolean, (JSONObject) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance(tehran) }
    var day by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = tehran }.format(Date()))
    }
    var sessions by remember { mutableStateOf<List<TrackingSessionItem>>(emptyList()) }
    var selectedId by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(JSONArray()) }
    var loading by remember { mutableStateOf(false) }
    var loadingPath by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var truncated by remember { mutableStateOf(false) }

    // Summary refresh is active only while the manager's tracking tab is on-screen.
    LaunchedEffect(day) {
        sessions = emptyList()
        selectedId = ""
        points = JSONArray()
        while (true) {
            loading = true
            onRequest("trackingSummary", JSONObject().put("date", day), false) { result ->
                loading = false
                if (result.optBoolean("ok")) {
                    val arr = result.optJSONArray("sessions") ?: JSONArray()
                    sessions = (0 until arr.length()).mapNotNull { i ->
                        val s = arr.optJSONObject(i) ?: return@mapNotNull null
                        TrackingSessionItem(
                            id = s.optString("sessionId"),
                            username = s.optString("username"),
                            name = s.optString("name"),
                            status = s.optString("status"),
                            start = s.optString("startAt"),
                            end = s.optString("endAt"),
                            lastSeen = s.optString("lastSeen"),
                            lat = s.optDouble("lat",0.0),
                            lng = s.optDouble("lng",0.0)
                        )
                    }
                    if (selectedId.isNotBlank() && sessions.none { it.id == selectedId }) {
                        selectedId = ""
                        points = JSONArray()
                    }
                    message = null
                } else {
                    message = result.optString("message", "اطلاعات ردیابی دریافت نشد.")
                }
            }
            delay(60000L)
        }
    }

    // The selected route refreshes approximately every 30 seconds.
    LaunchedEffect(selectedId) {
        if (selectedId.isBlank()) return@LaunchedEffect
        while (true) {
            val currentId = selectedId
            loadingPath = true
            onRequest("trackingPath",
                JSONObject().put("sessionId", currentId), false) { result ->
                if (selectedId != currentId) return@onRequest
                loadingPath = false
                if (result.optBoolean("ok")) {
                    points = result.optJSONArray("points") ?: JSONArray()
                    truncated = result.optBoolean("truncated", false)
                    message = null
                } else message = result.optString("message", "نقاط مسیر دریافت نشد.")
            }
            delay(30000L)
        }
    }

    Column(
        Modifier.fillMaxSize().background(trackingBg)
            .verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        ModernSectionHeader(
            "ردیابی زنده ویزیتورها",
            "نمایش مسیر واقعی ثبت‌شده توسط GPS",
            Icons.Filled.Route
        )
        ModernPanel {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("تاریخ کاری", color = trackingMuted, fontSize = 11.sp)
                    Text(day, color = trackingNavy, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    border = BorderStroke(1.dp, trackingGold),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                day = String.format(Locale.US, "%04d-%02d-%02d", y, m+1, d)
                            },
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("انتخاب تاریخ")
                }
            }
            Text("تاریخ بر اساس ساعت ایران است.", color = trackingMuted, fontSize = 11.sp)
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = trackingGold)
        if (!message.isNullOrBlank()) {
            Text(message.orEmpty(), color = Color(0xFFB42318), fontSize = 12.sp)
        }
        if (sessions.isEmpty() && !loading) {
            ModernPanel {
                Text("برای این تاریخ، هنوز مسیر ثبت‌شده‌ای وجود ندارد.",
                    color = trackingMuted, fontSize = 13.sp)
            }
        } else {
            Text("ویزیتورها و مسیرهای روز انتخاب‌شده", color = trackingNavy,
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        sessions.forEach { session ->
            val picked = selectedId == session.id
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                color = if (picked) Color(0xFFFFF0CC) else Color.White,
                border = BorderStroke(1.dp,
                    if (picked) trackingGold else Color(0xFFE9E0D4)),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PersonPinCircle, contentDescription = null,
                        tint = trackingGold, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.name.ifBlank { session.username }, color = trackingNavy,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("شروع: " + showTehranTime(session.start) +
                            "   آخرین موقعیت: " + showTehranTime(session.lastSeen),
                            color = trackingMuted, fontSize = 11.sp)
                        Text(if (session.status == "فعال") "● مسیر فعال" else
                            "● " + session.status,
                            color = if (session.status == "فعال")
                                Color(0xFF168552) else trackingMuted,
                            fontSize = 11.sp)
                    }
                    Button(
                        onClick = { selectedId = session.id; points = JSONArray() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = trackingNavy,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) { Text("نقشه", fontSize = 11.sp) }
                }
            }
        }
        if (selectedId.isNotBlank()) {
            val selected = sessions.firstOrNull { it.id == selectedId }
            ModernPanel {
                Text("مسیر " + (selected?.name ?: "انتخاب‌شده"),
                    color = trackingNavy, fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Route, contentDescription = null,
                        tint = trackingGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(points.length().toString() + " نقطه ثبت‌شده",
                        color = trackingMuted, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    if (loadingPath) CircularProgressIndicator(
                        modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                }
                ArvinLiveRouteMap(points)
                Text("● سبز: شروع   ● قرمز: آخرین موقعیت",
                    color = trackingMuted, fontSize = 11.sp)
                if (truncated) Text(
                    "برای سرعت مناسب، فقط بخشی از آخرین نقاط نگهداری یا نمایش داده می‌شود.",
                    color = Color(0xFF9A6519), fontSize = 12.sp)
                if (points.length() > 0) {
                    val last = points.optJSONObject(points.length()-1)
                    Text("آخرین مختصات: " + last?.optDouble("lat") +
                        " ، " + last?.optDouble("lng"),
                        color = trackingMuted, fontSize = 11.sp)
                }
            }
        }
        Text(
            "اطلاعات هنگام فعال بودن مسیر تقریباً هر ۳۰ ثانیه ارسال می‌شوند. " +
            "تأخیر اینترنت، GPS و محدودیت‌های گوشی ممکن است زمان به‌روزرسانی را افزایش دهند.",
            color = trackingMuted, fontSize = 11.sp
        )
    }
}

/** No third-party JavaScript: bundled renderer with OSM tiles over HTTPS. */
@Composable
private fun ArvinLiveRouteMap(points: JSONArray) {
    val context = LocalContext.current
    val coordinateArray = remember(points) {
        JSONArray().apply {
            for (i in 0 until points.length()) {
                val p = points.optJSONObject(i) ?: continue
                val lat = p.optDouble("lat", Double.NaN)
                val lng = p.optDouble("lng", Double.NaN)
                if (lat.isFinite() && lng.isFinite()) {
                    put(JSONObject().put("lat", lat).put("lng", lng))
                }
            }
        }.toString()
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(335.dp),
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setBackgroundColor(android.graphics.Color.rgb(235,238,235))
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val current = view?.tag as? String ?: "[]"
                        view?.evaluateJavascript("window.setRoute(" + current + ");", null)
                    }
                }
                tag = coordinateArray
                val html = context.assets.open("arvin_tracking_map.html")
                    .bufferedReader().use { it.readText() }
                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net/",
                    html, "text/html", "UTF-8", null
                )
            }
        },
        update = { view ->
            view.tag = coordinateArray
            view.evaluateJavascript("if(window.setRoute){window.setRoute(" +
                coordinateArray + ");}", null)
        }
    )
}

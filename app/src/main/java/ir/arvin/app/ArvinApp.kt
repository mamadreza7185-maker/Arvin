package ir.arvin.app

import android.webkit.WebView
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val navy = Color(0xFF12243E)
private val gold = Color(0xFFB88B4D)
private val bg = Color(0xFFF4F7FB)
private val muted = Color(0xFF64748B)

private enum class Tab(val title: String, val icon: ImageVector) {
    HOME("خانه", Icons.Filled.Home),
    SERVICES("سامانه", Icons.Filled.Apps),
    REPORTS("گزارش‌ها", Icons.Filled.Assessment),
    PROFILE("حساب من", Icons.Filled.Person)
}

@Composable
fun ArvinApp(
    webView: WebView,
    loading: Boolean,
    webError: String?,
    onReload: () -> Unit,
    hasLocationPermission: () -> Boolean
) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val context = LocalContext.current
    var webMountedOnce by remember { mutableStateOf(false) }
    val openInChrome = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView.url ?: "https://script.google.com/macros/s/AKfycbwAU8DUulyp17fRUnmZFIIJ_IoYJhWqZ14lmrpdVSYoq-GFFAzFaNLthXBHjOQwb9US/exec")))
    }
    BackHandler(enabled = tab != Tab.HOME || (tab == Tab.SERVICES && webView.canGoBack())) {
        if (tab == Tab.SERVICES && webView.canGoBack()) webView.goBack()
        else tab = Tab.HOME
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(navy)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text("پخش آروین", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "پخش مواد غذایی، بهداشتی و شوینده",
                    color = Color(0xFFD9E3F0), fontSize = 12.sp
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navy,
                            selectedTextColor = navy,
                            indicatorColor = Color(0xFFF0E6D8),
                            unselectedIconColor = muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Keep the same WebView instance alive across tabs so login/session is preserved.
            // It is hosted in the Compose tree only when the service tab is selected.
            if (tab == Tab.SERVICES) {
                Column(Modifier.fillMaxSize()) {
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = gold)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onReload) { Text("بارگذاری دوباره") }
                        TextButton(onClick = openInChrome) { Text("باز کردن در مرورگر") }
                    }
                    if (webError != null) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(webError, color = Color(0xFFB42318), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onReload) { Text("تلاش دوباره") }
                        }
                    }
                    AndroidView(
                        factory = {
                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = { view ->
                            if (!webMountedOnce) {
                                webMountedOnce = true
                                view.post { view.reload() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                when (tab) {
                    Tab.HOME -> HomeScreen(onOpen = { tab = Tab.SERVICES })
                    Tab.REPORTS -> ReportsScreen(onOpen = { tab = Tab.SERVICES })
                    Tab.PROFILE -> ProfileScreen(
                        hasLocationPermission = hasLocationPermission,
                        onOpen = { tab = Tab.SERVICES }
                    )
                    Tab.SERVICES -> Unit
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = navy),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("سامانه مدیریت پخش آروین", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text("حضور و غیاب، مسیر ویزیتورها و گزارش‌های روزانه در یک برنامه",
                    color = Color(0xFFD9E3F0), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = gold)
                ) {
                    Icon(Icons.Filled.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ورود به سامانه")
                }
            }
        }
        Text("دسترسی سریع", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = navy)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutCard("حضور و غیاب", "ثبت ورود و خروج", Icons.Filled.AccessTime,
                Modifier.weight(1f), onOpen)
            ShortcutCard("مسیر ویزیتور", "شروع و پایان مسیر", Icons.Filled.LocationOn,
                Modifier.weight(1f), onOpen)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutCard("گزارش روزانه", "فروش و عملکرد", Icons.Filled.Assessment,
                Modifier.weight(1f), onOpen)
            ShortcutCard("درخواست خروج", "ثبت درخواست ساعتی", Icons.Filled.EventNote,
                Modifier.weight(1f), onOpen)
        }
        Text("اطلاعات واقعی پس از ورود به سامانه فعلی نمایش داده می‌شود.",
            color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(30.dp))
            Text(title, color = navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReportsScreen(onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("گزارش‌های مدیریتی", fontSize = 21.sp, color = navy, fontWeight = FontWeight.Bold)
        Text("برای مشاهده گزارش‌های واقعی و دسترسی‌های مجاز خود وارد سامانه شوید.",
            color = muted, fontSize = 13.sp)
        ReportRow("گزارش حضور و غیاب", Icons.Filled.AccessTime, onOpen)
        ReportRow("گزارش مسیر ویزیتورها", Icons.Filled.LocationOn, onOpen)
        ReportRow("گزارش فروش و وصول", Icons.Filled.BarChart, onOpen)
        ReportRow("درخواست‌های کارکنان", Icons.Filled.EventNote, onOpen)
    }
}

@Composable
private fun ReportRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = gold)
            Spacer(Modifier.width(14.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium,
                color = navy)
            Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = muted)
        }
    }
}

@Composable
private fun ProfileScreen(hasLocationPermission: () -> Boolean, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("حساب کاربری", fontSize = 21.sp, color = navy, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("پخش آروین", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = navy)
                Spacer(Modifier.height(8.dp))
                Text("اطلاعات حساب و سطح دسترسی از سامانه دریافت می‌شود.", color = muted)
                Spacer(Modifier.height(14.dp))
                Text(
                    if (hasLocationPermission()) "دسترسی موقعیت مکانی: فعال"
                    else "دسترسی موقعیت مکانی: غیرفعال",
                    color = if (hasLocationPermission()) Color(0xFF16794A) else muted
                )
            }
        }
        Button(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = navy)
        ) {
            Text("ورود به حساب / مدیریت نشست")
        }
        Text("نسخه ۱.۱ • رابط کاربری اندروید", color = muted, fontSize = 12.sp)
    }
}

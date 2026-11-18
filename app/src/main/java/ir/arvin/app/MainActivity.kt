package ir.arvin.app

import android.Manifest
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Native Android UI and navigation shell.
 *
 * Existing attendance, sales, reports, and authentication still run in
 * the existing Google Apps Script application until an authenticated API
 * is provided. Do not duplicate or fake employee data in the Android UI.
 */
class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var isLoading by mutableStateOf(true)
    private var pageError by mutableStateOf<String?>(null)
    private var nativeLogin by mutableStateOf(true)
    private var loginBusy by mutableStateOf(false)
    private var loginMessage by mutableStateOf<String?>(null)
    private var loggedInName by mutableStateOf<String?>(null)
    private var sessionToken: String? = null
    private val apiExecutor = Executors.newSingleThreadExecutor()
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    companion object {
        private const val APP_URL =
            "https://script.google.com/macros/s/AKfycbwAU8DUulyp17fRUnmZFIIJ_IoYJhWqZ14lmrpdVSYoq-GFFAzFaNLthXBHjOQwb9US/exec"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = hasLocationPermission()
        pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
        pendingGeoCallback = null
        pendingGeoOrigin = null
        if (!granted) {
            Toast.makeText(
                this,
                "برای ثبت موقعیت مکانی، اجازه دسترسی لازم است.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(18, 34, 61)
        window.navigationBarColor = android.graphics.Color.WHITE

        CookieManager.getInstance().setAcceptCookie(true)
        webView = WebView(this).apply {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setGeolocationEnabled(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url
                    if (uri.scheme != "https") return true
                    val host = uri.host.orEmpty().lowercase()
                    val googleHost = host == "script.google.com" ||
                        host == "accounts.google.com" ||
                        host == "google.com" ||
                        host.endsWith(".google.com") ||
                        host == "googleusercontent.com" ||
                        host.endsWith(".googleusercontent.com")
                    if (googleHost) return false

                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    } catch (_: Exception) {
                        true
                    }
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    isLoading = true
                    pageError = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    isLoading = false
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        isLoading = false
                        pageError = "اتصال به سامانه برقرار نشد. اینترنت را بررسی و دوباره تلاش کنید."
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) {
                    val host = Uri.parse(origin).host.orEmpty().lowercase()
                    val trusted = origin.startsWith("https://") &&
                        (host == "script.google.com" ||
                            host == "googleusercontent.com" ||
                            host.endsWith(".googleusercontent.com"))
                    if (!trusted) {
                        callback.invoke(origin, false, false)
                        return
                    }
                    if (hasLocationPermission()) {
                        callback.invoke(origin, true, false)
                    } else {
                        pendingGeoOrigin = origin
                        pendingGeoCallback = callback
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL)
        } else {
            val restored = webView.restoreState(savedInstanceState)
            if (restored == null) webView.loadUrl(APP_URL)
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF183456),
                    secondary = Color(0xFFB48A4A),
                    surface = Color.White,
                    background = Color(0xFFF4F7FB)
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (nativeLogin) {
                        NativeLoginScreen(
                            busy = loginBusy,
                            message = loginMessage,
                            loggedInName = loggedInName,
                            onLogin = { username, password -> loginNative(username, password) },
                            onLogout = { sessionToken = null; loggedInName = null; loginMessage = null },
                            onBrowser = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APP_URL)))
                            }
                        )
                    } else ArvinApp(
                        webView = webView,
                        loading = isLoading,
                        webError = pageError,
                        onReload = {
                            pageError = null
                            webView.reload()
                        },
                        hasLocationPermission = { hasLocationPermission() }
                    )
                }
            }
        }
    }

    private fun loginNative(username: String, password: String) {
        if (loginBusy) return
        loginBusy = true
        loginMessage = null
        apiExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(APP_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 25000
                    doOutput = true
                    instanceFollowRedirects = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val body = JSONObject().put("action", "login").put("username", username.trim())
                    .put("password", password).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val raw = connection.inputStream.bufferedReader().use { it.readText() }
                val response = JSONObject(raw)
                Handler(Looper.getMainLooper()).post {
                    loginBusy = false
                    if (response.optBoolean("ok")) {
                        sessionToken = response.optString("token")
                        loggedInName = response.optJSONObject("user")?.optString("name") ?: username
                        loginMessage = "ورود موفق بود. اتصال امکانات به نسخه بومی در حال تکمیل است."
                    } else loginMessage = response.optString("message", "ورود ناموفق بود.")
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    loginBusy = false
                    loginMessage = "ارتباط با API برقرار نشد. اتصال اینترنت و نسخه انتشار را بررسی کنید."
                }
            } finally { connection?.disconnect() }
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        pendingGeoCallback?.invoke(pendingGeoOrigin, false, false)
        pendingGeoCallback = null
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        apiExecutor.shutdownNow()
        super.onDestroy()
    }
}


@androidx.compose.runtime.Composable
private fun NativeLoginScreen(
    busy: Boolean,
    message: String?,
    loggedInName: String?,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onBrowser: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("پخش آروین", fontSize = 27.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFF12243E))
        Spacer(Modifier.height(8.dp))
        Text("ورود امن به سامانه کارکنان", color = Color(0xFF64748B))
        Spacer(Modifier.height(26.dp))
        if (loggedInName == null) {
            OutlinedTextField(value = username, onValueChange = { username = it },
                label = { Text("نام کاربری") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("رمز عبور") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onLogin(username, password) }, enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) { Text(if (busy) "در حال بررسی..." else "ورود") }
        } else {
            Text("خوش آمدید، $loggedInName", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onLogout) { Text("خروج از حساب") }
        }
        if (message != null) {
            Spacer(Modifier.height(14.dp))
            Text(message, color = Color(0xFF45546C))
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBrowser) { Text("ورود از طریق مرورگر (نسخه فعلی)") }
    }
}

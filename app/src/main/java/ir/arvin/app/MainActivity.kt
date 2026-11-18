package ir.arvin.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Native login and a fully native, role-aware interface for Arvin Distribution. */
class MainActivity : ComponentActivity() {
    companion object {
        private const val APP_URL = "https://script.google.com/macros/s/AKfycbwAU8DUulyp17fRUnmZFIIJ_IoYJhWqZ14lmrpdVSYoq-GFFAzFaNLthXBHjOQwb9US/exec"
    }

    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var currentToken: String? = null
    private var displayName by mutableStateOf<String?>(null)
    private var userRole by mutableStateOf("")
    private var userName by mutableStateOf("")
    private var loginBusy by mutableStateOf(false)
    private var loginMessage by mutableStateOf<String?>(null)
    private var pendingLocation: ((JSONObject?, String?) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val callback = pendingLocation
        pendingLocation = null
        if (hasLocationPermission()) {
            if (callback != null) fetchLocation(callback)
        } else {
            callback?.invoke(null, "برای ثبت حضور، مسیر و گزارش، اجازه دسترسی به موقعیت مکانی لازم است.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(18, 36, 62)
        window.navigationBarColor = AndroidColor.WHITE
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF12243E),
                secondary = Color(0xFFB88B4D),
                background = Color(0xFFF4F7FB)
            )) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    val logged = displayName
                    if (logged == null) {
                        NativeLoginScreen(
                            busy = loginBusy,
                            message = loginMessage,
                            onLogin = ::loginNative
                        )
                    } else {
                        ArvinNativeDashboard(
                            name = logged,
                            username = userName,
                            role = userRole,
                            onRequest = ::request,
                            onLogout = ::logoutNative
                        )
                    }
                }
            }
        }
    }

    private fun postJson(payload: JSONObject, callback: (JSONObject) -> Unit) {
        worker.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = (URL(APP_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    instanceFollowRedirects = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                }
                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                val stream = if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                JSONObject(raw)
            } catch (_: Exception) {
                JSONObject().put("ok", false).put(
                    "message", "ارتباط با سامانه برقرار نشد. اینترنت یا انتشار Google Apps Script را بررسی کنید."
                )
            } finally {
                connection?.disconnect()
            }
            main.post { if (!isFinishing && !isDestroyed) callback(result) }
        }
    }

    private fun loginNative(username: String, password: String) {
        if (loginBusy) return
        loginBusy = true
        loginMessage = null
        val body = JSONObject().put("action", "login")
            .put("username", username.trim()).put("password", password)
        postJson(body) { result ->
            loginBusy = false
            if (result.optBoolean("ok") && result.optString("token").isNotEmpty()) {
                val user = result.optJSONObject("user")
                currentToken = result.optString("token")
                userName = user?.optString("username", username.trim()) ?: username.trim()
                userRole = user?.optString("role", "warehouse") ?: "warehouse"
                displayName = user?.optString("name", userName) ?: userName
                loginMessage = null
            } else {
                loginMessage = result.optString("message", "ورود ناموفق بود.")
            }
        }
    }

    /** Calls the existing secure Apps Script API, never putting tokens in URL parameters. */
    private fun request(
        action: String,
        data: JSONObject,
        requireGps: Boolean,
        callback: (JSONObject) -> Unit
    ) {
        val token = currentToken
        if (token.isNullOrBlank()) {
            callback(JSONObject().put("ok", false).put("message", "نشست تمام شده؛ دوباره وارد شوید."))
            return
        }
        val send = { form: JSONObject ->
            postJson(
                JSONObject().put("action", action).put("token", token).put("data", form),
                callback
            )
        }
        if (requireGps) {
            fetchLocation { location, error ->
                if (location != null) {
                    data.put("location", location)
                    send(data)
                } else {
                    callback(JSONObject().put("ok", false).put(
                        "message", error ?: "موقعیت مکانی به دست نیامد."
                    ))
                }
            }
        } else {
            send(data)
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun fetchLocation(callback: (JSONObject?, String?) -> Unit) {
        if (!hasLocationPermission()) {
            pendingLocation = callback
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider == null) {
                callback(null, "موقعیت مکانی گوشی را روشن کنید.")
                return
            }
            var completed = false
            fun finish(location: Location?, error: String?) {
                if (completed) return
                completed = true
                if (location != null) {
                    callback(JSONObject()
                        .put("lat", location.latitude)
                        .put("lng", location.longitude)
                        .put("accuracy", location.accuracy.toDouble()), null)
                } else callback(null, error)
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    finish(location, null)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Legacy Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            main.postDelayed({
                if (!completed) {
                    locationManager.removeUpdates(listener)
                    finish(null, "دریافت GPS بیش از حد طول کشید. در فضای باز دوباره تلاش کنید.")
                }
            }, 25000)
        } catch (_: SecurityException) {
            callback(null, "مجوز موقعیت مکانی صادر نشده است.")
        } catch (_: Exception) {
            callback(null, "موقعیت مکانی در دسترس نیست.")
        }
    }

    private fun logoutNative() {
        val token = currentToken
        currentToken = null
        displayName = null
        userName = ""
        userRole = ""
        loginMessage = null
        if (!token.isNullOrBlank()) {
            postJson(JSONObject().put("action", "logout").put("token", token)) {}
        }
    }

    override fun onDestroy() {
        pendingLocation = null
        worker.shutdownNow()
        super.onDestroy()
    }
}

@Composable
private fun NativeLoginScreen(
    busy: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("پخش آروین", fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFF12243E))
        Spacer(Modifier.height(8.dp))
        Text("سامانه حضور و غیاب و مدیریت کارکنان", color = Color(0xFF64748B))
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(value = username, onValueChange = { username = it },
            label = { Text("نام کاربری") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("رمز عبور") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onLogin(username, password) },
            enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (busy) "در حال ورود..." else "ورود به برنامه")
        }
        if (message != null) {
            Spacer(Modifier.height(14.dp))
            Text(message, color = Color(0xFFB42318))
        }
        Spacer(Modifier.height(20.dp))
        Text("نسخه بومی اندروید • پخش مواد غذایی، بهداشتی و شوینده",
            color = Color(0xFF64748B), fontSize = 11.sp)
    }
}

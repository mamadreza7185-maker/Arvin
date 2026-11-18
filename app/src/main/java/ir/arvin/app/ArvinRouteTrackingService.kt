package ir.arvin.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Explicitly started, visible Android location foreground service.
 *
 * Records one GPS fix around every 30 seconds and sends batches to Apps Script.
 * There is no hidden or automatic start, and STOP unregisters GPS immediately.
 * A session-scoped token can only upload locations and stop its own route.
 */
class ArvinRouteTrackingService : Service(), LocationListener {
    companion object {
        private const val ENDPOINT =
            "https://script.google.com/macros/s/AKfycbwAU8DUulyp17fRUnmZFIIJ_IoYJhWqZ14lmrpdVSYoq-GFFAzFaNLthXBHjOQwb9US/exec"
        private const val CHANNEL = "arvin_active_route_v16"
        private const val NOTIFICATION_ID = 607
        private const val PREFS = "arvin_tracking_queue_v16"
        private const val ACTION_START = "ir.arvin.app.ROUTE_START"
        private const val ACTION_STOP = "ir.arvin.app.ROUTE_STOP"
        private const val EXTRA_SESSION = "route_session_id"
        private const val EXTRA_TOKEN = "route_upload_token"

        fun start(context: Context, sessionId: String, trackingToken: String) {
            val intent = Intent(context, ArvinRouteTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SESSION, sessionId)
                .putExtra(EXTRA_TOKEN, trackingToken)
            ContextCompat.startForegroundService(context, intent)
        }
        fun stop(context: Context) {
            val intent = Intent(context, ArvinRouteTrackingService::class.java).setAction(ACTION_STOP)
            // Avoid bringing up a new service from background; the notification
            // PendingIntent goes to a running service, and UI is already visible.
            context.startService(intent)
        }
        fun isActive(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("active", false)
    }

    private lateinit var manager: LocationManager
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val queueLock = Any()
    private var sessionId = ""
    private var trackingToken = ""
    private var tracking = false
    private var finishing = false
    private var uploading = false
    private var lastLocationAt = 0L
    private var startedAt = 0L
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Disable location immediately even if the network is offline.
            stopCollecting()
            finishing = true
            updateNotification("موقعیت‌یابی متوقف شد؛ در حال ارسال اطلاعات نهایی")
            flush()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (tracking) return START_NOT_STICKY

        val id = intent.getStringExtra(EXTRA_SESSION).orEmpty()
        val secret = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        if (id.isBlank() || secret.length < 32) {
            stopSelf()
            return START_NOT_STICKY
        }
        sessionId = id
        trackingToken = secret
        startedAt = System.currentTimeMillis()
        finishing = false
        prefs.edit()
            .putString("sessionId", id)
            .putString("queue", "[]")
            .putLong("lastGps", 0L)
            .putLong("lastUpload", 0L)
            .remove("lastError")
            .apply()
        try {
            val notification = buildNotification("ثبت مسیر فعال است؛ GPS تا پایان مسیر روشن می‌ماند")
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else startForeground(NOTIFICATION_ID, notification)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                finishing = true
                updateNotification("مجوز موقعیت مکانی وجود ندارد؛ توقف ردیابی")
                flush()
                return START_NOT_STICKY
            }

            tracking = true
            prefs.edit().putBoolean("active", true).apply()
            var anyProvider = false
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 30000L, 0f, this, Looper.getMainLooper()
                )
                anyProvider = true
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 30000L, 0f, this, Looper.getMainLooper()
                )
                anyProvider = true
            }
            if (!anyProvider) {
                stopCollecting()
                finishing = true
                updateNotification("GPS خاموش شده است؛ در حال بستن مسیر")
                flush()
            } else {
                main.postDelayed(::tick, 30000L)
            }
        } catch (_: Exception) {
            prefs.edit().putString("lastError",
                "سرویس GPS اجرا نشد؛ مجوز موقعیت یا تنظیمات باتری گوشی را بررسی کنید.").apply()
            stopCollecting()
            finishing = true
            // Exit cleanly if Android rejects the foreground service start.
            if (trackingToken.isNotBlank()) flush() else stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        if (!tracking || finishing || !location.hasAccuracy() ||
            location.accuracy > 200f ||
            System.currentTimeMillis() - lastLocationAt < 27000L) return

        val now = System.currentTimeMillis()
        lastLocationAt = now
        val point = JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("time", location.time.takeIf { it > 0 } ?: now)
            .put("lat", location.latitude)
            .put("lng", location.longitude)
            .put("accuracy", location.accuracy.toDouble())
        synchronized(queueLock) {
            val old = JSONArray(prefs.getString("queue", "[]") ?: "[]")
            old.put(point)
            prefs.edit()
                .putString("queue", old.toString())
                .putLong("lastGps", now)
                .apply()
        }
        flush()
    }

    private fun tick() {
        if (!tracking && !finishing) return
        if (System.currentTimeMillis() - startedAt > 13L * 60L * 60L * 1000L) {
            stopCollecting()
            finishing = true
            updateNotification("مدت مجاز ردیابی تمام شد؛ در حال ثبت پایان مسیر")
        }
        flush()
        main.postDelayed(::tick, if (finishing) 60000L else 30000L)
    }

    private fun stopCollecting() {
        tracking = false
        prefs.edit().putBoolean("active", false).apply()
        try { manager.removeUpdates(this) } catch (_: Exception) {}
    }

    /** While this service runs, retain failed uploads in app-private storage. */
    private fun flush() {
        if (uploading || sessionId.isBlank() || trackingToken.isBlank()) return
        uploading = true
        executor.execute {
            try {
                val batch = synchronized(queueLock) {
                    val all = JSONArray(prefs.getString("queue", "[]") ?: "[]")
                    JSONArray().apply {
                        for (i in 0 until minOf(all.length(), 20)) put(all.getJSONObject(i))
                    }
                }
                if (batch.length() > 0) {
                    val response = post("trackingAppend", JSONObject()
                        .put("sessionId", sessionId)
                        .put("trackingToken", trackingToken)
                        .put("points", batch))
                    if (!response.optBoolean("ok")) {
                        prefs.edit().putString("lastError",
                            response.optString("message", "ارسال نقاط ناموفق بود.")).apply()
                        return@execute
                    }
                    prefs.edit().putLong("lastUpload", System.currentTimeMillis())
                        .remove("lastError").apply()
                    synchronized(queueLock) {
                        val all = JSONArray(prefs.getString("queue", "[]") ?: "[]")
                        val left = JSONArray()
                        for (i in batch.length() until all.length()) left.put(all.get(i))
                        prefs.edit().putString("queue", left.toString()).apply()
                    }
                }
                if (finishing) {
                    val isEmpty = synchronized(queueLock) {
                        JSONArray(prefs.getString("queue", "[]") ?: "[]").length() == 0
                    }
                    if (isEmpty) {
                        val stopped = post("trackingStop", JSONObject()
                            .put("sessionId", sessionId)
                            .put("trackingToken", trackingToken))
                        if (stopped.optBoolean("ok")) {
                            prefs.edit().putLong("lastUpload", System.currentTimeMillis())
                                .remove("lastError").apply()
                            main.post { stopSelf() }
                        } else {
                            prefs.edit().putString("lastError",
                                stopped.optString("message", "ثبت پایان مسیر ناموفق بود.")).apply()
                        }
                    }
                }
            } catch (_: Exception) {
                prefs.edit().putString("lastError",
                    "ارتباط با سرور ردیابی برقرار نشد؛ بعد از اتصال اینترنت دوباره تلاش می‌شود.")
                    .apply()
                // Preserve pending points. Next tick retries when internet returns.
            } finally {
                main.post { uploading = false }
            }
        }
    }

    private fun post(action: String, data: JSONObject): JSONObject {
        var c: HttpURLConnection? = null
        try {
            c = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            val body = JSONObject().put("action", action).put("data", data).toString()
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val response = c.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response)
        } finally { c?.disconnect() }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL, "ردیابی فعال مسیر ویزیتور", NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "ردیابی با اطلاع کاربر فقط بین شروع و پایان مسیر انجام می‌شود."
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ArvinRouteTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("پخش آروین • ثبت زنده مسیر")
            .setContentText(message)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "پایان مسیر و توقف GPS", stop)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    override fun onDestroy() {
        stopCollecting()
        main.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }
}

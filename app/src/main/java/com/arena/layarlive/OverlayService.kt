package com.arena.layarlive

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayRoot: FrameLayout? = null
    private val webViews = mutableListOf<WebView>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForegroundService()
        removeOverlay()
        val scene = SceneStore.load(this)
        showScene(scene)
        return START_NOT_STICKY
    }

    private fun startAsForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopService = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("LayarLive • overlay aktif")
            .setContentText("Scene terkunci; sentuhan game diteruskan.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopService)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LayarLive overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifikasi saat scene overlay sedang aktif"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showScene(scene: SceneConfig) {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        try {
            windowManager.addView(root, params)
            overlayRoot = root
            root.post {
                if (overlayRoot !== root || root.width <= 0 || root.height <= 0) return@post
                if (scene.saweriaEnabled && isValidSaweriaUrl(scene.saweriaUrl)) {
                    addSaweriaWidget(root, scene)
                }
                if (scene.textEnabled && scene.text.isNotBlank()) {
                    addTextLayer(root, scene)
                }
            }
        } catch (_: Exception) {
            overlayRoot = null
            stopSelf()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun addSaweriaWidget(root: FrameLayout, scene: SceneConfig) {
        val url = scene.saweriaUrl
        val webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return !(uri.scheme == "https" && isSaweriaHost(uri.host))
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    if (request.isForMainFrame) showWidgetError(root, scene.saweriaBox)
                }
            }
        }
        webViews.add(webView)
        root.addView(webView, layoutParamsFor(root, scene.saweriaBox))
        webView.loadUrl(url)
    }

    private fun showWidgetError(root: FrameLayout, box: LayerBox) {
        if (overlayRoot !== root) return
        val error = TextView(this).apply {
            text = "Saweria gagal dimuat • cek koneksi / URL"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.argb(225, 30, 24, 49), Color.rgb(167, 139, 250), 14)
        }
        root.addView(error, layoutParamsFor(root, box))
    }

    private fun addTextLayer(root: FrameLayout, scene: SceneConfig) {
        val textView = TextView(this).apply {
            text = scene.text
            textSize = scene.textSizeSp.coerceIn(12f, 38f)
            setTextColor(scene.textColor)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            background = rounded(scene.textBackground, Color.argb(125, 192, 177, 255), 14)
            setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Color.argb(100, 0, 0, 0))
        }
        root.addView(textView, layoutParamsFor(root, scene.textBox))
    }

    private fun layoutParamsFor(root: FrameLayout, box: LayerBox): FrameLayout.LayoutParams {
        val screenWidth = root.width.coerceAtLeast(1)
        val screenHeight = root.height.coerceAtLeast(1)
        val width = (screenWidth * box.width.coerceIn(0.08f, 1f)).roundToInt().coerceAtLeast(dp(58))
            .coerceAtMost(screenWidth)
        val height = (screenHeight * box.height.coerceIn(0.075f, 1f)).roundToInt().coerceAtLeast(dp(38))
            .coerceAtMost(screenHeight)
        val x = (screenWidth * box.x.coerceIn(0f, 1f)).roundToInt().coerceAtMost(screenWidth - width)
        val y = (screenHeight * box.y.coerceIn(0f, 1f)).roundToInt().coerceAtMost(screenHeight - height)
        return FrameLayout.LayoutParams(width, height, Gravity.TOP or Gravity.START).apply {
            leftMargin = x
            topMargin = y
        }
    }

    private fun isValidSaweriaUrl(value: String): Boolean = try {
        val uri = Uri.parse(value)
        uri.scheme == "https" && isSaweriaHost(uri.host) &&
            uri.path.orEmpty().startsWith("/widgets/alert") &&
            !uri.getQueryParameter("streamKey").isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    private fun isSaweriaHost(host: String?): Boolean = host == "saweria.co" || host == "www.saweria.co"

    private fun rounded(fill: Int, stroke: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(fill)
        setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun removeOverlay() {
        mainHandler.removeCallbacksAndMessages(null)
        webViews.forEach { view ->
            try {
                view.stopLoading()
                view.onPause()
                view.removeAllViews()
                view.destroy()
            } catch (_: Exception) {
                // The WebView may already have been destroyed by Android.
            }
        }
        webViews.clear()
        val root = overlayRoot
        if (root != null) {
            try {
                windowManager.removeView(root)
            } catch (_: Exception) {
                // The system may already have removed the overlay window.
            }
        }
        overlayRoot = null
    }

    override fun onDestroy() {
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SCENE = "com.arena.layarlive.action.START_SCENE"
        const val ACTION_STOP = "com.arena.layarlive.action.STOP"

        private const val CHANNEL_ID = "layarlive_overlay_v2"
        private const val NOTIFICATION_ID = 2125
    }
}

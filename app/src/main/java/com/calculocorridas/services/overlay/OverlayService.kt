package com.calculocorridas.services.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.calculocorridas.R
import com.calculocorridas.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideOverlay() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_RIDE -> showRide(intent)
            ACTION_HIDE      -> hideOverlay()
        }
        return START_STICKY
    }

    private fun showRide(intent: Intent) {
        val data = OverlayData(
            appSource    = intent.getStringExtra(EXTRA_APP_SOURCE) ?: "",
            value        = intent.getDoubleExtra(EXTRA_RIDE_VALUE, 0.0),
            distanceKm   = intent.getDoubleExtra(EXTRA_RIDE_DISTANCE, 0.0),
            durationMin  = intent.getDoubleExtra(EXTRA_RIDE_DURATION, 0.0),
            valuePerKm   = intent.getDoubleExtra(EXTRA_VALUE_PER_KM, 0.0),
            valuePerHour = intent.getDoubleExtra(EXTRA_VALUE_PER_HOUR, 0.0),
            netProfit    = intent.getDoubleExtra(EXTRA_NET_PROFIT, 0.0),
            classification = intent.getStringExtra(EXTRA_CLASSIFICATION) ?: "poor"
        )

        if (overlayView == null) {
            createOverlayView(data)
        } else {
            overlayView?.update(data)
        }

        // Auto-hide após 30 segundos sem novo card
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, AUTO_HIDE_MS)
    }

    private fun createOverlayView(data: OverlayData) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w("OverlayService", "SYSTEM_ALERT_WINDOW not granted — overlay skipped")
            return
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 200
        }

        overlayView = OverlayView(this).apply {
            update(data)
            onDismiss = { hideOverlay() }
            onDrag = { dx, dy ->
                params.x += dx.toInt()
                params.y += dy.toInt()
                windowManager?.updateViewLayout(this, params)
            }
        }

        runCatching { windowManager?.addView(overlayView, params) }
            .onFailure { Log.e("OverlayService", "Failed to add overlay view", it) }
    }

    private fun hideOverlay() {
        handler.removeCallbacks(hideRunnable)
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, getString(R.string.notification_channel_service_id))
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitorando corridas…")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW_RIDE    = "com.calculocorridas.SHOW_RIDE"
        const val ACTION_HIDE         = "com.calculocorridas.HIDE_OVERLAY"
        const val EXTRA_APP_SOURCE    = "extra_app_source"
        const val EXTRA_RIDE_VALUE    = "extra_ride_value"
        const val EXTRA_RIDE_DISTANCE = "extra_ride_distance"
        const val EXTRA_RIDE_DURATION = "extra_ride_duration"
        const val EXTRA_VALUE_PER_KM  = "extra_value_per_km"
        const val EXTRA_VALUE_PER_HOUR= "extra_value_per_hour"
        const val EXTRA_NET_PROFIT    = "extra_net_profit"
        const val EXTRA_CLASSIFICATION= "extra_classification"
        const val NOTIFICATION_ID     = 1001
        private const val AUTO_HIDE_MS = 15_000L // 15 segundos
    }
}

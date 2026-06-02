package com.interactiveword.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.interactiveword.MainActivity
import com.interactiveword.R

class AudioCaptureService : Service() {

    companion object {
        const val CHANNEL_ID      = "audio_capture_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_SHOW_CONTROLS = "ACTION_SHOW_CONTROLS"
        const val ACTION_START_MINUS   = "ACTION_START_MINUS"
        const val ACTION_START_PLUS    = "ACTION_START_PLUS"
        const val ACTION_END_MINUS     = "ACTION_END_MINUS"
        const val ACTION_END_PLUS      = "ACTION_END_PLUS"
        const val ACTION_DISMISS       = "ACTION_DISMISS"
        const val ACTION_STOP_SERVICE  = "ACTION_STOP_SERVICE"

        // Extras read by MainActivity when the user confirms capture
        const val EXTRA_START_MS = "EXTRA_START_MS"
        const val EXTRA_END_MS   = "EXTRA_END_MS"
        const val EXTRA_URI      = "EXTRA_URI"

        const val TIME_STEP_MS = 5_000L
    }

    private var showingControls = false
    private var captureStartMs  = 0L
    private var captureEndMs    = 30_000L
    private var currentUri: Uri? = null
    private var videoTitle      = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_CONTROLS -> {
                val info = detectCurrentMedia()
                currentUri      = info.uri
                videoTitle      = info.title ?: getString(R.string.capture_video_unknown)
                captureStartMs  = maxOf(0L, info.positionMs - 10_000L)
                captureEndMs    = info.positionMs + 20_000L
                showingControls = true
                startForegroundWithType(buildControlsNotification())
            }
            ACTION_START_MINUS -> {
                captureStartMs = maxOf(0L, captureStartMs - TIME_STEP_MS)
                startForegroundWithType(buildControlsNotification())
            }
            ACTION_START_PLUS -> {
                captureStartMs = minOf(captureEndMs - TIME_STEP_MS, captureStartMs + TIME_STEP_MS)
                startForegroundWithType(buildControlsNotification())
            }
            ACTION_END_MINUS -> {
                captureEndMs = maxOf(captureStartMs + TIME_STEP_MS, captureEndMs - TIME_STEP_MS)
                startForegroundWithType(buildControlsNotification())
            }
            ACTION_END_PLUS -> {
                captureEndMs += TIME_STEP_MS
                startForegroundWithType(buildControlsNotification())
            }
            ACTION_DISMISS -> {
                showingControls = false
                startForegroundWithType(buildStandbyNotification())
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                showingControls = false
                startForegroundWithType(buildStandbyNotification())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ── Media detection ──────────────────────────────────────────────────────

    private data class MediaInfo(val uri: Uri?, val positionMs: Long, val title: String?)

    private fun detectCurrentMedia(): MediaInfo {
        return try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = msm.getActiveSessions(
                ComponentName(this, MediaWatcherService::class.java)
            )
            val controller = controllers.firstOrNull()
                ?: return MediaInfo(null, 0L, null)

            val posMs  = controller.playbackState?.position ?: 0L
            val title  = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val uriStr = controller.metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
                ?: controller.metadata?.description?.mediaUri?.toString()
            MediaInfo(uriStr?.let { Uri.parse(it) }, posMs, title)
        } catch (_: Exception) {
            MediaInfo(null, 0L, null)
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_capture),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, AudioCaptureService::class.java).also { it.action = action },
            PendingIntent.FLAG_IMMUTABLE,
        )

    /** Opens MainActivity directly with capture extras.
     *  PendingIntent.getActivity() triggered by notification interaction is always
     *  allowed on Android 12+, unlike startActivity() from a service context. */
    private fun confirmActivityIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 50,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_START_MS, captureStartMs)
                putExtra(EXTRA_END_MS,   captureEndMs)
                putExtra(EXTRA_URI,      currentUri?.toString())
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun buildStandbyNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_title_capture))
            .setContentText(getString(R.string.notification_text_standby))
            .setOngoing(true)
            .setContentIntent(serviceIntent(ACTION_SHOW_CONTROLS, 10))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                serviceIntent(ACTION_STOP_SERVICE, 11),
            )
            .build()

    private fun buildControlsNotification(): Notification {
        val rangeLabel = "${formatMs(captureStartMs)} ~ ${formatMs(captureEndMs)}"
        val confirmPi  = confirmActivityIntent()

        val bigView = RemoteViews(packageName, R.layout.notification_capture_controls)
        bigView.setTextViewText(R.id.tv_video_title, videoTitle)
        bigView.setTextViewText(R.id.tv_start_time,  formatMs(captureStartMs))
        bigView.setTextViewText(R.id.tv_end_time,    formatMs(captureEndMs))
        bigView.setOnClickPendingIntent(R.id.btn_start_minus, serviceIntent(ACTION_START_MINUS, 20))
        bigView.setOnClickPendingIntent(R.id.btn_start_plus,  serviceIntent(ACTION_START_PLUS,  21))
        bigView.setOnClickPendingIntent(R.id.btn_end_minus,   serviceIntent(ACTION_END_MINUS,   22))
        bigView.setOnClickPendingIntent(R.id.btn_end_plus,    serviceIntent(ACTION_END_PLUS,    23))
        bigView.setOnClickPendingIntent(R.id.btn_confirm,     confirmPi)
        bigView.setOnClickPendingIntent(R.id.btn_dismiss,     serviceIntent(ACTION_DISMISS,     25))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_title_set_range))
            .setContentText(rangeLabel)
            .setOngoing(true)
            .setCustomBigContentView(bigView)
            .addAction(0, "◀ 시작", serviceIntent(ACTION_START_MINUS, 30))
            .addAction(0, "종료 ▶", serviceIntent(ACTION_END_PLUS,    31))
            .addAction(0, "✓ 캡처", confirmPi)
            .build()
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }
}

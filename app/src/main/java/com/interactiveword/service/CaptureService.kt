package com.interactiveword.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.interactiveword.MainActivity
import com.interactiveword.R

/**
 * Foreground Service: 상단 알림창에 상시 대기하다가
 * 사용자가 "캡처 시작" 버튼을 탭하면 MediaProjection으로
 * 시스템 오디오(브라우저 탭 포함)를 5초간 캡처한다.
 */
class CaptureService : Service() {

    companion object {
        const val CHANNEL_ID       = "capture_channel"
        const val NOTIFICATION_ID  = 1

        const val ACTION_START_SERVICE  = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE   = "ACTION_STOP_SERVICE"
        const val ACTION_START_CAPTURE  = "ACTION_START_CAPTURE"

        // MediaProjection 권한 인텐트 (Activity에서 넘겨줌)
        const val EXTRA_RESULT_CODE   = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA   = "EXTRA_RESULT_DATA"
    }

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false

    // ── 생명주기 ──────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForeground(NOTIFICATION_ID, buildNotification(capturing = false))
            }
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != -1 && resultData != null) {
                    setupMediaProjection(resultCode, resultData)
                    startCapture()
                }
            }
            ACTION_STOP_SERVICE -> {
                stopCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        mediaProjection?.stop()
        super.onDestroy()
    }

    // ── MediaProjection 설정 ──────────────────────────────────────────────
    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
    }

    // ── 오디오 캡처 ──────────────────────────────────────────────────────
    private fun startCapture() {
        val projection = mediaProjection ?: return
        if (isCapturing) return

        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate   = 16000
        val channelMask  = android.media.AudioFormat.CHANNEL_IN_MONO
        val encoding     = android.media.AudioFormat.ENCODING_PCM_16BIT
        val bufferSize   = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        isCapturing = true
        audioRecord?.startRecording()

        // 5초 후 자동 중단 및 서버 전송
        Thread {
            val durationMs  = 5000L
            val startTime   = System.currentTimeMillis()
            val audioData   = mutableListOf<Short>()

            val buffer = ShortArray(bufferSize / 2)
            while (isCapturing && System.currentTimeMillis() - startTime < durationMs) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) audioData.addAll(buffer.take(read).toList())
            }

            stopCapture()
            // TODO: audioData → WAV 변환 → 백엔드 /scan/upload 전송
        }.start()

        updateNotification(capturing = true)
    }

    private fun stopCapture() {
        isCapturing = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        updateNotification(capturing = false)
    }

    // ── 알림 ──────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_capture),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(capturing: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notification_title_capture))
            .setContentText(
                if (capturing) "캡처 중... (5초)"
                else getString(R.string.notification_text_capture)
            )
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                PendingIntent.getService(
                    this, 1,
                    Intent(this, CaptureService::class.java).apply {
                        action = ACTION_STOP_SERVICE
                    },
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

    private fun updateNotification(capturing: Boolean) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(capturing))
    }
}

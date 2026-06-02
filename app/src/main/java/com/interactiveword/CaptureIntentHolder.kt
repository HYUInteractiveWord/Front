package com.interactiveword

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow

object CaptureIntentHolder {
    data class CaptureRequest(val uri: Uri?, val startMs: Long, val endMs: Long)
    val pendingCapture: MutableStateFlow<CaptureRequest?> = MutableStateFlow(null)
}

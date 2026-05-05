package com.interactiveword

import kotlinx.coroutines.flow.MutableStateFlow

object ShareIntentHolder {
    val pendingYoutubeUrl: MutableStateFlow<String?> = MutableStateFlow(null)
}

package com.interactiveword.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// v0 CSS: --radius: 1rem (16dp) → rounded-2xl
val AppShapes = Shapes(
    small        = RoundedCornerShape(8.dp),
    medium       = RoundedCornerShape(12.dp),
    large        = RoundedCornerShape(16.dp),    // 카드 기본
    extraLarge   = RoundedCornerShape(20.dp),    // 큰 카드, 버튼
)

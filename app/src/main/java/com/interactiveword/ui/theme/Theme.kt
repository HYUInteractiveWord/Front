package com.interactiveword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 앱은 항상 다크 테마 사용 (v0 디자인 기준)
private val AppColorScheme = darkColorScheme(
    primary             = BrandGreenLight,       // 버튼, 액티브 상태, XP바
    onPrimary           = Color(0xFF00391C),
    primaryContainer    = BrandGreenDim,
    onPrimaryContainer  = OnBrandGreenDim,

    secondary           = BrandAmberLight,       // 별점, 강조 배지
    onSecondary         = BrandAmberDim,
    secondaryContainer  = BrandAmberDim,
    onSecondaryContainer = OnBrandAmberDim,

    background          = DarkBackground,
    onBackground        = DarkOnBackground,

    surface             = DarkSurface,           // 카드 배경
    onSurface           = DarkOnBackground,
    surfaceVariant      = DarkSurfaceVariant,    // 입력 필드, 뮤트 카드
    onSurfaceVariant    = DarkMutedText,

    outline             = DarkOutline,           // 카드 테두리
    outlineVariant      = Color(0xFF2A2A2A),

    error               = ErrorRed,
    onError             = OnErrorRed,
)

@Composable
fun InteractiveWordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content,
    )
}

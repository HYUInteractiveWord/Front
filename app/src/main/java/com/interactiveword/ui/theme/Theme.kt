package com.interactiveword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary             = BrandGreenLight,       // 버튼, 액티브 상태, XP바
    onPrimary           = Color.White,
    primaryContainer    = BrandGreenDim,
    onPrimaryContainer  = OnBrandGreenDim,

    secondary           = BrandAmberLight,       // 별점, 강조 배지
    onSecondary         = DarkOnBackground,
    secondaryContainer  = BrandAmberDim,
    onSecondaryContainer = OnBrandAmberDim,

    background          = DarkBackground,
    onBackground        = DarkOnBackground,

    surface             = DarkSurface,           // 카드 배경
    onSurface           = DarkOnBackground,
    surfaceVariant      = DarkSurfaceVariant,    // 입력 필드, 뮤트 카드
    onSurfaceVariant    = GameTextMid,

    outline             = DarkOutline,           // 카드 테두리
    outlineVariant      = DarkOutline,

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

package com.interactiveword.ui.theme

import androidx.compose.ui.graphics.Color

// ── 브랜드 색상 ─────────────────────────────────────────────────────────────
// v0 CSS: --primary: oklch(0.65 0.2 145) → 중간 밝기 초록
val BrandGreen        = Color(0xFF52AD77)
val BrandGreenLight   = Color(0xFF82E0A8)   // 다크 테마 primary
val BrandGreenDim     = Color(0xFF00522A)   // primaryContainer (다크)
val OnBrandGreenDim   = Color(0xFF9EFCC3)   // onPrimaryContainer (다크)

// v0 CSS: --accent: oklch(0.75 0.18 75) → 황금빛 amber
val BrandAmber        = Color(0xFFC49820)
val BrandAmberLight   = Color(0xFFE8C97A)   // 다크 테마 secondary
val BrandAmberDim     = Color(0xFF3E2E00)   // secondaryContainer (다크)
val OnBrandAmberDim   = Color(0xFFFDD78A)   // onSecondaryContainer (다크)

// ── 다크 테마 배경/표면 ────────────────────────────────────────────────────
// v0 CSS: --background: oklch(0.145 0 0) ≈ #1A1A1A
val DarkBackground    = Color(0xFF1A1A1A)
// v0 CSS: --card: oklch(0.145 0 0)
val DarkSurface       = Color(0xFF222222)
// v0 CSS: --secondary / muted: oklch(0.269 0 0) ≈ #404040
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkOutline       = Color(0xFF3D3D3D)   // --border

// ── 텍스트 ─────────────────────────────────────────────────────────────────
// v0 CSS: --foreground: oklch(0.985 0 0) ≈ white
val DarkOnBackground  = Color(0xFFFAFAFA)
// v0 CSS: --muted-foreground: oklch(0.708 0 0) ≈ medium gray
val DarkMutedText     = Color(0xFFB3B3B3)

// ── 상태 색상 ──────────────────────────────────────────────────────────────
val ErrorRed          = Color(0xFFCF6679)
val OnErrorRed        = Color(0xFF690020)

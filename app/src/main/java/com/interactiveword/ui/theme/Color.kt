package com.interactiveword.ui.theme

import androidx.compose.ui.graphics.Color

// ── 게임 민트 테마 팔레트 ──────────────────────────────────────────────────
val GameBgMain        = Color(0xFFF5F7FF)
val GameBgCard        = Color(0xFFFFFFFF)
val GameBgCardHover   = Color(0xFFF8FFF9)

val GameMint          = Color(0xFF00C896)
val GameMintDark      = Color(0xFF00A878)
val GameMintLight     = Color(0xFFE0FAF3)

val GameCoral         = Color(0xFFFF6B6B)
val GameCoralLight    = Color(0xFFFFE8E8)
val GameGold          = Color(0xFFFFD700)
val GamePurple        = Color(0xFF9B59B6)

val GameTextDark      = Color(0xFF1A1A2E)
val GameTextMid       = Color(0xFF555577)
val GameTextLight     = Color(0xFF9999BB)

val GameBorder        = Color(0xFFEEEEF5)

val GradeCommon       = Color(0xFFB0BEC5)
val GradeUncommon     = Color(0xFF66BB6A)
val GradeRare         = Color(0xFFFFD700)
val GradeEpic         = Color(0xFFAB47BC)
val GradeLegendaryStart = Color(0xFFFF6B6B)
val GradeLegendaryEnd = Color(0xFF00C896)

// 기존 코드 호환용 별칭. 이름은 유지하고 값만 민트 라이트 테마로 교체한다.
val BrandGreen        = GameMint
val BrandGreenLight   = GameMint
val BrandGreenDim     = GameMintLight
val OnBrandGreenDim   = GameMint

val BrandAmber        = GameGold
val BrandAmberLight   = GameGold
val BrandAmberDim     = Color(0xFFFFF8DC)
val OnBrandAmberDim   = GameTextDark

val DarkBackground    = GameBgMain
val DarkSurface       = GameBgCard
val DarkSurfaceVariant = GameBgCardHover
val DarkOutline       = GameBorder

val DarkOnBackground  = GameTextDark
val DarkMutedText     = GameTextLight

val ErrorRed          = GameCoral
val OnErrorRed        = Color(0xFFFFFFFF)

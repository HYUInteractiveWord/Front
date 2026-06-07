package com.interactiveword.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val defaultFontFamily = FontFamily.SansSerif

fun getAppTypography(languageCode: String): Typography {
    val isRu = languageCode == "ru"
    val diff = if (isRu) 3 else 0 // 러시아어는 폰트 크기를 더 줄임 (기존 2 -> 3)

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = (28 - diff).sp,
            lineHeight = (36 - diff).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = (24 - diff).sp,
            lineHeight = (32 - diff).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = (20 - diff).sp,
            lineHeight = (28 - diff).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize   = (16 - (diff / 2)).sp, // 중간 크기는 1sp만 줄임
            lineHeight = (24 - diff).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = (16 - diff).sp,
            lineHeight = (24 - diff).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = (14 - diff).sp,
            lineHeight = (20 - diff).sp,
        ),
        bodySmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = (12 - diff).sp,
            lineHeight = (16 - diff).sp,
        ),
        labelMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = (12 - diff).sp,
            lineHeight = (16 - diff).sp,
        ),
    )
}
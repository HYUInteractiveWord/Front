package com.interactiveword.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.interactiveword.ui.theme.DarkOutline

data class WordCardEffectStyle(
    val label: String,
    val borderColor: Color,
    val progressColor: Color,
    val containerColor: Color?,
    val showSparkle: Boolean,
)

fun wordCardEffectStyle(wordPoint: Int): WordCardEffectStyle {
    val point = wordPoint.coerceIn(0, 100)

    return when {
        point >= 100 -> WordCardEffectStyle(
            label = "MASTER",
            borderColor = Color(0xFFFFC107),      // 금색
            progressColor = Color(0xFFFFC107),    // 금색
            containerColor = null,                
            showSparkle = true,
        )

        point >= 76 -> WordCardEffectStyle(
            label = "숙련",
            borderColor = Color(0xFF9B7EDE),      // 보라색
            progressColor = Color(0xFF9B7EDE),
            containerColor = null,
            showSparkle = false,
        )

        point >= 51 -> WordCardEffectStyle(
            label = "성장 중",
            borderColor = Color(0xFF5B8DEF),      // 파란색
            progressColor = Color(0xFF5B8DEF),
            containerColor = null,
            showSparkle = false,
        )

        point >= 26 -> WordCardEffectStyle(
            label = "연습 중",
            borderColor = Color(0xFFC0C0C0),      // 은색
            progressColor = Color(0xFFC0C0C0),
            containerColor = null,
            showSparkle = false,
        )

        else -> WordCardEffectStyle(
            label = "새 단어",
            borderColor = DarkOutline,
            progressColor = Color(0xFF8B8B8B),
            containerColor = null,
            showSparkle = false,
        )
    }
}

@Composable
fun WordCardEffectBadge(
    style: WordCardEffectStyle,
    modifier: Modifier = Modifier,
) {
    val badgeColor = if (style.showSparkle) {
        Color(0xFFFFB300)
    } else {
        style.borderColor.copy(alpha = 0.16f)
    }

    val textColor = if (style.showSparkle) {
        Color(0xFF2E1A00)
    } else {
        style.borderColor
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = badgeColor,
        contentColor = textColor,
        shadowElevation = if (style.showSparkle) 4.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            if (style.showSparkle) {
                Text("✦", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Text("MASTER", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Text("✦", style = MaterialTheme.typography.labelMedium)
            } else {
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
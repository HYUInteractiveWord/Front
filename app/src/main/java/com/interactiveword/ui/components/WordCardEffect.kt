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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.interactiveword.R
import com.interactiveword.ui.theme.DarkOutline

data class WordCardEffectStyle(
    val labelRes: Int,
    val borderColor: Color,
    val progressColor: Color,
    val containerColor: Color?,
    val showSparkle: Boolean,
)

fun wordCardEffectStyle(wordPoint: Int): WordCardEffectStyle {
    val point = wordPoint.coerceIn(0, 100)

    return when {
        point >= 100 -> WordCardEffectStyle(
            labelRes = R.string.word_rank_master,
            borderColor = Color(0xFFFFC107),      // 금색
            progressColor = Color(0xFFFFC107),    // 금색
            containerColor = null,                
            showSparkle = true,
        )

        point >= 80 -> WordCardEffectStyle(
            labelRes = R.string.word_rank_expert,
            borderColor = Color(0xFF9B7EDE),      // 보라색
            progressColor = Color(0xFF9B7EDE),
            containerColor = null,
            showSparkle = false,
        )

        point >= 60 -> WordCardEffectStyle(
            labelRes = R.string.word_rank_growing,
            borderColor = Color(0xFF5B8DEF),      // 파란색
            progressColor = Color(0xFF5B8DEF),
            containerColor = null,
            showSparkle = false,
        )

        point >= 40 -> WordCardEffectStyle(
            labelRes = R.string.word_rank_practicing,
            borderColor = Color(0xFFC0C0C0),      // 은색
            progressColor = Color(0xFFC0C0C0),
            containerColor = null,
            showSparkle = false,
        )

        point >= 20 -> WordCardEffectStyle(
            labelRes = R.string.word_rank_basic,
            borderColor = Color(0xFF4CAF50),      // 초록색
            progressColor = Color(0xFF4CAF50),
            containerColor = null,
            showSparkle = false,
        )

        else -> WordCardEffectStyle(
            labelRes = R.string.word_rank_new,
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
                Text(stringResource(style.labelRes), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Text("✦", style = MaterialTheme.typography.labelMedium)
            } else {
                Text(
                    text = stringResource(style.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

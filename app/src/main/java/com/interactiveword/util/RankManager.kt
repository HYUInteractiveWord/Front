package com.interactiveword.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.interactiveword.R

data class RankBand(
    val rank: String,
    val minXp: Int,
    val maxXpExclusive: Int?,
)

object RankManager {
    val rankBands = listOf(
        RankBand("Bronze",    0,     500),
        RankBand("Silver",    500,   1000),
        RankBand("Gold",      1000,  1500),
        RankBand("Sapphire",  1500,  2000),
        RankBand("Ruby",      2000,  2500),
        RankBand("Emerald",   2500,  3000),
        RankBand("Amethyst",  3000,  3500),
        RankBand("Pearl",     3500,  4000),
        RankBand("Obsidian",  4000,  4500),
        RankBand("Diamond",   4500,  null),
    )

    fun getCurrentBand(xp: Int): RankBand {
        return rankBands.firstOrNull { band ->
            xp >= band.minXp && (band.maxXpExclusive == null || xp < band.maxXpExclusive)
        } ?: rankBands.last()
    }

    fun getNextBand(xp: Int): RankBand? {
        val current = getCurrentBand(xp)
        val index = rankBands.indexOf(current)
        return rankBands.getOrNull(index + 1)
    }

    @Composable
    fun getRankLabel(rank: String?): String {
        val resId = getRankResId(rank) ?: return rank ?: ""
        return stringResource(resId)
    }

    fun getRankLabel(context: Context, rank: String?): String {
        val resId = getRankResId(rank) ?: return rank ?: ""
        return context.getString(resId)
    }

    private fun getRankResId(rank: String?): Int? {
        return when (rank?.lowercase()) {
            "bronze"   -> R.string.rank_bronze
            "silver"   -> R.string.rank_silver
            "gold"     -> R.string.rank_gold
            "platinum" -> R.string.rank_platinum
            "sapphire" -> R.string.rank_sapphire
            "ruby"     -> R.string.rank_ruby
            "emerald"  -> R.string.rank_emerald
            "amethyst" -> R.string.rank_amethyst
            "pearl"    -> R.string.rank_pearl
            "obsidian" -> R.string.rank_obsidian
            "diamond"  -> R.string.rank_diamond
            "master"   -> R.string.rank_master
            else -> null
        }
    }

    fun getRankColor(rank: String?): Color {
        return when (rank?.lowercase()) {
            "bronze"   -> Color(0xFFCD7F32) // Bronze
            "silver"   -> Color(0xFFC0C0C0) // Silver
            "gold"     -> Color(0xFFFFD700) // Gold
            "platinum" -> Color(0xFFE5E4E2) // Platinum
            "sapphire" -> Color(0xFF0F52BA) // Sapphire
            "ruby"     -> Color(0xFFE0115F) // Ruby
            "emerald"  -> Color(0xFF50C878) // Emerald
            "amethyst" -> Color(0xFF9966CC) // Amethyst
            "pearl"    -> Color(0xFFF0EAD6) // Pearl
            "obsidian" -> Color(0xFF343434) // Obsidian
            "diamond"  -> Color(0xFFB9F2FF) // Diamond
            "master"   -> Color(0xFFFF00FF) // Master (Magenta)
            else -> Color.White
        }
    }
}

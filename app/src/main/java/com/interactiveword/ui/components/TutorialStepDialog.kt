package com.interactiveword.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.interactiveword.ui.theme.BrandGreenLight

object TutorialPrefs {
    private const val PREF_NAME = "demo_tutorial_prefs"

    const val KEY_HOME = "home"
    const val KEY_DICTIONARY = "dictionary"
    const val KEY_SCAN = "scan"
    const val KEY_WORD_CARD = "word_card"
    const val KEY_MISSION = "mission"

    private fun userKey(userId: Int, key: String) = "u${userId}_$key"

    fun shouldShow(context: Context, userId: Int, key: String): Boolean {
        return !context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(userKey(userId, key), false)
    }

    fun markShown(context: Context, userId: Int, key: String) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(userKey(userId, key), true)
            .apply()
    }

    fun resetAllForUser(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val prefix = "u${userId}_"
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    fun resetAll(context: Context) {
        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

@Composable
fun TutorialStepDialog(
    @DrawableRes imageRes: Int,
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    contentScale = ContentScale.Fit,
                )

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreenLight,
                ),
            ) {
                Text(confirmText)
            }
        },
    )
}
package com.interactiveword.ui.screens.dictionary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.interactiveword.ui.components.TutorialPrefs
import com.interactiveword.ui.components.TutorialStepDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.interactiveword.R
import com.interactiveword.ui.navigation.Screen
import com.interactiveword.ui.theme.BrandGreenLight
import com.interactiveword.ui.theme.DarkMutedText
import com.interactiveword.ui.theme.DarkOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    navController: NavController,
    vm: DictionaryViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDictionaryTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userId) {
        uiState.userId?.let { userId ->
            if (TutorialPrefs.shouldShow(context, userId, TutorialPrefs.KEY_DICTIONARY)) {
                showDictionaryTutorial = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dictionary_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (showDictionaryTutorial) {
            TutorialStepDialog(
                imageRes = R.drawable.tutorial_scan_result_ru,
                title = stringResource(R.string.tutorial_dictionary_title),
                body = stringResource(R.string.tutorial_dictionary_body),
                confirmText = stringResource(R.string.action_confirm),
                onConfirm = {
                    uiState.userId?.let { userId ->
                        TutorialPrefs.markShown(context, userId, TutorialPrefs.KEY_DICTIONARY)
                    }
                    showDictionaryTutorial = false
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder = { Text(stringResource(R.string.dictionary_search_hint), color = DarkMutedText) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = DarkMutedText)
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = DarkMutedText)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { 
                    vm.searchNow()
                    keyboardController?.hide()
                }),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreenLight,
                    unfocusedBorderColor = DarkOutline,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { 
                    vm.searchNow()
                    keyboardController?.hide()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
            ) {
                Text(stringResource(R.string.action_search))
            }

            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreenLight)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.dictionary_search_failed, uiState.errorMessage.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                uiState.candidates.isNotEmpty() -> {
                    Text(
                        text = stringResource(R.string.dictionary_result_count, uiState.candidates.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = DarkMutedText,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.candidates) { result ->
                            val added = result.word in uiState.addedWords

                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, DarkOutline),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = result.word, style = MaterialTheme.typography.titleLarge)
                                            if (!result.pos.isNullOrBlank()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(R.string.dictionary_category, getPosString(result.pos)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = BrandGreenLight,
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                navController.navigate(
                                                    Screen.DictionaryVerify.createRoute(
                                                        word = result.word,
                                                        pos = result.pos,
                                                        definition = result.definition,
                                                    )
                                                )
                                            },
                                            enabled = !added && !uiState.isSlotFull,
                                            shape = MaterialTheme.shapes.large,
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenLight),
                                        ) {
                                            val btnText = when {
                                                added -> stringResource(R.string.dictionary_added)
                                                uiState.isSlotFull -> "슬롯 부족"
                                                else -> stringResource(R.string.action_add)
                                            }
                                            Text(btnText)
                                        }
                                    }

                                    val displayDefinition = result.definitionTranslated ?: result.definition
                                    if (!displayDefinition.isNullOrBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(text = displayDefinition, style = MaterialTheme.typography.bodyMedium, color = DarkMutedText)
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.query.isBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.dictionary_enter_word),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.dictionary_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getPosString(pos: String?): String {
    if (pos == null) return ""
    return when {
        pos.contains("대명사") -> stringResource(R.string.pos_pronoun)
        pos.contains("명사") -> stringResource(R.string.pos_noun)
        pos.contains("수사") -> stringResource(R.string.pos_numeral)
        pos.contains("동사") -> stringResource(R.string.pos_verb)
        pos.contains("형용사") -> stringResource(R.string.pos_adjective)
        pos.contains("관형사") -> stringResource(R.string.pos_determiner)
        pos.contains("부사") -> stringResource(R.string.pos_adverb)
        pos.contains("조사") -> stringResource(R.string.pos_particle)
        pos.contains("감탄사") -> stringResource(R.string.pos_interjection)
        else -> pos // 매칭 안 되면 원래 글자 그대로
    }
}

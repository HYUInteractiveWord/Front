package com.interactiveword.ui.screens.dictionary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사전") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
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
                placeholder = { Text("한국어 단어 검색...", color = DarkMutedText) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = DarkMutedText)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { vm.searchNow() },
                ),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreenLight,
                    unfocusedBorderColor = DarkOutline,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.searchNow() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreenLight,
                ),
            ) {
                Text("검색")
            }

            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BrandGreenLight)
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "검색 실패: ${uiState.errorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                uiState.candidates.isNotEmpty() -> {
                    Text(
                        text = "검색 결과 ${uiState.candidates.size}개",
                        style = MaterialTheme.typography.labelMedium,
                        color = DarkMutedText,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.candidates) { result ->
                            val added = result.word in uiState.addedWords

                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
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
                                            Text(
                                                text = result.word,
                                                style = MaterialTheme.typography.titleLarge,
                                            )

                                            if (!result.pos.isNullOrBlank()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "분류: ${result.pos}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = BrandGreenLight,
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { vm.addToCollection(result.word) },
                                            enabled = !added,
                                            shape = MaterialTheme.shapes.large,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BrandGreenLight,
                                            ),
                                        ) {
                                            Text(if (added) "추가됨" else "추가")
                                        }
                                    }

                                    if (!result.definition.isNullOrBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = result.definition,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DarkMutedText,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                uiState.query.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "검색할 단어를 입력하세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }
            }
        }
    }
}
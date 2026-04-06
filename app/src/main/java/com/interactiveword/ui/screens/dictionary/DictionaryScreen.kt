package com.interactiveword.ui.screens.dictionary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("사전 검색") },
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

            // 검색 바
            OutlinedTextField(
                value         = uiState.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder   = { Text("한국어 단어 검색...", color = DarkMutedText) },
                leadingIcon   = {
                    Icon(Icons.Filled.Search, null, tint = DarkMutedText)
                },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = MaterialTheme.shapes.extraLarge,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BrandGreenLight,
                    unfocusedBorderColor = DarkOutline,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // 결과
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandGreenLight)
                    }
                }
                uiState.result != null -> {
                    val result = uiState.result!!
                    Card(
                        shape  = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(result.word, style = MaterialTheme.typography.headlineMedium)
                                result.pos?.let {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = BrandGreenLight.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            it,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrandGreenLight,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                result.definition ?: "뜻을 찾을 수 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.addToCollection(result.word) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreenLight,
                                ),
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("단어장에 추가")
                            }
                        }
                    }
                }
                uiState.query.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "알고 싶은 한국어 단어를 검색해보세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkMutedText,
                        )
                    }
                }
            }
        }
    }
}

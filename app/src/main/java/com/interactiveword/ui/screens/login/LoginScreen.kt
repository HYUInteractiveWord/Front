package com.interactiveword.ui.screens.login

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.interactiveword.R
import com.interactiveword.ShareIntentHolder
import com.interactiveword.data.local.LanguageManager
import com.interactiveword.ui.navigation.Screen

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: LoginViewModel = viewModel(factory = AndroidViewModelFactory.getInstance(app))
    val uiState by vm.uiState.collectAsState()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var selectedLanguage by rememberSaveable { mutableStateOf(LanguageManager.getSavedLanguage(context)) }

    fun changeLanguage(lang: String) {
        selectedLanguage = lang
        LanguageManager.saveLanguage(context, lang)
        (context as? Activity)?.recreate()
    }

    LaunchedEffect(uiState) {
        val loggedIn = uiState as? LoginUiState.LoggedIn
        if (loggedIn != null) {
            if (loggedIn.languageChanged) {
                (context as? Activity)?.recreate()
                return@LaunchedEffect
            }

            val destination = if (ShareIntentHolder.pendingYoutubeUrl.value != null)
                Screen.Scan.route else Screen.Home.route
            navController.navigate(destination) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // 키보드 올라올 때 스크롤 가능하게 밀어줌
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()) // 스크롤 가능
        ) {
            Text(
                text = if (isRegisterMode) stringResource(R.string.register_title) else stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 언어 선택 라디오 버튼 영역
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.label_language),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedLanguage == "en",
                            onClick = { changeLanguage("en") },
                        )
                        Text("English", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedLanguage == "ru",
                            onClick = { changeLanguage("ru") },
                        )
                        Text("Русский", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.label_username), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (isRegisterMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.label_email), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.label_password), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isRegisterMode) vm.register(username, email, password, selectedLanguage)
                    else vm.login(username, password)
                },
                enabled = uiState !is LoginUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isRegisterMode) stringResource(R.string.action_register) else stringResource(R.string.action_login),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    text = if (isRegisterMode) stringResource(R.string.link_to_login) else stringResource(R.string.link_to_register),
                    textAlign = TextAlign.Center
                )
            }

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { vm.demoLogin(username, email, password, selectedLanguage) },
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    Text(
                        text = "DEMO Register",
                        color = Color(0xFFF97316), // 주황색
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
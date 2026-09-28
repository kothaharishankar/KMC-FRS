package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.Constants
import com.example.ui.components.AppButton
import com.example.ui.components.AppTextField
import com.example.ui.components.AuthScaffold
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    isAdminRoute: Boolean,
    onAdminToggle: (Boolean) -> Unit
) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val loginIdState by viewModel.loginId.collectAsState()
    val loginPasswordState by viewModel.loginPassword.collectAsState()
    
    // Persistent server-side or local API failure error banner (Module 2 Requirement)
    val errorState by viewModel.errorMessage.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

    // Local validation state for empty/short inputs
    var loginIdError by remember { mutableStateOf<String?>(null) }
    var loginPasswordError by remember { mutableStateOf<String?>(null) }

    AuthScaffold(
        onThemeToggle = { viewModel.toggleTheme() },
        isDarkMode = isDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAdminRoute) "HR Admin Portal Sign In" else "Employee Access Sign In",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = if (isAdminRoute) "Administrative system controls access" else "Please authenticate to verify attendance metrics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp)
            )

            // PERSISTENT red error banner for server/api failure (Module 2 Requirement)
            AnimatedVisibility(visible = errorState != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { viewModel.clearError() }, // Clear by clicking
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error detail",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorState ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Employee ID Input
            AppTextField(
                value = loginIdState,
                onValueChange = {
                    viewModel.loginId.value = it
                    loginIdError = null
                    // Clear persistent API error banner on change
                    if (errorState != null) viewModel.clearError()
                },
                label = if (isAdminRoute) "Admin Employee ID" else "Employee ID",
                leadingIcon = Icons.Default.Person,
                isError = loginIdError != null,
                errorMessage = loginIdError,
                testTag = "username_input"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            AppTextField(
                value = loginPasswordState,
                onValueChange = {
                    viewModel.loginPassword.value = it
                    loginPasswordError = null
                    // Clear persistent API error banner on change
                    if (errorState != null) viewModel.clearError()
                },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                isError = loginPasswordError != null,
                errorMessage = loginPasswordError,
                testTag = "password_input"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign In Action Button
            AppButton(
                text = if (isAdminRoute) "Admin Portal Sign In" else "Sign In",
                onClick = {
                    // Inline local validations
                    var hasError = false
                    if (loginIdState.trim().isEmpty()) {
                        loginIdError = "Employee ID cannot be empty."
                        hasError = true
                    }
                    if (loginPasswordState.isEmpty()) {
                        loginPasswordError = "Password cannot be empty."
                        hasError = true
                    } else if (loginPasswordState.length < 6) {
                        loginPasswordError = "Password must be at least 6 characters."
                        hasError = true
                    }

                    if (!hasError) {
                        viewModel.login(isAdminRoute = isAdminRoute)
                    }
                },
                isLoading = isAuthenticating,
                testTag = "submit_button"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action footer navigation links
            if (!isAdminRoute) {
                // Outlined Admin Login Toggle button (Module 2 requirement)
                OutlinedButton(
                    onClick = { 
                        viewModel.clearError()
                        onAdminToggle(true) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1A56DB)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Portal Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Admin back to employee login link (Module 2 requirement)
                Text(
                    text = "Back to Employee Login",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A56DB),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { 
                            viewModel.clearError()
                            onAdminToggle(false) 
                        }
                        .padding(12.dp)
                )
            }
        }
    }
}



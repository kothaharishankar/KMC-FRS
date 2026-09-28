package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.AuthStatus

object AppRouter {
    /**
     * Resolves and enforces the application's global navigation guards.
     * Keeps unauthenticated users on auth screens, redirects employees away from admin screens,
     * and redirects admins away from employee screens.
     */
    @Composable
    fun ResolveRedirects(viewModel: MainViewModel) {
        val authStatus by viewModel.authStatus.collectAsState()
        val isAdmin by viewModel.isAdmin.collectAsState()
        val currentScreen by viewModel.currentScreen.collectAsState()

        LaunchedEffect(authStatus, isAdmin, currentScreen) {
            when (authStatus) {
                AuthStatus.UNKNOWN -> {
                    // Stay on splash screen
                }
                AuthStatus.UNAUTHENTICATED -> {
                    // Redirect unauthenticated users to Login
                    if (currentScreen != Screen.Login) {
                        viewModel.navigateTo(Screen.Login)
                    }
                }
                AuthStatus.AUTHENTICATED -> {
                    if (isAdmin) {
                        // Admins cannot access login or employee screens
                        if (currentScreen is Screen.EmployeeHome || currentScreen == Screen.Login) {
                            viewModel.navigateTo(Screen.AdminHome(Screen.AdminTab.Dashboard))
                        }
                    } else {
                        // Employees cannot access login or admin screens
                        if (currentScreen is Screen.AdminHome || currentScreen == Screen.Login) {
                            viewModel.navigateTo(Screen.EmployeeHome(Screen.EmployeeTab.Home))
                        }
                    }
                }
            }
        }
    }

    /**
     * A beautiful, production-grade Material 3 Splash Screen displaying official KSCCL branding.
     */
    @Composable
    fun SplashScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Deep Slate Dark
                            Color(0xFF1E293B)
                        )
                    )
                )
                .testTag("splash_screen_root"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Branded Fingerprint / Face icon container
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Security Scanner Icon",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "KSCCL",
                    fontSize = 36.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "DIGITAL ATTENDANCE SYSTEM",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6),
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                CircularProgressIndicator(
                    color = Color(0xFF3B82F6),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Verifying session security...",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

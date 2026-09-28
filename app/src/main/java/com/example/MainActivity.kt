package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.navigation.AppRouter
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.AuthStatus
import com.example.ui.widgets.ErrorBoundary

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Native JVM Exception Handler (Equivalent to FlutterError.onError)
        // Intercepts all synchronous / main thread/ JVM-level uncaught exceptions and keeps the application running safely
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalException", "Intercepted uncaught exception in thread ${thread?.name}: ", throwable)
            try {
                com.example.core.FlutterErrorHandlingMapping.setupGlobalErrorHandlers()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    viewModel.showBanner("A system exception was intercepted: ${throwable.localizedMessage ?: "Unknown"}", false)
                    viewModel.showPopup(
                        title = "Global Error Caught",
                        message = "A background system exception was successfully caught: ${throwable.localizedMessage ?: "Unknown"}. The application was kept running safely.",
                        iconType = "ERROR"
                    )
                }
            } catch (ex: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        enableEdgeToEdge()
        
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            val authStatus by viewModel.authStatus.collectAsState()
            
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Last-resort Error Boundary safety net catching composition-level exceptions
                        ErrorBoundary(
                            onRetry = { 
                                viewModel.tryRestoreSession() 
                            }
                        ) {
                            if (authStatus == AuthStatus.UNKNOWN) {
                                AppRouter.SplashScreen()
                            } else {
                                // Dynamically resolve routing redirects and guards
                                AppRouter.ResolveRedirects(viewModel)
                                
                                // Render current screen
                                MainNavigationContent(viewModel)
                            }
                        }

                        // Top Floating Notifications Panel overlay
                        NotificationOverlay(viewModel)

                        // Center Modal Dialog Alert popup
                        AlertPopup(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    var isAdminLoginMode by remember { mutableStateOf(false) }

    when (val screen = currentScreen) {
        is Screen.Login -> {
            LoginScreen(
                viewModel = viewModel,
                isAdminRoute = isAdminLoginMode,
                onAdminToggle = { isAdminLoginMode = it }
            )
        }
        is Screen.EmployeeHome -> {
            val activeTab by viewModel.activeEmployeeTab.collectAsState()
            EmployeeScreenContainer(
                viewModel = viewModel,
                activeTab = activeTab
            )
        }
        is Screen.AdminHome -> {
            val activeTab by viewModel.activeAdminTab.collectAsState()
            AdminScreenContainer(
                viewModel = viewModel,
                activeTab = activeTab
            )
        }
    }
}

@Composable
fun NotificationOverlay(viewModel: MainViewModel) {
    val message by viewModel.snackbarMessage.collectAsState()
    val isSuccess by viewModel.isSuccessBanner.collectAsState()

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        val containerColor = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.dismissBanner() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.OfflinePin else Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = message ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.dismissBanner() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AlertPopup(viewModel: MainViewModel) {
    val message by viewModel.activePopupMessage.collectAsState()
    val title by viewModel.activePopupTitle.collectAsState()
    val iconType by viewModel.activePopupIconType.collectAsState()

    if (message != null) {
        val icon = when (iconType.uppercase()) {
            "SUCCESS" -> Icons.Default.OfflinePin
            "ERROR" -> Icons.Default.Error
            "WARNING" -> Icons.Default.Error
            else -> Icons.Default.Close
        }
        val iconColor = when (iconType.uppercase()) {
            "SUCCESS" -> Color(0xFF10B981)
            "ERROR" -> Color(0xFFEF4444)
            "WARNING" -> Color(0xFFF59E0B)
            else -> Color(0xFF1A56DB)
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissPopup() },
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = title ?: "Notification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = message ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissPopup() }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF1A56DB))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

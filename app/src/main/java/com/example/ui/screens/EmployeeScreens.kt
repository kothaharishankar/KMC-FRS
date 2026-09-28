package com.example.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.core.Constants
import com.example.model.AttendanceRecord
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.LocationStatusState
import com.example.ui.viewmodel.TodayAttendanceState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FrontCameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isDisposed = false

    DisposableEffect(lifecycleOwner) {
        onDispose {
            isDisposed = true
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            previewView.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) {}
                override fun onViewDetachedFromWindow(v: android.view.View) {
                    isDisposed = true
                    try {
                        if (cameraProviderFuture.isDone) {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            cameraProviderFuture.addListener({
                if (isDisposed) return@addListener
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                        try {
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                .build()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback to back camera
                            try {
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build()
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (backEx: Exception) {
                                backEx.printStackTrace()
                                // Ultimate fallback to default front or back
                                try {
                                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (ultEx: Exception) {
                                    ultEx.printStackTrace()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = modifier
    )
}

fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onImageCaptured: (ByteArray) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val executor = ContextCompat.getMainExecutor(context)
        val photoFile = File(context.cacheDir, "temp_face_capture.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        if (photoFile.exists()) {
                            val bytes = photoFile.readBytes()
                            onImageCaptured(bytes)
                            try {
                                photoFile.delete() // clean up
                            } catch (de: Exception) {
                                de.printStackTrace()
                            }
                        } else {
                            onError(Exception("Photo file does not exist"))
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    } catch (e: Exception) {
        onError(e)
    }
}

@Composable
fun EmployeeScreenContainer(
    viewModel: MainViewModel,
    activeTab: Screen.EmployeeTab
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        bottomBar = {
            EmployeeBottomBar(
                activeTab = activeTab,
                onTabSelected = { tab -> viewModel.setEmployeeTab(tab) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                Screen.EmployeeTab.Home -> EmployeeHomeScreen(viewModel)
                Screen.EmployeeTab.History -> EmployeeHistoryScreen(viewModel)
                Screen.EmployeeTab.Calendar -> EmployeeCalendarScreen(viewModel)
                Screen.EmployeeTab.Profile -> EmployeeProfileScreen(viewModel)
            }
        }
    }
}

@Composable
fun EmployeeBottomBar(
    activeTab: Screen.EmployeeTab,
    onTabSelected: (Screen.EmployeeTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        val items = listOf(
            Triple(Screen.EmployeeTab.Home, "Home", Icons.Default.Home),
            Triple(Screen.EmployeeTab.History, "History", Icons.Default.History),
            Triple(Screen.EmployeeTab.Calendar, "Calendar", Icons.Default.CalendarMonth),
            Triple(Screen.EmployeeTab.Profile, "Profile", Icons.Default.Person)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = activeTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF1A56DB) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1A56DB) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1A56DB).copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun EmployeeHeader(
    viewModel: MainViewModel,
    activeTab: Screen.EmployeeTab
) {
    val user by viewModel.currentUser.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Logo (Standalone, without inner texts)
        KakinadaSmartCityLogo(
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 6.dp),
            showText = false
        )

        // 2. "KSCCL" Header text
        Text(
            text = "KSCCL",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )

        // 3. "FACIAL ATTENDANCE SYSTEM" Subtitle text
        Text(
            text = "FACIAL ATTENDANCE SYSTEM",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 4. User details row (Avatar, Welcome/ID, Navigation links)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile with Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = getInitials(user?.name ?: "EM"),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Welcome, ${user?.name ?: "Employee"}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${user?.employeeId ?: ""}",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Navigation and Logout links
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Theme Toggle Icon
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Logout",
                    color = Color(0xFFEF4444),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clickable { viewModel.logout() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun EmployeeHomeScreen(viewModel: MainViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val todayState by viewModel.todayStatusState.collectAsState()
    val locStatus by viewModel.locationStatus.collectAsState()
    val isLocationDetecting by viewModel.isLocationDetecting.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val isVerifyingFace by viewModel.isVerifyingFace.collectAsState()
    val faceDetected by viewModel.faceDetected.collectAsState()
    val livenessScore by viewModel.livenessScore.collectAsState()
    val workplaceMode by viewModel.workplaceMode.collectAsState()
    val faceVerificationResult by viewModel.faceVerificationResult.collectAsState()
    val faceVerificationStep by viewModel.faceVerificationStep.collectAsState()
    val verificationError by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: hasCameraPermission
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        
        hasCameraPermission = cameraGranted
        hasLocationPermission = locationGranted
        
        if (locationGranted) {
            viewModel.detectLocation()
        } else {
            viewModel.locationStatus.value = LocationStatusState.DENIED
            android.widget.Toast.makeText(context, "Location permission is required for attendance marking!", android.widget.Toast.LENGTH_LONG).show()
        }
        
        if (!cameraGranted) {
            android.widget.Toast.makeText(context, "Camera permission is required for face verification!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Trigger location auto-detection on launch and check/request permissions
    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            if (locStatus == LocationStatusState.DETECTING) {
                viewModel.detectLocation()
            }
        }
    }

    // Pulsing scan lines animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_progress"
    )

    when (todayState) {
        is TodayAttendanceState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF1A56DB))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading Today's Attendance Status...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        is TodayAttendanceState.Error -> {
            val errorMsg = (todayState as TodayAttendanceState.Error).message
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Cloud Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Connection Failed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMsg,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { user?.let { viewModel.loadTodayStatus(it.employeeId) } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry Connection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        else -> {
            val isAlreadyMarked = todayState is TodayAttendanceState.Marked
            val todayRecord = (todayState as? TodayAttendanceState.Marked)?.record

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Centered Top Header with Logo, Headings and profile details with text links
                EmployeeHeader(viewModel = viewModel, activeTab = Screen.EmployeeTab.Home)

                Spacer(modifier = Modifier.height(8.dp))

                // Attendance State Card (Pending or Marked)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isAlreadyMarked) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAlreadyMarked) Color(0xFF16A34A) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAlreadyMarked) "Attendance Marked" else "Attendance Pending",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlreadyMarked) Color(0xFF065F46) else Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isAlreadyMarked) "You have marked attendance for today." else "You have not marked attendance for today yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        if (!isAlreadyMarked) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "FACIAL BIOMETRIC VERIFICATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Grey bordered viewfinder placeholder with real camera preview support
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color(0xFF64748B), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCameraPermission) {
                                    FrontCameraPreview(
                                        imageCapture = imageCapture,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val scanY = size.height * scanProgress
                                    if (isVerifyingFace) {
                                        drawLine(
                                            color = Color(0xFF60A5FA),
                                            start = Offset(0f, scanY),
                                            end = Offset(size.width, scanY),
                                            strokeWidth = 4f
                                        )
                                    }
                                }

                                if (isVerifyingFace) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .fillMaxSize()
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF60A5FA), modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = faceVerificationStep.takeIf { it.isNotEmpty() } ?: (if (livenessScore > 0.9) "Verifying Credentials..." else "Analyzing eye-blink liveness..."),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                } else if (!hasCameraPermission) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Camera Icon",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Access Required",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Grant Camera", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Camera Icon",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Camera Preview Ready",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Position your face and tap Mark Attendance",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Location detection Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (locStatus) {
                                            LocationStatusState.DETECTING -> Color(0xFFFEF3C7)
                                            LocationStatusState.GRANTED -> Color(0xFFE0F2FE)
                                            else -> Color(0xFFFEE2E2)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when (locStatus) {
                                            LocationStatusState.DETECTING -> Color(0xFFFCD34D)
                                            LocationStatusState.GRANTED -> Color(0xFFBAE6FD)
                                            else -> Color(0xFFFCA5A5)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.detectLocation() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (locStatus == LocationStatusState.DETECTING) {
                                        CircularProgressIndicator(
                                            color = Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Detecting location…",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    } else if (locStatus == LocationStatusState.GRANTED) {
                                        Text(
                                            text = "📍 Location detected (${locationName})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = "Location access denied — tap to retry",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (faceVerificationResult == "Failed" && !verificationError.isNullOrEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Verification Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Verification Failed",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = verificationError ?: "Facial match or biometric checks failed.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                viewModel.errorMessage.value = null
                                                viewModel.faceVerificationResult.value = null 
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // "Mark Attendance" Action button
                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        takePhoto(
                                            context = context,
                                            imageCapture = imageCapture,
                                            onImageCaptured = { bytes ->
                                                viewModel.startFaceVerification(bytes)
                                            },
                                            onError = { _ ->
                                                viewModel.startFaceVerification(null)
                                            }
                                        )
                                    } else {
                                        viewModel.startFaceVerification(null)
                                    }
                                },
                                enabled = !isVerifyingFace && locStatus == LocationStatusState.GRANTED,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = if (isVerifyingFace) MaterialTheme.colorScheme.primary.copy(alpha = 0.62f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            ) {
                                if (isVerifyingFace) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Verifying Identity...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "Mark Attendance",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            // Already Marked Success Box
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                                border = BorderStroke(1.5.dp, Color(0xFF059669)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("attendance_success_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success Icon",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Attendance Marked Successfully ✓",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    
                                    HorizontalDivider(color = Color(0xFF059669).copy(alpha = 0.15f))
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Time Icon",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Current Time: ${todayRecord?.getFormattedTime() ?: "--:--"}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Date Icon",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Current Date: ${todayRecord?.date ?: "--/--/----"}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Status Icon",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Present Status: ${todayRecord?.status ?: "PRESENT"}" + 
                                                        if (todayRecord?.status == "LATE") " (${todayRecord.getFormattedLateDuration()})" else "",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TODAY'S DETAILS Section matching the screenshot
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "TODAY'S DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "DATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val todayFormattedDate = remember {
                                    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                                    }.format(Date())
                                }
                                Text(
                                    text = todayFormattedDate,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Workplace Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "WORKPLACE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAlreadyMarked) todayRecord?.locationName ?: "Remote / Office" else workplaceMode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    if (!isAlreadyMarked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Submit on-time to be marked On-Time.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (todayRecord?.status == "LATE") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = todayRecord.getFormattedLateDuration().ifEmpty { "LATE" },
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeHistoryScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val loadState by viewModel.historyLoadState.collectAsState()

    // Trigger load on entry if it hasn't loaded yet or we are at loading
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            viewModel.loadHistory(user.employeeId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        EmployeeHeader(viewModel = viewModel, activeTab = Screen.EmployeeTab.History)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "My Attendance History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Review all your past verification logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = {
                    currentUser?.let { user ->
                        viewModel.loadHistory(user.employeeId)
                    }
                },
                modifier = Modifier.testTag("refresh_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = loadState) {
            is MainViewModel.HistoryLoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Retrieving logs from secure servers...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            is MainViewModel.HistoryLoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to load logs",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                currentUser?.let { user ->
                                    viewModel.loadHistory(user.employeeId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is MainViewModel.HistoryLoadState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No history records found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your marked logs will appear here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            is MainViewModel.HistoryLoadState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("history_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList) { log ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("history_item_${log.date}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatDateString(log.date),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = log.getFormattedTime(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = log.locationName,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusPill(status = log.status, lateDuration = log.getFormattedLateDuration().replace("late", "").trim())
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeCalendarScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val monthSummary by viewModel.monthSummary.collectAsState()
    val calendarLoadState by viewModel.calendarLoadState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    var selectedRecordDetail by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Trigger loading on entrance
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            viewModel.loadMonthSummary(user.employeeId, selectedMonth, selectedYear)
            viewModel.loadHistory(user.employeeId) // ensure history is loaded for details lookup
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val headerTitle = "${monthNames.getOrElse(selectedMonth) { "" }} $selectedYear"

    // Calculate calendar days
    val calendar = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Offset: Monday = 0, Tuesday = 1, ... Sunday = 6
    val offset = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    // Determine current/today day
    val todayCal = Calendar.getInstance()
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        EmployeeHeader(viewModel = viewModel, activeTab = Screen.EmployeeTab.Calendar)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val state = calendarLoadState) {
                is MainViewModel.CalendarLoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aggregating monthly overview...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                is MainViewModel.CalendarLoadState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    currentUser?.let { user ->
                                        viewModel.loadMonthSummary(user.employeeId, selectedMonth, selectedYear)
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is MainViewModel.CalendarLoadState.Loaded -> {
                    val summary = monthSummary
                    if (summary != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "My Calendar View",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Your calendar heatmap overview for verification states",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Month Selection Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.changeEmployeeMonth(-1) },
                                    modifier = Modifier.testTag("prev_month_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = headerTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("month_header_title")
                                )

                                IconButton(
                                    onClick = { viewModel.changeEmployeeMonth(1) },
                                    modifier = Modifier.testTag("next_month_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Legend Row
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "${summary.present} Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "${summary.absent} Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "${summary.holiday} Holiday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Calendar Grid Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().testTag("calendar_grid_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Days of week Headers
                                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        daysOfWeek.forEach { day ->
                                            Text(
                                                text = day,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Grid drawing
                                    val requiredCells = offset + maxDays
                                    val rows = if (requiredCells > 35) 6 else 5

                                    for (row in 0 until rows) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            for (col in 0 until 7) {
                                                val index = row * 7 + col
                                                val dayNum = index - offset + 1

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (index in offset until requiredCells) {
                                                        val status = summary.days[dayNum] ?: "none"
                                                        val isToday = dayNum == todayDay && selectedMonth == todayMonth && selectedYear == todayYear

                                                        val bgColor = when (status) {
                                                            "present" -> Color(0xFFE6FDF4)
                                                            "absent" -> Color(0xFFFEE2E2)
                                                            "holiday" -> Color(0xFFFEF3C7)
                                                            else -> Color.Transparent
                                                        }

                                                        val textColor = when (status) {
                                                            "present" -> Color(0xFF047857)
                                                            "absent" -> Color(0xFFB91C1C)
                                                            "holiday" -> Color(0xFFB45309)
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(CircleShape)
                                                                .background(bgColor)
                                                                .border(
                                                                    width = if (isToday) 2.dp else 0.dp,
                                                                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                    shape = CircleShape
                                                                )
                                                                .clickable {
                                                                    val dateString = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, dayNum)
                                                                    val existingLog = historyList.find { it.date == dateString }
                                                                    if (existingLog != null) {
                                                                        selectedRecordDetail = existingLog
                                                                    } else {
                                                                        val statusUpper = status.uppercase()
                                                                        if (statusUpper != "NONE") {
                                                                            selectedRecordDetail = AttendanceRecord(
                                                                                id = "TEMP_$dayNum",
                                                                                employeeId = currentUser?.employeeId ?: "",
                                                                                employeeName = currentUser?.name ?: "",
                                                                                date = dateString,
                                                                                time = "--:--",
                                                                                status = statusUpper,
                                                                                latitude = 0.0,
                                                                                longitude = 0.0,
                                                                                locationName = if (statusUpper == "HOLIDAY") "Official Holiday / Sunday Off" else "No attendance logged"
                                                                            )
                                                                        }
                                                                    }
                                                                    if (selectedRecordDetail != null) {
                                                                        showBottomSheet = true
                                                                    }
                                                                }
                                                                .testTag("day_cell_$dayNum"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = dayNum.toString(),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = textColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // Modal Bottom Sheet detailing day attendance stats
    if (showBottomSheet && selectedRecordDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 16.dp
        ) {
            val record = selectedRecordDetail!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("calendar_detail_sheet")
            ) {
                Text(
                    text = "Verification Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatDateString(record.date),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Timestamp: ${record.getFormattedTime()}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            StatusPill(status = record.status, lateDuration = record.getFormattedLateDuration().replace("late", "").trim())
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Location",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${record.locationName} (${String.format(java.util.Locale.getDefault(), "%.6f, %.6f", record.latitude, record.longitude)})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Verification Security Mode",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Google AI Studio Facial Landmarks Match (Liveness ${String.format(java.util.Locale.getDefault(), "%.1f%%", record.faceConfidence * 100)}✓)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Details")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        }
    }
}

@Composable
fun EmployeeProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isChangingPassword by viewModel.isChangingPassword.collectAsState()
    val changePasswordError by viewModel.changePasswordError.collectAsState()
    val changePasswordSuccess by viewModel.changePasswordSuccess.collectAsState()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Initials Avatar
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFF1A56DB).copy(alpha = 0.12f),
            border = BorderStroke(2.dp, Color(0xFF1A56DB))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = getInitials(currentUser?.name ?: "EM"),
                    color = Color(0xFF1A56DB),
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = currentUser?.name ?: "John Doe",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "ID: ${currentUser?.employeeId ?: ""}",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Info List Card
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ProfileInfoRow(label = "Employee ID", value = currentUser?.employeeId ?: "")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow(label = "Gov / Work Email", value = currentUser?.email?.takeIf { it.isNotEmpty() } ?: "employee@ksccl.gov.in")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow(label = "Mobile Number", value = currentUser?.phone?.takeIf { it.isNotEmpty() } ?: "9876543210")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow(label = "Date Joined", value = currentUser?.joinedDate?.takeIf { it.isNotEmpty() } ?: "2024-01-15")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileInfoRow(label = "Role Profile", value = currentUser?.role ?: "EMPLOYEE")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Change Password Section
        Text(
            text = "Security & Password",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Update Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Change your profile password by validating your previous password first.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Previous Password Input
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Previous Password", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_old_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // New Password Input
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_new_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm New Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (changePasswordError != null) {
                    Text(
                        text = changePasswordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            viewModel.showBanner("All password fields are required.", false)
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            viewModel.showBanner("New passwords do not match.", false)
                            return@Button
                        }
                        viewModel.changePassword(oldPassword, newPassword) {
                            oldPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    },
                    enabled = !isChangingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_change_password_button")
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Update Password", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme Switch settings Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            val isDark by viewModel.isDarkMode.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleTheme() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Theme",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDark) "Dark Theme Active" else "Bright Theme Active",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Switch(
                    checked = isDark,
                    onCheckedChange = { viewModel.toggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.performLogout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("profile_logout_button")
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun StatusPill(status: String, lateDuration: String? = null) {
    val bgColor = when (status.uppercase()) {
        "PRESENT" -> Color(0xFFD1FAE5)
        "LATE" -> Color(0xFFFEF3C7)
        "ABSENT" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF3F4F6) // holiday / pending
    }

    val textColor = when (status.uppercase()) {
        "PRESENT" -> Color(0xFF065F46)
        "LATE" -> Color(0xFF92400E)
        "ABSENT" -> Color(0xFF991B1B)
        else -> Color(0xFF374151)
    }

    val label = when (status.uppercase()) {
        "PRESENT" -> "PRESENT"
        "LATE" -> if (lateDuration != null) "LATE BY $lateDuration" else "LATE"
        "ABSENT" -> "ABSENT"
        else -> "PENDING"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// Helpers
private fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    if (parts.isEmpty()) return "EM"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts[0].take(1) + parts[1].take(1)).uppercase()
}

private fun formatDateString(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun KakinadaSmartCityLogo(
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF0F172A),
    subtitleColor: Color = Color(0xFF64748B),
    showText: Boolean = true
) {
    val context = LocalContext.current
    val imageBitmap = remember {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val isSmall = maxWidth < 80.dp
        val paddingVal = if (isSmall) 2.dp else 4.dp
        val cornerRadius = if (isSmall) 8.dp else 16.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(cornerRadius))
                .padding(paddingVal),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Kakinada Smart City Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

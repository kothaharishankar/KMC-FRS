package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.Constants
import com.example.model.AttendanceRecord
import com.example.model.User
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminScreenContainer(
    viewModel: MainViewModel,
    activeTab: Screen.AdminTab
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Title, Subtitle, and Pill Badge
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Analytics Portal",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HR Administrator",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Right: Theme Toggle, "Change Password" & "Logout" links
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDark by viewModel.isDarkMode.collectAsState()
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Change Password",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showChangePasswordDialog = true }
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Logout",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable { viewModel.performLogout() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(72.dp)
            ) {
                val adminTabs = listOf(
                    Triple(Screen.AdminTab.Dashboard, "Dashboard", Icons.Default.Dashboard),
                    Triple(Screen.AdminTab.Users, "Users", Icons.Default.People),
                    Triple(Screen.AdminTab.Attendance, "Attendance", Icons.Default.Assignment),
                    Triple(Screen.AdminTab.Calendar, "Calendar", Icons.Default.CalendarMonth),
                    Triple(Screen.AdminTab.Reports, "Reports", Icons.Default.Assessment)
                )

                adminTabs.forEach { (tab, label, icon) ->
                    val isSelected = activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setAdminTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF16A34A).copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                Screen.AdminTab.Dashboard -> AdminDashboardScreen(viewModel)
                Screen.AdminTab.Users -> AdminUsersScreen(viewModel)
                Screen.AdminTab.Attendance -> AdminAttendanceLogScreen(viewModel)
                Screen.AdminTab.Calendar -> AdminCalendarScreen(viewModel)
                Screen.AdminTab.Reports -> AdminReportsScreen(viewModel)
            }
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }
}

@Composable
fun ChangePasswordDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                                viewModel.showBanner("Password fields cannot be empty", false)
                            } else if (newPassword != confirmPassword) {
                                viewModel.showBanner("Passwords do not match", false)
                            } else if (newPassword.length < 6) {
                                viewModel.showBanner("Password must be at least 6 characters", false)
                            } else {
                                viewModel.showBanner("Password changed successfully ✓", true)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: MainViewModel) {
    val employees by viewModel.employees.collectAsState()
    val loadState by viewModel.employeesLoadState.collectAsState()
    val employeePhotos by viewModel.employeePhotos.collectAsState()
    var showFormSheet by remember { mutableStateOf(false) }
    var selectedEmployeeForEdit by remember { mutableStateOf<User?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<User?>(null) }
    var employeeForPasswordChange by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadEmployees()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title and "+ Add Employee" button top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Employee Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = {
                        selectedEmployeeForEdit = null
                        showFormSheet = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_employee_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Employee", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = loadState) {
                is MainViewModel.EmployeesLoadState.Loading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is MainViewModel.EmployeesLoadState.Error -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadEmployees() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is MainViewModel.EmployeesLoadState.Empty -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No employees yet — add your first one",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is MainViewModel.EmployeesLoadState.Loaded -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(employees) { employee ->
                            val photos = employeePhotos.filter { it.employeeId == employee.employeeId }
                            EmployeeCard(
                                employee = employee,
                                photos = photos,
                                onEdit = {
                                    selectedEmployeeForEdit = employee
                                    showFormSheet = true
                                },
                                onDelete = {
                                    showDeleteConfirmDialog = employee
                                },
                                onChangePassword = {
                                    employeeForPasswordChange = employee
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sheet (Dialog modal layout or ModalBottomSheet) for form
        if (showFormSheet) {
            EmployeeFormSheet(
                viewModel = viewModel,
                employee = selectedEmployeeForEdit,
                onDismiss = { showFormSheet = false }
            )
        }

        // Delete Confirm Dialog
        showDeleteConfirmDialog?.let { emp ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text(text = "Delete Employee") },
                text = { Text(text = "Remove ${emp.name}? This can't be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEmployee(emp.employeeId)
                            showDeleteConfirmDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Admin Override Employee Password Dialog
        employeeForPasswordChange?.let { emp ->
            AdminChangeEmployeePasswordDialog(
                viewModel = viewModel,
                employee = emp,
                onDismiss = { employeeForPasswordChange = null }
            )
        }
    }
}



@Composable
fun EmployeeCard(
    employee: User,
    photos: List<com.example.data.local.EmployeePhoto> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangePassword: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("employee_card_${employee.employeeId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: initials and name/ID info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = getInitials(employee.name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = employee.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: ${employee.employeeId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Right side: Action icon buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onChangePassword,
                        modifier = Modifier.testTag("change_password_employee_${employee.employeeId}").size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Override Password",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_employee_${employee.employeeId}").size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Employee",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_employee_${employee.employeeId}").size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Employee",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(12.dp))

            // Contact and meta info
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = employee.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = employee.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Joined: ${employee.joinedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


            }

            // Captured photos from Room database gallery
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "CAPTURED PHOTOS FROM DATABASE (${photos.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos) { photo ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            Base64Image(
                                base64Str = photo.photoBase64,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = photo.time,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = photo.date,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminChangeEmployeePasswordDialog(
    viewModel: MainViewModel,
    employee: User,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val isAdminChangingPassword by viewModel.isAdminChangingPassword.collectAsState()
    val adminChangePasswordError by viewModel.adminChangePasswordError.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Password for ${employee.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "As an HR Administrator, you can override the password for Employee ID: ${employee.employeeId}.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_override_new_password")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_override_confirm_password")
                )

                if (adminChangePasswordError != null) {
                    Text(
                        text = adminChangePasswordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        viewModel.showBanner("All password fields are required.", false)
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        viewModel.showBanner("Passwords do not match.", false)
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        viewModel.showBanner("Password must be at least 6 characters.", false)
                        return@Button
                    }
                    viewModel.adminChangeEmployeePassword(employee.employeeId, newPassword) {
                        onDismiss()
                    }
                },
                enabled = !isAdminChangingPassword,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isAdminChangingPassword) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                } else {
                    Text("Save Password", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", fontSize = 13.sp)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormSheet(
    viewModel: MainViewModel,
    employee: User?, // If null, we are in ADD mode; if not null, we are in EDIT mode
    onDismiss: () -> Unit
) {
    val employeesList by viewModel.employees.collectAsState()
    val isEditMode = employee != null
    val isSubmitting by viewModel.isSubmittingEmployee.collectAsState()
    val errorMsg by viewModel.employeeErrorMessage.collectAsState()

    var employeeId by remember { mutableStateOf(employee?.employeeId ?: "") }
    
    LaunchedEffect(employee, employeesList) {
        if (employee != null) {
            employeeId = employee.employeeId
        } else {
            val prefix = "CCC"
            val cccIds = employeesList
                .map { it.employeeId }
                .filter { it.startsWith(prefix, ignoreCase = true) }
                .mapNotNull { id ->
                    val numericPart = id.substring(prefix.length).filter { it.isDigit() }
                    numericPart.toIntOrNull()
                }
            val nextNumber = if (cccIds.isNotEmpty()) cccIds.maxOrNull()!! + 1 else 1
            employeeId = String.format("%s%03d", prefix, nextNumber)
        }
    }

    var name by remember { mutableStateOf(employee?.name ?: "") }
    var email by remember { mutableStateOf(employee?.email ?: "") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var department by remember { mutableStateOf(employee?.department ?: "Technical Support") }
    var designation by remember { mutableStateOf(employee?.designation ?: "Technical Executive") }
    var status by remember { mutableStateOf(employee?.status ?: "ACTIVE") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Validation error states
    var idError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    val phoneRegex = "^[0-9]{10}$".toRegex()

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Sheet header
                Text(
                    text = if (isEditMode) "Edit Employee Details" else "Add New Employee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Error message surfaced inline
                errorMsg?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Employee ID Field (Removed input field for Add mode, auto-generated sequentially)
                if (isEditMode) {
                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = {},
                        label = { Text("Employee ID") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().testTag("form_employee_id"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Auto-Generated Employee ID",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = employeeId.ifEmpty { "Generating..." },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Full Name") },
                    enabled = !isSubmitting,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().testTag("form_name"),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("e.g. Ramesh Kumar") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null
                    },
                    label = { Text("Email Address") },
                    enabled = !isSubmitting,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().testTag("form_email"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    placeholder = { Text("e.g. name@ccc.edu") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            phone = it
                            phoneError = null
                        }
                    },
                    label = { Text("Phone Number") },
                    enabled = !isSubmitting,
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth().testTag("form_phone"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text("10-digit mobile number") }
                )

                Spacer(modifier = Modifier.height(12.dp))




                // Password fields (Only shown in ADD mode)
                if (!isEditMode) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        enabled = !isSubmitting,
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("form_password"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("At least 6 characters") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        label = { Text("Confirm Password") },
                        enabled = !isSubmitting,
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth().testTag("form_confirm_password"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("Re-enter password") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            var hasError = false
                            
                            // Validate ID
                            if (employeeId.trim().isEmpty()) {
                                idError = "Employee ID is required"
                                hasError = true
                            } else if (employeeId.trim().length < 3) {
                                idError = "ID is too short"
                                hasError = true
                            }

                            // Validate Name
                            if (name.trim().isEmpty()) {
                                nameError = "Name is required"
                                hasError = true
                            }

                            // Validate Email
                            if (email.trim().isEmpty()) {
                                emailError = "Email is required"
                                hasError = true
                            } else if (!emailRegex.matches(email.trim())) {
                                emailError = "Enter a valid email address"
                                hasError = true
                            }

                            // Validate Phone
                            if (phone.trim().isEmpty()) {
                                phoneError = "Phone is required"
                                hasError = true
                            } else if (!phoneRegex.matches(phone.trim())) {
                                phoneError = "Enter a valid 10-digit number"
                                hasError = true
                            }

                            // Validate Passwords in ADD mode
                            if (!isEditMode) {
                                if (password.isEmpty()) {
                                    passwordError = "Password is required"
                                    hasError = true
                                } else if (password.length < 6) {
                                    passwordError = "Password must be at least 6 characters"
                                    hasError = true
                                }

                                if (confirmPassword.isEmpty()) {
                                    confirmPasswordError = "Confirm password is required"
                                    hasError = true
                                } else if (password != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match"
                                    hasError = true
                                }
                            }

                            if (!hasError) {
                                val user = User(
                                    employeeId = employeeId.trim().uppercase(),
                                    name = name.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    role = "EMPLOYEE",
                                    joinedDate = employee?.joinedDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    department = department.trim(),
                                    designation = designation.trim(),
                                    status = status.trim(),
                                    faceRegistered = employee?.faceRegistered ?: true
                                )

                                if (isEditMode) {
                                    viewModel.updateEmployee(
                                        empId = employee!!.employeeId,
                                        user = user,
                                        onSuccess = onDismiss
                                    )
                                } else {
                                    viewModel.addEmployee(
                                        user = user,
                                        password = password,
                                        onSuccess = onDismiss
                                    )
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("form_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = if (isEditMode) "Save" else "Add")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceLogScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val attendanceList by viewModel.adminAttendanceList.collectAsState()
    val loadState by viewModel.adminAttendanceLoadState.collectAsState()
    
    val searchQueryState = viewModel.logSearchQuery.collectAsState()
    var searchInput by remember { mutableStateOf(searchQueryState.value) }

    // Synchronize local search input with ViewModel's state on tab switch
    LaunchedEffect(searchQueryState.value) {
        if (searchInput != searchQueryState.value) {
            searchInput = searchQueryState.value
        }
    }

    // Debounce typing logic
    LaunchedEffect(searchInput) {
        viewModel.searchAdminAttendance(searchInput)
    }

    val isExportingCsv by viewModel.isExportingCsv.collectAsState()
    val isExportingExcel by viewModel.isExportingExcel.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title block
        Text(
            text = "Attendance Log",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track city personnel check-ins and generate official reports",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search + Export block
        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            placeholder = { Text("Search by name, ID, date, status...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = { searchInput = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Export Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Export:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // CSV Button
            ExportButton(
                label = "CSV",
                icon = Icons.Default.Description,
                isExporting = isExportingCsv,
                onClick = { viewModel.exportAttendance(context, MainViewModel.ExportFormat.CSV) }
            )

            // Excel Button
            ExportButton(
                label = "Excel",
                icon = Icons.Default.Assessment,
                isExporting = isExportingExcel,
                onClick = { viewModel.exportAttendance(context, MainViewModel.ExportFormat.EXCEL) }
            )

            // PDF Button
            ExportButton(
                label = "PDF",
                icon = Icons.Default.PictureAsPdf,
                isExporting = isExportingPdf,
                onClick = { viewModel.exportAttendance(context, MainViewModel.ExportFormat.PDF) }
            )
        }

        // List States
        when (val state = loadState) {
            is MainViewModel.AdminAttendanceLoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF1A56DB))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading attendance logs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            is MainViewModel.AdminAttendanceLoadState.Empty -> {
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
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No attendance records match your search",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try refining your search terms or date format (YYYY-MM-DD)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is MainViewModel.AdminAttendanceLoadState.Error -> {
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
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load logs",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadAllAttendance(searchInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB))
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is MainViewModel.AdminAttendanceLoadState.Success -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(attendanceList) { r ->
                        AttendanceLogCard(record = r)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExporting: Boolean,
    onClick: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = {
            if (isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        enabled = !isExporting,
        shape = RoundedCornerShape(8.dp),
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun Base64Image(base64Str: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64Str) {
        try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Photo",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AttendanceLogCard(record: AttendanceRecord) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left avatar fallback
                EmployeeAvatar(name = record.employeeName)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Middle text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.employeeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ID: ${record.employeeId}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                // Right status pill
                val lateDurationStr = record.getFormattedLateDuration().ifEmpty { record.lateDuration }
                AdminStatusPill(status = record.status, lateDuration = lateDurationStr)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date & Time Column
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${record.date} • ${record.getFormattedTime()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Location Column
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.locationName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!record.photoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verified Face Bio-ID Match (${String.format(java.util.Locale.getDefault(), "%.1f%%", record.faceConfidence * 100)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS Coordinates: ${String.format(java.util.Locale.getDefault(), "%.6f, %.6f", record.latitude, record.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp, 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Base64Image(
                        base64Str = record.photoUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeAvatar(name: String) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFF1A56DB).copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color(0xFF1A56DB),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AdminStatusPill(status: String, lateDuration: String?) {
    val (bgColor, textColor, text) = when (status.uppercase()) {
        "PRESENT" -> Triple(Color(0xFFE8F5E9), Color(0xFF16A34A), "PRESENT")
        "LATE" -> {
            val label = if (!lateDuration.isNullOrBlank()) lateDuration.uppercase() else "LATE"
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), label)
        }
        "ABSENT" -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), "ABSENT")
        "HOLIDAY" -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), "HOLIDAY")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
}

@Composable
fun AdminDashboardScreen(viewModel: MainViewModel) {
    val stats by viewModel.adminDashboardStats.collectAsState()
    val loadState by viewModel.adminDashboardLoadState.collectAsState()

    // Trigger loading stats on entry
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    when (val state = loadState) {
        is MainViewModel.AdminDashboardLoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading dashboard statistics...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
        is MainViewModel.AdminDashboardLoadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadStats() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is MainViewModel.AdminDashboardLoadState.Loaded -> {
            val dashboardStats = stats
            if (dashboardStats == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No statistics found.")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Row with Pull to Refresh Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CCC Analytics Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.testTag("refresh_stats_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh statistics",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Responsive Grid (2x2 Stat Cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Employees",
                            value = dashboardStats.totalEmployees.toString(),
                            icon = Icons.Outlined.People,
                            color = Color(0xFF1A56DB),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Present Today",
                            value = dashboardStats.presentToday.toString(),
                            icon = Icons.Outlined.CheckCircle,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Absent Today",
                            value = dashboardStats.absentToday.toString(),
                            icon = Icons.Outlined.Cancel,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Attendance Rate",
                            value = "${String.format(Locale.getDefault(), "%.1f", dashboardStats.attendanceRatePercent)}%",
                            icon = Icons.Outlined.TrendingUp,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Donut Chart Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Today's Attendance Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // Donut Chart Vector Canvas
                                Box(
                                    modifier = Modifier.size(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val total = dashboardStats.totalEmployees.toFloat().coerceAtLeast(1f)
                                    val presentPct = dashboardStats.presentToday.toFloat() / total
                                    val absentPct = dashboardStats.absentToday.toFloat() / total

                                    Canvas(modifier = Modifier.size(100.dp)) {
                                        // Present arc starting from 270 degrees
                                        drawArc(
                                            color = Color(0xFF16A34A),
                                            startAngle = -90f,
                                            sweepAngle = presentPct * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                                        )
                                        // Absent arc
                                        drawArc(
                                            color = Color(0xFFEF4444),
                                            startAngle = -90f + (presentPct * 360f),
                                            sweepAngle = absentPct * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", dashboardStats.attendanceRatePercent)}%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A56DB)
                                        )
                                        Text(
                                            text = "Rate",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                // Legend description list
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LegendRow(color = Color(0xFF16A34A), text = "Present (${dashboardStats.presentToday})")
                                    LegendRow(color = Color(0xFFEF4444), text = "Absent (${dashboardStats.absentToday})")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Feed: Today's Check-ins
                    Text(
                        text = "Today's Check-ins",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (dashboardStats.todayCheckins.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No check-ins yet today.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            dashboardStats.todayCheckins.forEach { record ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth().testTag("checkin_card_${record.empId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF1A56DB).copy(alpha = 0.1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = getInitials(record.name),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1A56DB)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = record.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${record.empId} · ${record.getFormattedTime()}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF16A34A).copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = record.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Quick Tools",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setAdminTab(Screen.AdminTab.Calendar) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF1A56DB),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Monthly Calendar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View employee shifts & attendance grids",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setAdminTab(Screen.AdminTab.Reports) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Reports Suite",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Export and print attendance CSV/Excel/PDF",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    lineHeight = 13.sp
                                )
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
fun AdminCalendarScreen(viewModel: MainViewModel) {
    val employeeOptions by viewModel.adminCalendarEmployees.collectAsState()
    val selectedEmpId by viewModel.adminCalendarSelectedEmpId.collectAsState()
    val selectedMonth by viewModel.adminCalendarSelectedMonth.collectAsState()
    val selectedYear by viewModel.adminCalendarSelectedYear.collectAsState()
    val loadState by viewModel.adminCalendarLoadState.collectAsState()
    val calendarSummary by viewModel.adminCalendarSummary.collectAsState()
    val errorMessage by viewModel.adminCalendarErrorMessage.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDayNum by remember { mutableStateOf<Int?>(null) }

    // Load initial options and summary on screen mount
    LaunchedEffect(Unit) {
        viewModel.loadEmployeeOptions()
        viewModel.loadSummary()
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

    // Offset: Monday = 1, Tuesday = 2, ... Sunday = 7
    // Adjust to starting offset for the grid (Monday is index 0)
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

    val selectedEmployeeName = remember(employeeOptions, selectedEmpId) {
        employeeOptions.find { it.employeeId == (selectedEmpId ?: "ALL") }?.name ?: "All Employees"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val state = loadState) {
            is MainViewModel.AdminCalendarLoadState.Loading -> {
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

            is MainViewModel.AdminCalendarLoadState.Error -> {
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
                                viewModel.loadSummary()
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            is MainViewModel.AdminCalendarLoadState.Success -> {
                val summary = state.summary

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Personnel Calendars",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Select an employee to display their dynamic heatmap",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Dropdown Filter Above Month Header
                    var expandedDropdown by remember { mutableStateOf(false) }

                    Text(
                        text = "Employee:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("employee_dropdown_trigger"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedEmployeeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand dropdown"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            employeeOptions.forEach { emp ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (emp.employeeId == "ALL") "All Employees" else "${emp.name} (${emp.employeeId})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectEmployee(emp.employeeId)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Month Selection Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.changeAdminMonth(-1) },
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
                            onClick = { viewModel.changeAdminMonth(1) },
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
                                                            selectedDayNum = dayNum
                                                            showBottomSheet = true
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

    // Modal Bottom Sheet detailing day attendance stats
    if (showBottomSheet && selectedDayNum != null) {
        val dayNum = selectedDayNum!!
        val dateString = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, dayNum)

        // Filter logs for this specific date
        val dayLogs = remember(allLogs, selectedEmpId, dateString) {
            if (selectedEmpId == null) {
                allLogs.filter { it.date == dateString }
            } else {
                allLogs.filter { it.date == dateString && it.employeeId == selectedEmpId }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .testTag("calendar_detail_sheet")
            ) {
                Text(
                    text = "Attendance Details — $dateString",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (dayLogs.isEmpty()) {
                    val status = calendarSummary?.days?.get(dayNum) ?: "none"
                    val message = if (status.lowercase() == "holiday") {
                        "Official Holiday / Weekly Off"
                    } else {
                        "No attendance record found for this date."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(dayLogs) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = log.employeeName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "ID: ${log.employeeId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val chipBg = when (log.status) {
                                            "PRESENT", "success" -> Color(0xFFE6FDF4)
                                            "LATE" -> Color(0xFFFEF3C7)
                                            "ABSENT", "failed" -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFF1F5F9)
                                        }

                                        val chipText = when (log.status) {
                                            "PRESENT", "success" -> Color(0xFF047857)
                                            "LATE" -> Color(0xFFB45309)
                                            "ABSENT", "failed" -> Color(0xFFB91C1C)
                                            else -> Color(0xFF475569)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = chipBg,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = log.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipText,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Time: ${log.getFormattedTime()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = log.locationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AdminReportsScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isExportingCsv by viewModel.isExportingCsv.collectAsState()
    val isExportingExcel by viewModel.isExportingExcel.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Export Reports Suite",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Download aggregated smart city attendance files immediately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // CSV Card
        ReportDownloadCard(
            title = "CSV Export",
            desc = "Download all attendance records as a comma-separated values file.",
            icon = Icons.Outlined.Article,
            iconColor = Color(0xFF0F172A),
            buttonLabel = "Download CSV",
            isExporting = isExportingCsv,
            onDownload = {
                viewModel.exportFullAttendance(context, MainViewModel.ExportFormat.CSV)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Excel Card
        ReportDownloadCard(
            title = "Excel Export",
            desc = "Download a formatted .xlsx spreadsheet with all attendance data.",
            icon = Icons.Outlined.GridOn,
            iconColor = Color(0xFF16A34A),
            buttonLabel = "Download Excel",
            isExporting = isExportingExcel,
            onDownload = {
                viewModel.exportFullAttendance(context, MainViewModel.ExportFormat.EXCEL)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Card
        ReportDownloadCard(
            title = "PDF Report",
            desc = "Download a printable PDF attendance report with company header.",
            icon = Icons.Outlined.PictureAsPdf,
            iconColor = Color(0xFFDC2626),
            buttonLabel = "Download PDF",
            isExporting = isExportingPdf,
            onDownload = {
                viewModel.exportFullAttendance(context, MainViewModel.ExportFormat.PDF)
            }
        )
    }
}

@Composable
fun ReportDownloadCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    buttonLabel: String,
    isExporting: Boolean,
    onDownload: () -> Unit
) {
    val isCsv = title.contains("CSV")
    val displayIconColor = if (isCsv) MaterialTheme.colorScheme.primary else iconColor
    val buttonBgColor = if (isCsv) MaterialTheme.colorScheme.primary else iconColor
    val buttonContentColor = if (isCsv) MaterialTheme.colorScheme.onPrimary else Color.White

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = displayIconColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = icon, contentDescription = null, tint = displayIconColor, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDownload,
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonContentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = buttonContentColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporting...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(buttonLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportProgressDialog(format: String, onDismiss: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        scope.launch {
            while (progress < 1f) {
                delay(120)
                progress += 0.08f
            }
            delay(400)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color(0xFF1A56DB),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Compiling executive $format Ledger...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress.coerceAtMost(1f),
                    color = Color(0xFF1A56DB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress.coerceAtMost(1f) * 100).toInt()}% Generated",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
        val formatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

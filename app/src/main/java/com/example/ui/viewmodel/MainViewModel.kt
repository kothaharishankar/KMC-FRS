package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.Constants
import com.example.core.ApiException
import com.example.data.local.PreferenceManager
import com.example.data.mock.MockDataRepository
import com.example.data.remote.AuthService
import com.example.data.remote.AttendanceService
import com.example.model.AttendanceRecord
import com.example.model.User
import com.example.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun String.equalsIgnoreCase(other: String): Boolean = this.equals(other, ignoreCase = true)

enum class AuthStatus { UNKNOWN, AUTHENTICATED, UNAUTHENTICATED }

enum class LocationStatusState { DETECTING, GRANTED, DENIED, ERROR }

sealed class TodayAttendanceState {
    object Loading : TodayAttendanceState()
    data class Marked(val record: AttendanceRecord) : TodayAttendanceState()
    object NotMarked : TodayAttendanceState()
    data class Error(val message: String) : TodayAttendanceState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager(application)
    
    // Module 2 & 3 core states
    val authStatus = MutableStateFlow(AuthStatus.UNKNOWN)
    val isLoading = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val isAdmin = MutableStateFlow(false)
    private val authService = AuthService()
    private val attendanceService = AttendanceService()
    private val adminService = com.example.data.remote.AdminService()

    // Module 3 core flows
    val isMarkingInProgress = MutableStateFlow(false)
    val todayStatusState = MutableStateFlow<TodayAttendanceState>(TodayAttendanceState.Loading)
    val locationStatus = MutableStateFlow(LocationStatusState.DETECTING)

    // Theme state
    val isDarkMode: StateFlow<Boolean> = preferenceManager.isDarkMode

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Form inputs and UI State
    val loginId = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val loginError = MutableStateFlow<String?>(null)
    val isAuthenticating = MutableStateFlow(false)

    // Signup form
    val signupId = MutableStateFlow("")
    val signupName = MutableStateFlow("")
    val signupEmail = MutableStateFlow("")
    val signupPhone = MutableStateFlow("")
    val signupPassword = MutableStateFlow("")
    val signupConfirmPassword = MutableStateFlow("")
    val signupError = MutableStateFlow<String?>(null)
    val isSigningUp = MutableStateFlow(false)

    // Logged in User state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Active sub-tabs
    private val _activeEmployeeTab = MutableStateFlow<Screen.EmployeeTab>(Screen.EmployeeTab.Home)
    val activeEmployeeTab: StateFlow<Screen.EmployeeTab> = _activeEmployeeTab.asStateFlow()

    private val _activeAdminTab = MutableStateFlow<Screen.AdminTab>(Screen.AdminTab.Dashboard)
    val activeAdminTab: StateFlow<Screen.AdminTab> = _activeAdminTab.asStateFlow()

    // Attendance mark screen states
    val isLocationDetecting = MutableStateFlow(false)
    val locationName = MutableStateFlow("Detecting...")
    val currentLatitude = MutableStateFlow(Constants.OFFICE_LATITUDE)
    val currentLongitude = MutableStateFlow(Constants.OFFICE_LONGITUDE)
    val workplaceMode = MutableStateFlow("Office") // Office / Remote
    val isGpsWithinGeofence = MutableStateFlow(true)
    val gpsPermissionGranted = MutableStateFlow(true)

    // Camera viewfinder liveness/landmarks statuses
    val faceDetected = MutableStateFlow(false)
    val livenessScore = MutableStateFlow(0.0) // 0.0 to 1.0 liveness verification
    val isVerifyingFace = MutableStateFlow(false)
    val faceVerificationResult = MutableStateFlow<String?>(null)
    val faceVerificationStep = MutableStateFlow<String>("")
    val isCameraScanning = MutableStateFlow(true)

    // Native Coroutines Exception Handler (Equivalent to PlatformDispatcher.instance.onError / runZonedGuarded)
    // Catches all asynchronous coroutine scope exceptions (Camera, database, permission, network, etc.)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        android.util.Log.e("GlobalError", "Caught background asynchronous coroutine exception: ", exception)
        viewModelScope.launch {
            showBanner("Background Exception Intercepted: ${exception.localizedMessage ?: "Unknown Error"}", false)
            showPopup(
                title = "Asynchronous Error Intercepted",
                message = "An unexpected asynchronous exception was caught: ${exception.localizedMessage ?: "Unknown"}. The application state has been preserved safely.",
                iconType = "ERROR"
            )
        }
    }

    // Helper to safely execute any asynchronous operations, wrapping them in a safety boundary (Equivalent to runZonedGuarded)
    fun safeLaunch(
        showFeedback: Boolean = true,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                android.util.Log.e("SafeLaunch", "Caught exception in safeLaunch: ", e)
                if (showFeedback) {
                    showBanner("Operation Error: ${e.localizedMessage ?: "Unknown"}", false)
                    showPopup(
                        title = "Operation Error Intercepted",
                        message = "An error occurred during this operation: ${e.localizedMessage ?: "Unknown"}. The app is kept running smoothly.",
                        iconType = "ERROR"
                    )
                }
                onError?.invoke(e)
            }
        }
    }

    // Lists loaded from repository
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _allLogs = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val allLogs: StateFlow<List<AttendanceRecord>> = _allLogs.asStateFlow()

    // Employee Photos Room Database state flow
    val employeePhotos: StateFlow<List<com.example.data.local.EmployeePhoto>> =
        com.example.data.local.AppDatabase.getDatabase(getApplication())
            .employeePhotoDao()
            .getAllPhotos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Employee management states (moved early to prevent init order NullPointerException)
    private val _employees = MutableStateFlow<List<User>>(emptyList())
    val employees: StateFlow<List<User>> = _employees.asStateFlow()

    sealed class EmployeesLoadState {
        object Loading : EmployeesLoadState()
        object Loaded : EmployeesLoadState()
        object Empty : EmployeesLoadState()
        data class Error(val message: String) : EmployeesLoadState()
    }
    val employeesLoadState = MutableStateFlow<EmployeesLoadState>(EmployeesLoadState.Loading)

    // Admin attendance log & export states (moved early to prevent init order NullPointerException)
    enum class ExportFormat {
        CSV, EXCEL, PDF
    }

    sealed class AdminAttendanceLoadState {
        object Loading : AdminAttendanceLoadState()
        object Success : AdminAttendanceLoadState()
        object Empty : AdminAttendanceLoadState()
        data class Error(val message: String) : AdminAttendanceLoadState()
    }

    private val _adminAttendanceList = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val adminAttendanceList: StateFlow<List<AttendanceRecord>> = _adminAttendanceList.asStateFlow()

    val adminAttendanceLoadState = MutableStateFlow<AdminAttendanceLoadState>(AdminAttendanceLoadState.Loading)

    // Search and filter inputs for Admin Attendance Logs
    val logSearchQuery = MutableStateFlow("")
    val logFilterEmployeeId = MutableStateFlow("ALL") // ALL or individual employee IDs
    val selectedFilterDate = MutableStateFlow("") // empty string for all dates

    // Admin CRUD employee dialog/sheet states
    val adminEditingUser = MutableStateFlow<User?>(null) // If not null, we are editing
    val isEmployeeFormOpen = MutableStateFlow(false)
    val employeeFormId = MutableStateFlow("")
    val employeeFormName = MutableStateFlow("")
    val employeeFormEmail = MutableStateFlow("")
    val employeeFormPhone = MutableStateFlow("")

    // Notification banner / snackbar status
    val snackbarMessage = MutableStateFlow<String?>(null)
    val isSuccessBanner = MutableStateFlow(true) // success (green) vs error (red)

    // Pop-up dialog states
    val activePopupMessage = MutableStateFlow<String?>(null)
    val activePopupTitle = MutableStateFlow<String?>(null)
    val activePopupIconType = MutableStateFlow<String>("INFO") // INFO, SUCCESS, ERROR, WARNING

    // Module 5 Admin Dashboard States
    private val _adminDashboardStats = MutableStateFlow<com.example.model.AdminDashboardStats?>(null)
    val adminDashboardStats: StateFlow<com.example.model.AdminDashboardStats?> = _adminDashboardStats.asStateFlow()

    sealed class AdminDashboardLoadState {
        object Loading : AdminDashboardLoadState()
        object Loaded : AdminDashboardLoadState()
        data class Error(val message: String) : AdminDashboardLoadState()
    }
    val adminDashboardLoadState = MutableStateFlow<AdminDashboardLoadState>(AdminDashboardLoadState.Loading)

    val isSubmittingEmployee = MutableStateFlow(false)
    val employeeErrorMessage = MutableStateFlow<String?>(null)

    val isChangingPassword = MutableStateFlow(false)
    val changePasswordError = MutableStateFlow<String?>(null)
    val changePasswordSuccess = MutableStateFlow(false)

    val isAdminChangingPassword = MutableStateFlow(false)
    val adminChangePasswordError = MutableStateFlow<String?>(null)

    val isExportingCsv = MutableStateFlow(false)
    val isExportingExcel = MutableStateFlow(false)
    val isExportingPdf = MutableStateFlow(false)

    // History Tab State Flow
    private val _historyList = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val historyList: StateFlow<List<AttendanceRecord>> = _historyList.asStateFlow()

    sealed class HistoryLoadState {
        object Loading : HistoryLoadState()
        object Loaded : HistoryLoadState()
        object Empty : HistoryLoadState()
        data class Error(val message: String) : HistoryLoadState()
    }
    val historyLoadState = MutableStateFlow<HistoryLoadState>(HistoryLoadState.Loading)

    // Calendar Tab State Flow
    val monthSummary = MutableStateFlow<com.example.model.MonthSummary?>(null)

    sealed class CalendarLoadState {
        object Loading : CalendarLoadState()
        object Loaded : CalendarLoadState()
        data class Error(val message: String) : CalendarLoadState()
    }
    val calendarLoadState = MutableStateFlow<CalendarLoadState>(CalendarLoadState.Loading)

    // Calendar selectors
    val selectedMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) // 1-indexed (1 = Jan)
    val selectedYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))

    // --- MODULE 8 ADMIN CALENDAR & REPORTS ---
    sealed class AdminCalendarLoadState {
        object Loading : AdminCalendarLoadState()
        data class Success(val summary: com.example.model.MonthSummary) : AdminCalendarLoadState()
        data class Error(val message: String) : AdminCalendarLoadState()
    }

    val adminCalendarEmployees = MutableStateFlow<List<User>>(emptyList())
    val adminCalendarSelectedEmpId = MutableStateFlow<String?>(null) // null = all
    val adminCalendarSelectedMonth = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
    val adminCalendarSelectedYear = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val adminCalendarSummary = MutableStateFlow<com.example.model.MonthSummary?>(null)
    val adminCalendarLoadState = MutableStateFlow<AdminCalendarLoadState>(AdminCalendarLoadState.Loading)
    val adminCalendarErrorMessage = MutableStateFlow<String?>(null)

    init {
        // Load persistable mock data at very start of MainViewModel
        com.example.data.mock.MockDatabaseManager.loadMockData(application)

        // Module 2 required tryRestoreSession operation
        tryRestoreSession()

        // Fetch logs and users in background with automatic reactive sync
        viewModelScope.launch {
            MockDataRepository.users.collect { list ->
                _allUsers.value = list
                _employees.value = list.filter { it.role != "ADMIN" }
                if (employeesLoadState.value != EmployeesLoadState.Loading) {
                    employeesLoadState.value = if (list.filter { it.role != "ADMIN" }.isEmpty()) EmployeesLoadState.Empty else EmployeesLoadState.Loaded
                }
                // Automatically refresh stats immediately if Admin is active
                if (isAdmin.value && authStatus.value == AuthStatus.AUTHENTICATED) {
                    loadStats()
                }
            }
        }
        viewModelScope.launch {
            MockDataRepository.attendanceLogs.collect { list ->
                _allLogs.value = list
                // Automatically refresh stats and log list immediately if Admin is active
                if (isAdmin.value && authStatus.value == AuthStatus.AUTHENTICATED) {
                    loadStats()
                    _adminAttendanceList.value = list
                }
                // Automatically refresh today's status immediately for the active employee
                _currentUser.value?.employeeId?.let { empId ->
                    loadTodayStatus(empId)
                }
            }
        }

        // Automatic Admin Polling loop (every 5 seconds) to refresh logs in real-time
        viewModelScope.launch {
            while (true) {
                delay(5000)
                if (isAdmin.value && authStatus.value == AuthStatus.AUTHENTICATED) {
                    try {
                        val records = attendanceService.getAllAttendance(logSearchQuery.value.takeIf { it.isNotEmpty() })
                        _adminAttendanceList.value = records
                        if (records.isNotEmpty() && (adminAttendanceLoadState.value == AdminAttendanceLoadState.Loading || adminAttendanceLoadState.value == AdminAttendanceLoadState.Empty)) {
                            adminAttendanceLoadState.value = AdminAttendanceLoadState.Success
                        }
                    } catch (e: Exception) {
                        // Keep polling silently in background
                    }
                }
            }
        }

        // Simulate initial GPS fetch
        detectLocation()
    }

    fun toggleTheme() {
        preferenceManager.setDarkMode(!isDarkMode.value)
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.EmployeeHome) {
            _activeEmployeeTab.value = screen.initialTab
        } else if (screen is Screen.AdminHome) {
            _activeAdminTab.value = screen.initialTab
        }
    }

    fun loadStats() {
        adminDashboardLoadState.value = AdminDashboardLoadState.Loading
        viewModelScope.launch {
            try {
                val stats = adminService.getDashboardStats()
                _adminDashboardStats.value = stats
                adminDashboardLoadState.value = AdminDashboardLoadState.Loaded
            } catch (e: Exception) {
                adminDashboardLoadState.value = AdminDashboardLoadState.Error(e.localizedMessage ?: "Failed to load dashboard statistics.")
            }
        }
    }

    fun refresh() {
        if (_activeAdminTab.value == Screen.AdminTab.Dashboard) {
            loadStats()
        } else if (_activeAdminTab.value == Screen.AdminTab.Users) {
            loadEmployees()
        } else if (_activeAdminTab.value == Screen.AdminTab.Attendance) {
            loadAllAttendance(logSearchQuery.value)
        }
    }

    fun loadEmployees() {
        employeesLoadState.value = EmployeesLoadState.Loading
        viewModelScope.launch {
            try {
                val list = adminService.getEmployees().filter { it.role != "ADMIN" }
                _employees.value = list
                employeesLoadState.value = if (list.isEmpty()) EmployeesLoadState.Empty else EmployeesLoadState.Loaded
            } catch (e: Exception) {
                employeesLoadState.value = EmployeesLoadState.Error(e.localizedMessage ?: "Failed to load employees.")
            }
        }
    }

    fun addEmployee(user: User, password: String, onSuccess: () -> Unit) {
        isSubmittingEmployee.value = true
        employeeErrorMessage.value = null
        viewModelScope.launch {
            try {
                val createdUser = adminService.addEmployee(user, password)
                _employees.value = _employees.value + createdUser
                employeesLoadState.value = EmployeesLoadState.Loaded
                isSubmittingEmployee.value = false
                showBanner("Employee ${createdUser.name} added successfully!", true)
                onSuccess()
            } catch (e: Exception) {
                isSubmittingEmployee.value = false
                employeeErrorMessage.value = e.localizedMessage ?: "Failed to add employee"
                showBanner(employeeErrorMessage.value ?: "", false)
            }
        }
    }

    fun updateEmployee(empId: String, user: User, onSuccess: () -> Unit) {
        isSubmittingEmployee.value = true
        employeeErrorMessage.value = null
        val previousList = _employees.value
        viewModelScope.launch {
            try {
                val updatedList = previousList.map {
                    if (it.employeeId == empId) user else it
                }
                _employees.value = updatedList

                val result = adminService.updateEmployee(empId, user)
                _employees.value = previousList.map {
                    if (it.employeeId == empId) result else it
                }
                employeesLoadState.value = EmployeesLoadState.Loaded
                isSubmittingEmployee.value = false
                showBanner("Employee ${user.name} updated successfully!", true)
                onSuccess()
            } catch (e: Exception) {
                _employees.value = previousList
                isSubmittingEmployee.value = false
                employeeErrorMessage.value = e.localizedMessage ?: "Failed to update employee"
                showBanner(employeeErrorMessage.value ?: "", false)
            }
        }
    }


    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        val user = _currentUser.value ?: return
        isChangingPassword.value = true
        changePasswordError.value = null
        changePasswordSuccess.value = false
        viewModelScope.launch {
            try {
                authService.changePassword(user.employeeId, oldPassword, newPassword)
                changePasswordSuccess.value = true
                isChangingPassword.value = false
                showBanner("Your password has been changed successfully!", true)
                onSuccess()
            } catch (e: Exception) {
                isChangingPassword.value = false
                changePasswordError.value = e.localizedMessage ?: "Failed to change password."
                showBanner(changePasswordError.value ?: "", false)
            }
        }
    }

    fun adminChangeEmployeePassword(employeeId: String, newPassword: String, onSuccess: () -> Unit) {
        isAdminChangingPassword.value = true
        adminChangePasswordError.value = null
        viewModelScope.launch {
            try {
                authService.adminChangePassword(employeeId, newPassword)
                isAdminChangingPassword.value = false
                showBanner("Employee's password has been updated successfully!", true)
                onSuccess()
            } catch (e: Exception) {
                isAdminChangingPassword.value = false
                adminChangePasswordError.value = e.localizedMessage ?: "Failed to change employee's password."
                showBanner(adminChangePasswordError.value ?: "", false)
            }
        }
    }

    fun deleteEmployee(empId: String) {
        val previousList = _employees.value
        viewModelScope.launch {
            try {
                _employees.value = previousList.filter { it.employeeId != empId }
                if (_employees.value.isEmpty()) {
                    employeesLoadState.value = EmployeesLoadState.Empty
                }

                val success = adminService.deleteEmployee(empId)
                if (!success) {
                    throw Exception("Deletion failed on server")
                }
                showBanner("Employee deleted successfully", true)
            } catch (e: Exception) {
                _employees.value = previousList
                employeesLoadState.value = if (previousList.isEmpty()) EmployeesLoadState.Empty else EmployeesLoadState.Loaded
                showBanner(e.localizedMessage ?: "Failed to delete employee", false)
            }
        }
    }


    private var searchJob: kotlinx.coroutines.Job? = null

    fun loadAllAttendance(query: String? = null) {
        adminAttendanceLoadState.value = AdminAttendanceLoadState.Loading
        viewModelScope.launch {
            try {
                val records = attendanceService.getAllAttendance(query)
                _adminAttendanceList.value = records
                adminAttendanceLoadState.value = if (records.isEmpty()) {
                    AdminAttendanceLoadState.Empty
                } else {
                    AdminAttendanceLoadState.Success
                }
            } catch (e: Exception) {
                adminAttendanceLoadState.value = AdminAttendanceLoadState.Error(e.localizedMessage ?: "Failed to load attendance logs.")
            }
        }
    }

    fun searchAdminAttendance(query: String) {
        logSearchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce delay
            loadAllAttendance(query)
        }
    }

    fun exportAttendance(context: Context, format: ExportFormat) {
        val list = _adminAttendanceList.value
        if (list.isEmpty()) {
            showBanner("No attendance logs to export.", false)
            return
        }

        val exportFlow = when (format) {
            ExportFormat.CSV -> isExportingCsv
            ExportFormat.EXCEL -> isExportingExcel
            ExportFormat.PDF -> isExportingPdf
        }

        exportFlow.value = true
        viewModelScope.launch {
            try {
                delay(800) // Simulating rendering delay for better UX
                val file = when (format) {
                    ExportFormat.CSV -> com.example.data.ExportService.exportCsv(context, list)
                    ExportFormat.EXCEL -> com.example.data.ExportService.exportExcel(context, list)
                    ExportFormat.PDF -> com.example.data.ExportService.exportPdf(context, list)
                }
                
                val mimeType = when (format) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.EXCEL -> "application/vnd.ms-excel"
                    ExportFormat.PDF -> "application/pdf"
                }

                com.example.data.ExportService.shareFile(context, file, mimeType)
                showBanner("Report generated successfully! Sharing...", true)
            } catch (e: Exception) {
                showBanner("Export failed: ${e.localizedMessage ?: "Unknown error"}", false)
            } finally {
                exportFlow.value = false
            }
        }
    }

    fun setEmployeeTab(tab: Screen.EmployeeTab) {
        _activeEmployeeTab.value = tab
        _currentUser.value?.let { user ->
            when (tab) {
                Screen.EmployeeTab.History -> loadHistory(user.employeeId)
                Screen.EmployeeTab.Calendar -> loadMonthSummary(user.employeeId, selectedMonth.value, selectedYear.value)
                else -> {}
            }
        }
    }

    fun loadHistory(empId: String) {
        historyLoadState.value = HistoryLoadState.Loading
        viewModelScope.launch {
            try {
                val history = attendanceService.getHistory(empId)
                _historyList.value = history
                historyLoadState.value = if (history.isEmpty()) HistoryLoadState.Empty else HistoryLoadState.Loaded
            } catch (e: Exception) {
                historyLoadState.value = HistoryLoadState.Error(e.localizedMessage ?: "Failed to load history")
            }
        }
    }

    fun loadMonthSummary(empId: String, month: Int, year: Int) {
        calendarLoadState.value = CalendarLoadState.Loading
        viewModelScope.launch {
            try {
                val summary = attendanceService.getMonthSummary(empId, month, year)
                monthSummary.value = summary
                calendarLoadState.value = CalendarLoadState.Loaded
            } catch (e: Exception) {
                calendarLoadState.value = CalendarLoadState.Error(e.localizedMessage ?: "Failed to load monthly summary")
            }
        }
    }

    fun changeEmployeeMonth(delta: Int) {
        var currentMonth = selectedMonth.value + delta
        var currentYear = selectedYear.value

        while (currentMonth > 12) {
            currentMonth -= 12
            currentYear += 1
        }
        while (currentMonth < 1) {
            currentMonth += 12
            currentYear -= 1
        }

        selectedMonth.value = currentMonth
        selectedYear.value = currentYear

        _currentUser.value?.let { user ->
            loadMonthSummary(user.employeeId, currentMonth, currentYear)
        }
    }

    fun setAdminTab(tab: Screen.AdminTab) {
        _activeAdminTab.value = tab
        if (tab == Screen.AdminTab.Dashboard) {
            loadStats()
        } else if (tab == Screen.AdminTab.Users) {
            loadEmployees()
        } else if (tab == Screen.AdminTab.Attendance) {
            loadAllAttendance(logSearchQuery.value)
        }
    }

    // --- MODULE 2 LOGIN / SIGNUP OPERATIONS ---

    fun tryRestoreSession() {
        viewModelScope.launch {
            try {
                authStatus.value = AuthStatus.UNKNOWN
                
                val (storedId, storedName, storedRole) = preferenceManager.getUserSession()
                if (storedId != null && storedName != null && storedRole != null) {
                    val user = User(
                        employeeId = storedId,
                        name = storedName,
                        email = "",
                        phone = "",
                        role = storedRole,
                        joinedDate = ""
                    )
                    _currentUser.value = user
                    authStatus.value = AuthStatus.AUTHENTICATED
                    isAdmin.value = (storedRole == Constants.ROLE_ADMIN)
                    _currentScreen.value = if (storedRole == Constants.ROLE_ADMIN) {
                        Screen.AdminHome(Screen.AdminTab.Dashboard)
                    } else {
                        Screen.EmployeeHome(Screen.EmployeeTab.Home)
                    }
                    if (storedRole != Constants.ROLE_ADMIN) {
                        loadTodayStatus(storedId)
                    }
                    showBanner("Welcome Back ${user.name}!", true)
                } else {
                    authStatus.value = AuthStatus.UNAUTHENTICATED
                    isAdmin.value = false
                    _currentUser.value = null
                    _currentScreen.value = Screen.Login
                }
            } catch (e: Exception) {
                authStatus.value = AuthStatus.UNAUTHENTICATED
                isAdmin.value = false
                _currentUser.value = null
                _currentScreen.value = Screen.Login
            }
        }
    }

    fun login(isAdminRoute: Boolean = false) {
        val id = loginId.value.trim()
        val pwd = loginPassword.value

        errorMessage.value = null
        loginError.value = null
        isLoading.value = true
        isAuthenticating.value = true

        viewModelScope.launch {
            try {
                val (token, user) = if (isAdminRoute) {
                    authService.adminLogin(id, pwd)
                } else {
                    authService.login(id, pwd)
                }

                // If user is admin but tries to login via standard route, forward them or accept
                // Let's verify administrator constraints
                if (isAdminRoute && user.role != Constants.ROLE_ADMIN) {
                    throw ApiException("Access Denied: You do not have HR Administrator privileges.")
                }

                // Save session
                preferenceManager.saveSession(token, user.employeeId, user.name, user.role)
                _currentUser.value = user
                authStatus.value = AuthStatus.AUTHENTICATED
                isAdmin.value = (user.role == Constants.ROLE_ADMIN)

                // Clean inputs
                loginId.value = ""
                loginPassword.value = ""

                // Navigate
                if (user.role == Constants.ROLE_ADMIN) {
                    navigateTo(Screen.AdminHome(Screen.AdminTab.Dashboard))
                } else {
                    navigateTo(Screen.EmployeeHome(Screen.EmployeeTab.Home))
                }

                if (user.role != Constants.ROLE_ADMIN) {
                    loadTodayStatus(user.employeeId)
                }

                showBanner("Welcome Back ${user.name}!", true)
                showPopup("Welcome Back", "Welcome Back ${user.name}!", "SUCCESS")
            } catch (e: ApiException) {
                errorMessage.value = e.message
                loginError.value = e.message
                showBanner(e.message ?: "Authentication failed.", false)
            } catch (e: Exception) {
                errorMessage.value = e.localizedMessage
                loginError.value = e.localizedMessage
                showBanner(e.localizedMessage ?: "Unexpected error occurred.", false)
            } finally {
                isLoading.value = false
                isAuthenticating.value = false
            }
        }
    }

    fun adminLogin() {
        login(isAdminRoute = true)
    }

    fun signup() {
        val users = MockDataRepository.users.value
        val cccPattern = Regex("^CCC(\\d+)$")
        var maxIdVal = 0 // Base default
        for (u in users) {
            val match = cccPattern.find(u.employeeId)
            if (match != null) {
                val numStr = match.groupValues[1]
                val num = numStr.toIntOrNull() ?: 0
                if (num > maxIdVal) {
                    maxIdVal = num
                }
            }
        }
        val nextIdVal = maxIdVal + 1
        val id = String.format("CCC%03d", nextIdVal)

        val name = signupName.value.trim()
        val email = signupEmail.value.trim()
        val phone = signupPhone.value.trim()
        val pwd = signupPassword.value
        val confirmPwd = signupConfirmPassword.value

        errorMessage.value = null
        signupError.value = null
        isLoading.value = true
        isSigningUp.value = true

        viewModelScope.launch {
            try {
                // Pre-validation (confirm password mismatch check)
                if (pwd != confirmPwd) {
                    throw ApiException("Passwords do not match. Please verify and re-type.")
                }

                val (token, user) = authService.signup(id, name, email, phone, pwd)

                // On signup success, save session & log them in immediately!
                preferenceManager.saveSession(token, user.employeeId, user.name, user.role)
                _currentUser.value = user
                authStatus.value = AuthStatus.AUTHENTICATED
                isAdmin.value = (user.role == Constants.ROLE_ADMIN)

                // Clear fields
                signupId.value = ""
                signupName.value = ""
                signupEmail.value = ""
                signupPhone.value = ""
                signupPassword.value = ""
                signupConfirmPassword.value = ""

                // Navigate immediately to employee dashboard
                navigateTo(Screen.EmployeeHome(Screen.EmployeeTab.Home))
                loadTodayStatus(user.employeeId)
                showBanner("Welcome Back ${user.name}!", true)
                showPopup("Welcome Back", "Welcome Back ${user.name}!", "SUCCESS")
            } catch (e: ApiException) {
                errorMessage.value = e.message
                signupError.value = e.message
                showBanner(e.message ?: "Enrollment failed.", false)
            } catch (e: Exception) {
                errorMessage.value = e.localizedMessage
                signupError.value = e.localizedMessage
                showBanner(e.localizedMessage ?: "Unexpected registration error.", false)
            } finally {
                isLoading.value = false
                isSigningUp.value = false
            }
        }
    }

    fun logout() {
        preferenceManager.clearSession()
        _currentUser.value = null
        android.util.Log.d("APP_DEBUG", "Setting UNAUTHENTICATED")
                authStatus.value = AuthStatus.UNAUTHENTICATED
        isAdmin.value = false
        todayStatusState.value = TodayAttendanceState.Loading
        navigateTo(Screen.Login)
        showBanner("Successfully logged out.", true)
    }

    fun clearError() {
        errorMessage.value = null
        loginError.value = null
        signupError.value = null
    }

    // --- Backward compatibility aliases ---
    fun performLogin(isAdminRoute: Boolean = false) {
        login(isAdminRoute)
    }

    fun performSignup() {
        signup()
    }

    fun performLogout() {
        logout()
    }

    // --- GEOLOCATION MANAGEMENT ---

    fun detectLocation() {
        locationStatus.value = LocationStatusState.DETECTING
        isLocationDetecting.value = true
        viewModelScope.launch {
            delay(1500) // Simulate GPS fix with a realistic loading delay
            
            // Default: User is standing inside Kakinada Smart City Office
            currentLatitude.value = Constants.OFFICE_LATITUDE + (Math.random() - 0.5) * 0.0002
            currentLongitude.value = Constants.OFFICE_LONGITUDE + (Math.random() - 0.5) * 0.0002
            
            recalculateGeofenceDistance()
            locationStatus.value = LocationStatusState.GRANTED
            isLocationDetecting.value = false
        }
    }

    // User can manually simulate being Remote to demonstrate flexible workplace configuration
    fun toggleWorkplaceMode() {
        if (workplaceMode.value == "Office") {
            workplaceMode.value = "Remote"
            currentLatitude.value = 17.4065 // Simulating Hyderabad or other remote area
            currentLongitude.value = 78.4772
            locationName.value = "Remote Work - East Wing Residency"
            isGpsWithinGeofence.value = false
        } else {
            workplaceMode.value = "Office"
            currentLatitude.value = Constants.OFFICE_LATITUDE
            currentLongitude.value = Constants.OFFICE_LONGITUDE
            locationName.value = "CCC kakinada"
            isGpsWithinGeofence.value = true
        }
    }

    private fun recalculateGeofenceDistance() {
        val dist = calculateDistance(
            currentLatitude.value, currentLongitude.value,
            Constants.OFFICE_LATITUDE, Constants.OFFICE_LONGITUDE
        )
        if (dist <= Constants.GEOFENCE_RADIUS_METERS) {
            isGpsWithinGeofence.value = true
            locationName.value = "CCC kakinada"
            workplaceMode.value = "Office"
        } else {
            isGpsWithinGeofence.value = false
            locationName.value = "Remote Workplace (${String.format("%.1f", dist / 1000.0)} km)"
            workplaceMode.value = "Remote"
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // metres
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    // --- ATTENDANCE VERIFICATION & SUBMISSION (MODULE 3) ---

    fun loadTodayStatus(empId: String) {
        todayStatusState.value = TodayAttendanceState.Loading
        viewModelScope.launch {
            try {
                val record = attendanceService.getTodayStatus(empId)
                if (record != null) {
                    todayStatusState.value = TodayAttendanceState.Marked(record)
                } else {
                    todayStatusState.value = TodayAttendanceState.NotMarked
                }
            } catch (e: Exception) {
                todayStatusState.value = TodayAttendanceState.Error(e.localizedMessage ?: "Could not verify today's attendance status.")
            }
        }
    }

    fun submitAttendance(empId: String, imageBytes: ByteArray? = null) {
        if (isMarkingInProgress.value) return
        
        // Prevent duplicate attendance
        if (todayStatusState.value is TodayAttendanceState.Marked) {
            errorMessage.value = "Attendance already marked for today!"
            faceVerificationResult.value = "Failed"
            showBanner("Attendance already marked for today!", false)
            return
        }
        
        isMarkingInProgress.value = true
        isVerifyingFace.value = true
        faceVerificationResult.value = null
        faceVerificationStep.value = "Camera permission verified ✓"
        errorMessage.value = null

        viewModelScope.launch {
            try {
                // Step 3: Face detected simulation
                delay(600)
                faceDetected.value = true
                faceVerificationStep.value = "Face detected ✓"
                
                // Step 4: Face recognized simulation
                delay(600)
                livenessScore.value = 0.98
                faceVerificationStep.value = "Face recognized ✓"
                
                // Step 5: Employee identified
                delay(400)
                faceVerificationStep.value = "Employee identified ✓"
                
                // Step 6: Attendance stored
                delay(400)
                faceVerificationStep.value = "Storing attendance..."
                val record = withContext(Dispatchers.IO) {
                    attendanceService.markAttendance(
                        empId = empId,
                        imageBytes = imageBytes,
                        latitude = currentLatitude.value,
                        longitude = currentLongitude.value
                    )
                }

                // Step 7: Database updated (Local Room DB update)
                faceVerificationStep.value = "Updating local database..."
                val photoB64 = record.photoUrl ?: imageBytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                val confidence = livenessScore.value
                val realEmpName = _currentUser.value?.name 
                    ?: MockDataRepository.users.value.find { it.employeeId.equalsIgnoreCase(empId) }?.name 
                    ?: record.employeeName.takeIf { !it.startsWith("Employee EMP", ignoreCase = true) } 
                    ?: "Employee $empId"

                val updatedRecord = record.copy(
                    employeeName = realEmpName,
                    photoUrl = photoB64,
                    faceConfidence = confidence,
                    timestamp = System.currentTimeMillis()
                )

                // Save photo to Room DB for Admin Portal view
                if (photoB64 != null && photoB64.isNotEmpty()) {
                    try {
                        withContext(Dispatchers.IO) {
                            val appDb = com.example.data.local.AppDatabase.getDatabase(getApplication())
                            val empPhoto = com.example.data.local.EmployeePhoto(
                                employeeId = updatedRecord.employeeId,
                                employeeName = updatedRecord.employeeName,
                                date = updatedRecord.date,
                                time = updatedRecord.time,
                                photoBase64 = photoB64
                            )
                            appDb.employeePhotoDao().insertPhoto(empPhoto)
                        }
                    } catch (dbEx: Exception) {
                        // Suppress background DB save issues
                    }
                }

                // Prepend to overall list immediately for live view sync and save to Room database
                withContext(Dispatchers.IO) {
                    com.example.data.mock.MockDataRepository.addAttendanceRecord(updatedRecord)
                }
                
                faceVerificationStep.value = "Database updated ✓"
                delay(300)

                // Step 8: Attendance history updated
                faceVerificationStep.value = "Updating attendance history..."
                val updatedLogs = _allLogs.value.toMutableList()
                updatedLogs.removeAll { it.employeeId.equalsIgnoreCase(empId) && it.date == updatedRecord.date }
                updatedLogs.add(0, updatedRecord)
                _allLogs.value = updatedLogs
                _adminAttendanceList.value = updatedLogs
                faceVerificationStep.value = "Attendance history updated ✓"
                delay(300)

                // Step 9: Dashboard refreshed
                faceVerificationStep.value = "Refreshing dashboard..."
                todayStatusState.value = TodayAttendanceState.Marked(updatedRecord)
                loadStats() // Refresh Admin Portal stats immediately
                faceVerificationStep.value = "Dashboard refreshed ✓"
                delay(300)

                // Step 10: Show Attendance Marked Successfully Banner/Popup
                faceVerificationResult.value = "Success"
                showBanner("Attendance Marked Successfully ✓ Status: ${updatedRecord.status}", true)
                showPopup("Attendance Marked ✓", "Hello ${updatedRecord.employeeName}, your attendance has been successfully marked as ${updatedRecord.status} at ${updatedRecord.time}.", "SUCCESS")
            } catch (e: Exception) {
                faceVerificationResult.value = "Failed"
                errorMessage.value = e.localizedMessage
                showBanner(e.localizedMessage ?: "Failed to mark attendance.", false)
            } finally {
                isMarkingInProgress.value = false
                isVerifyingFace.value = false
                faceVerificationStep.value = ""
            }
        }
    }

    fun startFaceVerification(imageBytes: ByteArray? = null) {
        val user = _currentUser.value ?: return
        submitAttendance(user.employeeId, imageBytes)
    }

    // --- ADMIN CRUD - EMPLOYEE MANAGEMENT ---

    fun openAddEmployeeDialog() {
        adminEditingUser.value = null
        employeeFormId.value = ""
        employeeFormName.value = ""
        employeeFormEmail.value = ""
        employeeFormPhone.value = ""
        isEmployeeFormOpen.value = true
    }

    fun openEditEmployeeDialog(user: User) {
        adminEditingUser.value = user
        employeeFormId.value = user.employeeId
        employeeFormName.value = user.name
        employeeFormEmail.value = user.email
        employeeFormPhone.value = user.phone
        isEmployeeFormOpen.value = true
    }

    fun submitEmployeeForm() {
        val id = employeeFormId.value.trim().uppercase()
        val name = employeeFormName.value.trim()
        val email = employeeFormEmail.value.trim()
        val phone = employeeFormPhone.value.trim()

        if (id.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showBanner("Please fill out all fields.", false)
            return
        }

        if (adminEditingUser.value == null) {
            // ADD NEW
            val newUser = User(
                employeeId = id,
                name = name,
                email = email,
                phone = phone,
                role = "EMPLOYEE",
                joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            val success = MockDataRepository.addEmployee(newUser)
            if (success) {
                showBanner("Employee $id Added Successfully", true)
                isEmployeeFormOpen.value = false
            } else {
                showBanner("Employee ID $id already exists.", false)
            }
        } else {
            // UPDATE EXISTING
            val existing = adminEditingUser.value!!
            val updatedUser = existing.copy(
                name = name,
                email = email,
                phone = phone
            )
            val success = MockDataRepository.updateEmployee(updatedUser)
            if (success) {
                showBanner("Employee details updated successfully", true)
                isEmployeeFormOpen.value = false
            } else {
                showBanner("Failed to update employee.", false)
            }
        }
    }



    // Helper banner message triggers
    fun showBanner(message: String, isSuccess: Boolean) {
        snackbarMessage.value = message
        isSuccessBanner.value = isSuccess
        viewModelScope.launch {
            delay(3500)
            if (snackbarMessage.value == message) {
                snackbarMessage.value = null
            }
        }
    }
    
    fun dismissBanner() {
        snackbarMessage.value = null
    }

    fun showPopup(title: String, message: String, iconType: String = "INFO") {
        activePopupTitle.value = title
        activePopupMessage.value = message
        activePopupIconType.value = iconType
    }

    fun dismissPopup() {
        activePopupMessage.value = null
    }

    // --- MODULE 8 ADMIN CALENDAR & REPORTS ---
    fun loadEmployeeOptions() {
        viewModelScope.launch {
            try {
                val list = adminService.getEmployeeList()
                val completeList = listOf(
                    User(
                        employeeId = "ALL",
                        name = "All Employees",
                        email = "",
                        phone = "",
                        role = "EMPLOYEE",
                        joinedDate = ""
                    )
                ) + list
                adminCalendarEmployees.value = completeList
            } catch (e: Exception) {
                adminCalendarErrorMessage.value = e.localizedMessage ?: "Failed to load employees"
            }
        }
    }

    fun loadSummary() {
        adminCalendarLoadState.value = AdminCalendarLoadState.Loading
        adminCalendarErrorMessage.value = null
        viewModelScope.launch {
            try {
                val empId = adminCalendarSelectedEmpId.value
                val month = adminCalendarSelectedMonth.value
                val year = adminCalendarSelectedYear.value

                val summary = adminService.getMonthSummaryForAll(month, year, empId)
                adminCalendarSummary.value = summary
                adminCalendarLoadState.value = AdminCalendarLoadState.Success(summary)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Failed to load calendar summary"
                adminCalendarErrorMessage.value = msg
                adminCalendarLoadState.value = AdminCalendarLoadState.Error(msg)
            }
        }
    }

    fun selectEmployee(empId: String?) {
        adminCalendarSelectedEmpId.value = if (empId == "ALL" || empId.isNullOrEmpty()) null else empId
        loadSummary()
    }

    fun changeAdminMonth(delta: Int) {
        var newMonth = adminCalendarSelectedMonth.value + delta
        var newYear = adminCalendarSelectedYear.value

        if (newMonth > 12) {
            newMonth = 1
            newYear += 1
        } else if (newMonth < 1) {
            newMonth = 12
            newYear -= 1
        }

        adminCalendarSelectedMonth.value = newMonth
        adminCalendarSelectedYear.value = newYear
        loadSummary()
    }

    fun exportFullAttendance(context: Context, format: ExportFormat) {
        val exportFlow = when (format) {
            ExportFormat.CSV -> isExportingCsv
            ExportFormat.EXCEL -> isExportingExcel
            ExportFormat.PDF -> isExportingPdf
        }

        exportFlow.value = true
        viewModelScope.launch {
            try {
                // Fetch full org-wide attendance list directly, ignoring search/filters
                val fullList = attendanceService.getAllAttendance(null)
                if (fullList.isEmpty()) {
                    showBanner("No attendance logs to export.", false)
                    return@launch
                }
                delay(800) // Simulating rendering delay for better UX
                val file = when (format) {
                    ExportFormat.CSV -> com.example.data.ExportService.exportCsv(context, fullList)
                    ExportFormat.EXCEL -> com.example.data.ExportService.exportExcel(context, fullList)
                    ExportFormat.PDF -> com.example.data.ExportService.exportPdf(context, fullList)
                }

                val mimeType = when (format) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.EXCEL -> "application/vnd.ms-excel"
                    ExportFormat.PDF -> "application/pdf"
                }

                com.example.data.ExportService.shareFile(context, file, mimeType)
                showBanner("Full report generated successfully! Sharing...", true)
            } catch (e: Exception) {
                showBanner("Export failed: ${e.localizedMessage ?: "Unknown error"}", false)
            } finally {
                exportFlow.value = false
            }
        }
    }
}

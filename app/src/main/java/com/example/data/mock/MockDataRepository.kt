package com.example.data.mock

import android.content.Context
import com.example.model.AttendanceRecord
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

object MockDataRepository {
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
    }
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
    }

    private val _attendanceLogs = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceLogs: StateFlow<List<AttendanceRecord>> = _attendanceLogs

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var dbInstance: com.example.data.local.AppDatabase? = null
    private var isInitialized = false
    private var lastFileTimestamp = 0L

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        dbInstance = db

        // Observe employees from Room
        repositoryScope.launch {
            db.employeeDao().getAllEmployees().collect { list ->
                _users.value = list
                // Sync with AdminService.mockEmployees
                com.example.data.remote.AdminService.mockEmployees.clear()
                com.example.data.remote.AdminService.mockEmployees.addAll(list)
                backupState()
            }
        }

        // Observe attendance logs from Room
        repositoryScope.launch {
            db.attendanceRecordDao().getAllRecords().collect { list ->
                _attendanceLogs.value = list
                backupState()
            }
        }

        // Real-time bidirectional file sync: monitor for changes written by Desktop/External applications
        repositoryScope.launch {
            val file = java.io.File("/sdcard/Download/ksccl_backup.json")
            if (file.exists()) {
                lastFileTimestamp = file.lastModified()
            }
            while (true) {
                kotlinx.coroutines.delay(1500)
                try {
                    if (file.exists()) {
                        val currentTimestamp = file.lastModified()
                        if (currentTimestamp > lastFileTimestamp) {
                            lastFileTimestamp = currentTimestamp
                            val restored = MockDatabaseManager.restoreFromSdcard(context)
                            if (restored != null) {
                                val (restoredUsers, restoredAttendance) = restored
                                if (restoredUsers.isNotEmpty()) {
                                    // Merge users/employees
                                    db.employeeDao().insertEmployees(restoredUsers)
                                    // Merge attendance logs
                                    restoredAttendance.forEach { att ->
                                        db.attendanceRecordDao().insertRecord(att)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Seeding or restoring
        repositoryScope.launch {
            val hasAdmins = db.employeeDao().getEmployeeById("ADMIN")
            if (hasAdmins == null) {
                val restored = MockDatabaseManager.restoreFromSdcard(context)
                if (restored != null) {
                    val (restoredUsers, restoredAttendance) = restored
                    if (restoredUsers.isNotEmpty()) {
                        db.employeeDao().insertEmployees(restoredUsers)
                        restoredAttendance.forEach { att ->
                            db.attendanceRecordDao().insertRecord(att)
                        }
                        return@launch
                    }
                }

                // First run defaults
                val defaultList = listOf(
                    User("ADMIN", "HR Admin", "admin@ksccl.gov.in", "9000000001", "ADMIN", "2023-05-20", password = "admin123"),
                    User("CCC001", "Ramesh Kumar", "ramesh@ksccl.gov.in", "9876543210", "EMPLOYEE", "2024-01-15", password = "password123"),
                    User("CCC002", "Sita Devi", "sita@ksccl.gov.in", "9876543211", "EMPLOYEE", "2024-02-10", password = "password123")
                )
                db.employeeDao().insertEmployees(defaultList)
                
                com.example.data.remote.AdminService.mockEmployeePasswords.clear()
                defaultList.forEach { u ->
                    com.example.data.remote.AdminService.mockEmployeePasswords[u.employeeId] = u.password
                }
            } else {
                // Initialize passwords map from database
                db.employeeDao().getAllEmployees().collect { list ->
                    list.forEach { u ->
                        com.example.data.remote.AdminService.mockEmployeePasswords[u.employeeId] = u.password
                    }
                }
            }
        }
    }

    private fun backupState() {
        val usersList = _users.value
        val attList = _attendanceLogs.value
        MockDatabaseManager.backupToSdcard(usersList, attList)
        MockDatabaseManager.saveMockDataToPrefs(usersList, com.example.data.remote.AdminService.mockEmployeePasswords)
        val file = java.io.File("/sdcard/Download/ksccl_backup.json")
        if (file.exists()) {
            lastFileTimestamp = file.lastModified()
        }
    }

    fun setUsers(list: List<User>) {
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployees(list)
        }
    }

    // Auth actions
    fun authenticate(employeeId: String, password: String): Result<User> {
        val trimmedId = employeeId.trim().uppercase()
        val user = _users.value.find { it.employeeId.uppercase() == trimmedId }
        
        return if (user != null) {
            if (user.password == password) {
                Result.success(user)
            } else {
                Result.failure(Exception("Incorrect password"))
            }
        } else {
            Result.failure(Exception("User with ID $employeeId not found or credentials invalid"))
        }
    }

    fun registerUser(
        employeeId: String,
        name: String,
        email: String,
        phone: String,
        role: String = "EMPLOYEE"
    ): Result<User> {
        val trimmedId = employeeId.trim().uppercase()
        if (_users.value.any { it.employeeId.uppercase() == trimmedId }) {
            return Result.failure(Exception("Employee ID already registered"))
        }

        val newUser = User(
            employeeId = trimmedId,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = role,
            joinedDate = dateFormat.format(Date()),
            password = "password123"
        )
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployee(newUser)
        }
        return Result.success(newUser)
    }

    fun addEmployeeFromAdminService(user: User) {
        val trimmedId = user.employeeId.trim().uppercase()
        val password = com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId] ?: "password123"
        val newUser = user.copy(employeeId = trimmedId, password = password)
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployee(newUser)
        }
    }

    fun updateEmployeeFromAdminService(user: User) {
        val trimmedId = user.employeeId.trim().uppercase()
        val password = com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId] ?: "password123"
        val newUser = user.copy(employeeId = trimmedId, password = password)
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployee(newUser)
        }
    }

    fun deleteEmployeeFromAdminService(empId: String) {
        val trimmedId = empId.trim().uppercase()
        repositoryScope.launch {
            dbInstance?.employeeDao()?.deleteEmployeeById(trimmedId)
        }
    }

    // CRUD actions for Employee Management
    fun addEmployee(user: User): Boolean {
        val trimmedId = user.employeeId.trim().uppercase()
        if (_users.value.any { it.employeeId.uppercase() == trimmedId }) return false
        val newUser = user.copy(employeeId = trimmedId)
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployee(newUser)
        }
        return true
    }

    fun updateEmployee(user: User): Boolean {
        val trimmedId = user.employeeId.trim().uppercase()
        if (!_users.value.any { it.employeeId.uppercase() == trimmedId }) return false
        val updatedUser = user.copy(employeeId = trimmedId)
        repositoryScope.launch {
            dbInstance?.employeeDao()?.insertEmployee(updatedUser)
        }
        return true
    }

    fun deleteEmployee(employeeId: String): Boolean {
        val trimmedId = employeeId.trim().uppercase()
        if (!_users.value.any { it.employeeId.uppercase() == trimmedId }) return false
        repositoryScope.launch {
            dbInstance?.employeeDao()?.deleteEmployeeById(trimmedId)
        }
        return true
    }

    fun addAttendanceRecord(record: AttendanceRecord) {
        repositoryScope.launch {
            dbInstance?.attendanceRecordDao()?.insertRecord(record)
        }
    }

    // Attendance action
    fun markAttendance(
        employeeId: String,
        employeeName: String,
        latitude: Double,
        longitude: Double,
        locationName: String
    ): Result<AttendanceRecord> {
        val today = dateFormat.format(Date())
        
        // Prevent duplicate mark
        val alreadyMarked = _attendanceLogs.value.any { 
            it.employeeId == employeeId && it.date == today && (it.status == "PRESENT" || it.status == "LATE") 
        }
        if (alreadyMarked) {
            return Result.failure(Exception("Attendance already marked for today!"))
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val isLate = currentHour > 10 || (currentHour == 10 && currentMinute > 30)
        val status = if (isLate) "LATE" else "PRESENT"
        val lateBy = if (isLate) {
            (currentHour * 60 + currentMinute) - (10 * 60 + 30)
        } else null
        val lateDuration = if (isLate && lateBy != null) {
            val h = lateBy / 60
            val m = lateBy % 60
            if (h > 0) "${h} hr ${m} min late" else "${m} min late"
        } else {
            null
        }

        val record = AttendanceRecord(
            id = "ATT_${System.currentTimeMillis()}",
            employeeId = employeeId,
            employeeName = employeeName,
            date = today,
            time = timeFormat.format(Date()),
            status = status,
            lateDuration = lateDuration,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            lateByMinutes = lateBy
        )

        repositoryScope.launch {
            dbInstance?.attendanceRecordDao()?.insertRecord(record)
        }
        return Result.success(record)
    }
}

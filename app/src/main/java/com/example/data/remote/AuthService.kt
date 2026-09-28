package com.example.data.remote

import com.example.core.Constants
import com.example.core.ApiException
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthService {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Handles employee login.
     * Supports both real HTTP Flask API calls and local mock-mode simulation.
     */
    suspend fun login(empId: String, password: String): Pair<String, User> = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        
        // 1. Validation Checks before any backend request
        if (trimmedId.isEmpty()) {
            throw ApiException("Employee ID cannot be empty.")
        }
        if (password.isEmpty()) {
            throw ApiException("Password cannot be empty.")
        }
        if (password.length < 6) {
            throw ApiException("Password must be at least 6 characters.")
        }

        // 2. Mock Backend Mode (Module 2 Requirement)
        if (Constants.useMockBackend) {
            delay(1200) // Artificial delay to simulate network roundtrip

            // Normal Mock success paths matching default users
            val matchedUser = com.example.data.mock.MockDataRepository.users.value.find { it.employeeId.uppercase() == trimmedId }
            if (matchedUser != null) {
                if (matchedUser.status.uppercase() != "ACTIVE") {
                    throw ApiException("Access Denied: Your account is currently ${matchedUser.status}. Please contact your HR Administrator.")
                }
                // Check if there is a password set in AdminService mock
                val savedPassword = com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId]
                if (savedPassword != null && savedPassword != password) {
                    throw ApiException("Incorrect password entered.")
                } else if (savedPassword == null && password != "password123") { // Default fallback password if none set 
                    throw ApiException("Incorrect password. Default is password123.")
                }
                
                // Return JWT token and mock user session
                return@withContext Pair("mock_jwt_token_for_${matchedUser.employeeId}", matchedUser)
            } else {
                throw ApiException("Employee ID $trimmedId is not registered in the system.")
            }
        }

        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_LOGIN}"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(bodyString).getString("error")
                    } catch (e: Exception) {
                        "HTTP login failed with status code ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }
                
                val responseJson = JSONObject(bodyString)
                val token = responseJson.getString("token")
                val userJson = responseJson.getJSONObject("user")
                
                val user = User(
                    employeeId = userJson.getString("employeeId"),
                    name = userJson.getString("name"),
                    email = userJson.optString("email", ""),
                    phone = userJson.optString("phone", ""),
                    role = userJson.getString("role"),
                    joinedDate = userJson.optString("joinedDate", ""),
                    department = userJson.optString("department", "Technical Support"),
                    designation = userJson.optString("designation", "Technical Executive"),
                    status = userJson.optString("status", "ACTIVE"),
                    faceRegistered = userJson.optBoolean("faceRegistered", true)
                )
                
                return@withContext Pair(token, user)
            }
        } catch (e: IOException) {
            throw ApiException("Network connection failure. Verify your internet connection or check Flask server status.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error: ${e.localizedMessage}", null)
        }
    }

    /**
     * Handles administrator portal login.
     * Supports both real HTTP Flask API calls and local mock-mode simulation.
     */
    suspend fun adminLogin(empId: String, password: String): Pair<String, User> = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()

        // 1. Pre-validations
        if (trimmedId.isEmpty()) {
            throw ApiException("Administrator Employee ID cannot be empty.")
        }
        if (password.isEmpty()) {
            throw ApiException("Password cannot be empty.")
        }
        if (password.length < 6) {
            throw ApiException("Password must be at least 6 characters.")
        }

        // 2. Mock Backend Mode
        if (Constants.useMockBackend) {
            delay(1200)
            // Let's validate if the employee is an actual Administrator
            if (trimmedId == "ADMIN" && password == "kscclkkd") {
                val adminUser = User(
                    employeeId = "ADMIN",
                    name = "HR Admin",
                    email = "admin@kscclsmartcity.gov.in",
                    phone = "9110223344",
                    role = "ADMIN",
                    joinedDate = "2023-11-01"
                )
                return@withContext Pair("mock_jwt_token_for_ADMIN", adminUser)
            } else if (trimmedId.startsWith("EMP")) {
                throw ApiException("Access Denied: This employee ID is registered as Standard Staff, not an HR Administrator.")
            } else {
                throw ApiException("Invalid Administrator credentials.")
            }
        }

        // 3. Real Backend REST mode
        val url = "${Constants.BASE_URL}auth/admin-login"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(bodyString).getString("error")
                    } catch (e: Exception) {
                        "HTTP admin login failed with status code ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }

                val responseJson = JSONObject(bodyString)
                val token = responseJson.getString("token")
                val userJson = responseJson.getJSONObject("user")

                val user = User(
                    employeeId = userJson.getString("employeeId"),
                    name = userJson.getString("name"),
                    email = userJson.optString("email", ""),
                    phone = userJson.optString("phone", ""),
                    role = userJson.getString("role"),
                    joinedDate = userJson.optString("joinedDate", ""),
                    department = userJson.optString("department", "Technical Support"),
                    designation = userJson.optString("designation", "Technical Executive"),
                    status = userJson.optString("status", "ACTIVE"),
                    faceRegistered = userJson.optBoolean("faceRegistered", true)
                )

                if (user.role != Constants.ROLE_ADMIN) {
                    throw ApiException("Access Denied: You do not hold HR Administrator rights.", 403)
                }

                return@withContext Pair(token, user)
            }
        } catch (e: IOException) {
            throw ApiException("Network connection failure. Verify your internet connection or check Flask server status.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error: ${e.localizedMessage}", null)
        }
    }

    /**
     * Handles new employee signup/registration.
     * Supports both real HTTP Flask API calls and local mock-mode simulation.
     */
    suspend fun signup(
        empId: String,
        name: String,
        email: String,
        phone: String,
        password: String
    ): Pair<String, User> = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        // 1. Strict Form Validations
        if (trimmedId.isEmpty()) throw ApiException("Employee ID is required.")
        if (trimmedName.isEmpty()) throw ApiException("Name is required.")
        
        if (trimmedEmail.isEmpty()) {
            throw ApiException("Gov / Work Email is required.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            throw ApiException("Please enter a valid email address.")
        }

        if (trimmedPhone.isEmpty()) {
            throw ApiException("Phone number is required.")
        }
        if (trimmedPhone.length != 10 || !trimmedPhone.all { it.isDigit() }) {
            throw ApiException("Phone number must be exactly 10 digits.")
        }

        if (password.length < 6) {
            throw ApiException("Password must be at least 6 characters.")
        }

        // 2. Mock Backend Mode
        if (Constants.useMockBackend) {
            delay(1500) // Simulate registration overhead

            if (com.example.data.mock.MockDataRepository.users.value.any { it.employeeId.uppercase() == trimmedId }) {
                throw ApiException("Registration conflict: Employee ID $trimmedId already has an active account.")
            }

            // Simulate mock successful enrollment
            val newUser = User(
                employeeId = trimmedId,
                name = trimmedName,
                email = trimmedEmail,
                phone = trimmedPhone,
                role = "EMPLOYEE",
                joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            
            // Add user to the shared repositories
            com.example.data.mock.MockDataRepository.addEmployee(newUser)
            com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId] = password

            return@withContext Pair("mock_jwt_token_for_$trimmedId", newUser)
        }

        // 3. Real Backend REST mode
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_SIGNUP}"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("name", trimmedName)
            put("email", trimmedEmail)
            put("phone", trimmedPhone)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(bodyString).getString("error")
                    } catch (e: Exception) {
                        "HTTP signup failed with status code ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }

                val responseJson = JSONObject(bodyString)
                val token = responseJson.getString("token")
                val userJson = responseJson.getJSONObject("user")

                val user = User(
                    employeeId = userJson.getString("employeeId"),
                    name = userJson.getString("name"),
                    email = userJson.optString("email", ""),
                    phone = userJson.optString("phone", ""),
                    role = userJson.getString("role"),
                    joinedDate = userJson.optString("joinedDate", ""),
                    department = userJson.optString("department", "Technical Support"),
                    designation = userJson.optString("designation", "Technical Executive"),
                    status = userJson.optString("status", "ACTIVE"),
                    faceRegistered = userJson.optBoolean("faceRegistered", true)
                )

                return@withContext Pair(token, user)
            }
        } catch (e: IOException) {
            throw ApiException("Network connection failure. Verify your internet connection or check Flask server status.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error during signup: ${e.localizedMessage}", null)
        }
    }

    /**
     * Changes standard employee password by self.
     */
    suspend fun changePassword(employeeId: String, oldPassword: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedId = employeeId.trim().uppercase()

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            throw ApiException("Passwords cannot be empty.")
        }
        if (newPassword.length < 6) {
            throw ApiException("New password must be at least 6 characters.")
        }

        if (Constants.useMockBackend) {
            delay(1000)
            val savedPassword = com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId]
            if (savedPassword != null && savedPassword != oldPassword) {
                throw ApiException("Incorrect old password.")
            } else if (savedPassword == null && oldPassword != "password123") {
                throw ApiException("Incorrect old password.")
            }

            com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId] = newPassword
            com.example.data.mock.MockDatabaseManager.saveCurrentState()
            return@withContext true
        }

        val url = "${Constants.BASE_URL}auth/change-password"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("old_password", oldPassword)
            put("new_password", newPassword)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(bodyString).getString("error")
                    } catch (e: Exception) {
                        "Failed to change password. Status code: ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }
                return@withContext true
            }
        } catch (e: IOException) {
            throw ApiException("Network connection failure. Verify your internet connection.", null)
        }
    }

    /**
     * Allows admin to change/override password of any standard employee.
     */
    suspend fun adminChangePassword(employeeId: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedId = employeeId.trim().uppercase()

        if (newPassword.isEmpty() || newPassword.length < 6) {
            throw ApiException("New password must be at least 6 characters.")
        }

        if (Constants.useMockBackend) {
            delay(1000)
            com.example.data.remote.AdminService.mockEmployeePasswords[trimmedId] = newPassword
            com.example.data.mock.MockDatabaseManager.saveCurrentState()
            return@withContext true
        }

        val url = "${Constants.BASE_URL}admin/change-employee-password"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("new_password", newPassword)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(bodyString).getString("error")
                    } catch (e: Exception) {
                        "Failed to change employee password. Status code: ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }
                return@withContext true
            }
        } catch (e: IOException) {
            throw ApiException("Network connection failure. Verify your internet connection.", null)
        }
    }
}

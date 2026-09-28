package com.example.data.remote

import com.example.core.Constants
import com.example.core.ApiException
import com.example.model.AdminDashboardStats
import com.example.model.TodayCheckin
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AdminService {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        val mockEmployees = mutableListOf<User>()
        val mockEmployeePasswords = mutableMapOf<String, String>()
    }

    private fun isSameCalendarDay(t1: Long, t2: Long, zone: java.util.TimeZone): Boolean {
        if (t1 <= 0 || t2 <= 0) return false
        val cal1 = java.util.Calendar.getInstance(zone).apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance(zone).apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    /**
     * Fetches dashboard statistics for HR Administrator.
     * Hits GET /admin/dashboard-stats
     */
    suspend fun getDashboardStats(): AdminDashboardStats = withContext(Dispatchers.IO) {
        if (Constants.useMockBackend) {
            delay(300) // Fast non-blocking response
            val kolkataZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            val todayKolkata = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                timeZone = kolkataZone
            }.format(java.util.Date())
            val todayLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            val allLogs = com.example.data.mock.MockDataRepository.attendanceLogs.value
            val todayLogs = allLogs.filter { log ->
                log.date == todayKolkata || log.date == todayLocal ||
                isSameCalendarDay(log.timestamp, System.currentTimeMillis(), kolkataZone) ||
                isSameCalendarDay(log.timestamp, System.currentTimeMillis(), java.util.TimeZone.getDefault())
            }

            val allUsers = com.example.data.mock.MockDataRepository.users.value.ifEmpty { mockEmployees }
            val actualEmployees = allUsers.filter { !it.role.equals("ADMIN", ignoreCase = true) }
            val totalCount = maxOf(actualEmployees.size, todayLogs.map { it.employeeId.uppercase() }.distinct().size)
            
            val presentCount = todayLogs.filter { 
                it.status.equals("PRESENT", ignoreCase = true) || 
                it.status.equals("LATE", ignoreCase = true) || 
                it.status.equals("PRESENTEE", ignoreCase = true) ||
                it.status.equals("ON_TIME", ignoreCase = true)
            }.map { it.employeeId.uppercase() }.distinct().size

            val absentCount = maxOf(0, totalCount - presentCount)
            val rate = if (totalCount > 0) (presentCount.toDouble() / totalCount.toDouble() * 100.0) else 0.0
            
            val checkins = todayLogs.map { log ->
                val empName = actualEmployees.find { it.employeeId.equals(log.employeeId, ignoreCase = true) }?.name
                    ?: log.employeeName.takeIf { !it.startsWith("Employee EMP", ignoreCase = true) }
                    ?: "Employee ${log.employeeId}"
                TodayCheckin(
                    empId = log.employeeId,
                    name = empName,
                    time = log.time,
                    status = log.status
                )
            }
            
            return@withContext AdminDashboardStats(
                totalEmployees = totalCount,
                presentToday = presentCount,
                absentToday = absentCount,
                attendanceRatePercent = rate,
                todayCheckins = checkins
            )
        }

        val url = "${Constants.BASE_URL}admin/dashboard-stats"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw ApiException("HTTP error code ${response.code} fetching admin stats", response.code)
                }

                val json = JSONObject(bodyString)
                val total = json.optInt("totalEmployees", 0)
                val present = json.optInt("presentToday", 0)
                val absent = json.optInt("absentToday", 0)
                val rate = json.optDouble("attendanceRatePercent", 0.0)

                val checkinsArr = json.optJSONArray("todayCheckins") ?: org.json.JSONArray()
                val checkinsList = mutableListOf<TodayCheckin>()
                for (i in 0 until checkinsArr.length()) {
                    val obj = checkinsArr.getJSONObject(i)
                    checkinsList.add(
                        TodayCheckin(
                            empId = obj.optString("empId", ""),
                            name = obj.optString("name", ""),
                            time = obj.optString("time", ""),
                            status = obj.optString("status", "")
                        )
                    )
                }

                return@withContext AdminDashboardStats(
                    totalEmployees = total,
                    presentToday = present,
                    absentToday = absent,
                    attendanceRatePercent = rate,
                    todayCheckins = checkinsList
                )
            }
        } catch (e: IOException) {
            throw ApiException("Could not retrieve admin stats due to network failure.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error fetching stats: ${e.localizedMessage}", null)
        }
    }

    /**
     * Fetches all employees.
     * Hits GET /admin/employees
     */
    suspend fun getEmployees(): List<User> = withContext(Dispatchers.IO) {
        if (Constants.useMockBackend) {
            delay(500)
            return@withContext mockEmployees.toList()
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) throw ApiException("Failed to fetch employees", response.code)
                val jsonArray = JSONObject(bodyString).getJSONArray("employees")
                val list = mutableListOf<User>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        User(
                            employeeId = obj.getString("employeeId"),
                            name = obj.getString("name"),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", ""),
                            role = obj.getString("role"),
                            joinedDate = obj.optString("joinedDate", "")
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            throw ApiException("Network Error: ${e.localizedMessage}", null)
        }
    }

    suspend fun addEmployee(user: User, password: String): User = withContext(Dispatchers.IO) {
        val trimmedId = user.employeeId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            mockEmployees.add(user.copy(employeeId = trimmedId))
            mockEmployeePasswords[trimmedId] = password
            com.example.data.mock.MockDataRepository.addEmployeeFromAdminService(user.copy(employeeId = trimmedId))
            com.example.data.mock.MockDatabaseManager.saveCurrentState()
            return@withContext user.copy(employeeId = trimmedId)
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}"
        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("name", user.name)
            put("email", user.email)
            put("phone", user.phone)
            put("role", user.role)
            put("password", password)
        }.toString()

        val request = Request.Builder().url(url).post(requestBodyJson.toRequestBody(jsonMediaType)).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("Failed to add employee", response.code)
                return@withContext user.copy(employeeId = trimmedId)
            }
        } catch (e: Exception) {
            throw ApiException("Network Error: ${e.localizedMessage}", null)
        }
    }

    suspend fun updateEmployee(empId: String, user: User): User = withContext(Dispatchers.IO) {
        val targetId = empId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            val index = mockEmployees.indexOfFirst { it.employeeId == targetId }
            if (index != -1) {
                mockEmployees[index] = user.copy(employeeId = targetId)
            }
            com.example.data.mock.MockDataRepository.updateEmployeeFromAdminService(user.copy(employeeId = targetId))
            com.example.data.mock.MockDatabaseManager.saveCurrentState()
            return@withContext user.copy(employeeId = targetId)
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}/$targetId"
        val requestBodyJson = JSONObject().apply {
            put("name", user.name)
            put("email", user.email)
            put("phone", user.phone)
            put("role", user.role)
        }.toString()

        val request = Request.Builder().url(url).put(requestBodyJson.toRequestBody(jsonMediaType)).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("Failed to update employee", response.code)
                return@withContext user.copy(employeeId = targetId)
            }
        } catch (e: Exception) {
            throw ApiException("Network Error: ${e.localizedMessage}", null)
        }
    }

    suspend fun deleteEmployee(empId: String): Boolean = withContext(Dispatchers.IO) {
        val targetId = empId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            mockEmployees.removeAll { it.employeeId == targetId }
            com.example.data.mock.MockDataRepository.deleteEmployeeFromAdminService(targetId)
            com.example.data.mock.MockDatabaseManager.saveCurrentState()
            return@withContext true
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}/$targetId"
        val request = Request.Builder().url(url).delete().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("Failed to delete employee", response.code)
                return@withContext true
            }
        } catch (e: Exception) {
            throw ApiException("Network Error: ${e.localizedMessage}", null)
        }
    }

    suspend fun getEmployeeList(): List<User> = getEmployees()

    suspend fun getMonthSummaryForAll(month: Int, year: Int, empId: String?): com.example.model.MonthSummary = withContext(Dispatchers.IO) {
        if (Constants.useMockBackend) {
            delay(1000)
            return@withContext com.example.model.MonthSummary(
                present = 20,
                absent = 11,
                holiday = 0,
                days = emptyMap()
            )
        }
        val queryId = if (empId.isNullOrEmpty() || empId == "ALL") null else empId
        var url = "${Constants.BASE_URL}admin/calendar-summary?month=$month&year=$year"
        if (queryId != null) url += "&emp_id=$queryId"

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) throw ApiException("Failed to fetch calendar summary", response.code)
                return@withContext com.example.model.MonthSummary(
                    present = 20,
                    absent = 11,
                    holiday = 0,
                    days = emptyMap()
                )
            }
        } catch (e: Exception) {
            throw ApiException("Network Error: ${e.localizedMessage}", null)
        }
    }
}

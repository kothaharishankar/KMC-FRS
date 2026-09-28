package com.example.data.remote

import android.util.Base64
import com.example.core.Constants
import com.example.core.ApiException
import com.example.model.AttendanceRecord
import com.example.model.MonthSummary
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
import java.util.Calendar
import java.util.TimeZone

class AttendanceService {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Marks employee attendance using face biometric coordinates and GPS coordinates.
     * Supports real HTTP Flask REST API calls and mock simulated offline-first processing.
     */
    suspend fun markAttendance(
        empId: String,
        imageBytes: ByteArray?,
        latitude: Double,
        longitude: Double
    ): AttendanceRecord = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        if (trimmedId.isEmpty()) {
            throw ApiException("Failed to mark attendance: Employee ID is missing.")
        }

        // 1. Mock Backend Switch Handling
        if (Constants.useMockBackend) {
            val kolkataZone = TimeZone.getTimeZone("Asia/Kolkata")
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = kolkataZone
            }.format(Date())
            val todayLocal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val alreadyMarked = com.example.data.mock.MockDataRepository.attendanceLogs.value.any {
                it.employeeId.equals(trimmedId, ignoreCase = true) && 
                (it.date == todayDate || it.date == todayLocal) &&
                (it.status.equals("PRESENT", ignoreCase = true) || it.status.equals("LATE", ignoreCase = true))
            }
            if (alreadyMarked) {
                throw ApiException("Attendance already marked for today!", null)
            }

            delay(1000) // Fast non-blocking delay
            val todayTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                timeZone = kolkataZone
            }.format(Date())
            
            // Determine late or on-time status based on current hour in Asia/Kolkata (Office starts at 10:30 AM)
            val kolkataCal = Calendar.getInstance(kolkataZone)
            val currentHour = kolkataCal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = kolkataCal.get(Calendar.MINUTE)
            val isLate = currentHour > 10 || (currentHour == 10 && currentMinute > 30)
            val lateBy = if (isLate) {
                (currentHour * 60 + currentMinute) - (10 * 60 + 30)
            } else null
            val lateDuration = if (isLate && lateBy != null) {
                val h = lateBy / 60
                val m = lateBy % 60
                if (h > 0) "${h} hr ${m} min late" else "${m} min late"
            } else null

            val realEmpName = com.example.data.mock.MockDataRepository.users.value.find { 
                it.employeeId.equals(trimmedId, ignoreCase = true) 
            }?.name ?: AdminService.mockEmployees.find { 
                it.employeeId.equals(trimmedId, ignoreCase = true) 
            }?.name ?: "Employee $trimmedId"

            val status = if (isLate) "LATE" else "PRESENT"
            val photoBase64 = imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            val record = AttendanceRecord(
                id = "ATT_${System.currentTimeMillis()}",
                employeeId = trimmedId,
                employeeName = realEmpName,
                date = todayDate,
                time = todayTime,
                status = status,
                latitude = latitude,
                longitude = longitude,
                locationName = "CCC kakinada",
                photoUrl = photoBase64,
                lateDuration = lateDuration,
                lateByMinutes = lateBy
            )
            return@withContext record
        }

        // 2. Real Mode Integration with Flask API
        val url = "${Constants.BASE_URL}attendance/mark"
        val base64Image = imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""

        val requestBodyJson = JSONObject().apply {
            put("emp_id", trimmedId)
            put("image", base64Image)
            put("lat", latitude)
            put("lng", longitude)
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
                        "Server failed with status code ${response.code}"
                    }
                    throw ApiException(errorMessage, response.code)
                }

                val responseJson = JSONObject(bodyString)
                val status = responseJson.optString("status", "PRESENT").uppercase()
                val kolkataZone = TimeZone.getTimeZone("Asia/Kolkata")
                val verifiedAt = responseJson.optString("verified_at", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = kolkataZone
                }.format(Date()))
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = kolkataZone
                }.format(Date())
                val photoBase64 = imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

                val calculatedLateBy = if (status == "LATE") {
                    calculateLateMinutes(verifiedAt) ?: 45
                } else null
                val calculatedLateDuration = if (calculatedLateBy != null) {
                    val h = calculatedLateBy / 60
                    val m = calculatedLateBy % 60
                    if (h > 0) "${h} hr ${m} min late" else "${m} min late"
                } else null

                return@withContext AttendanceRecord(
                    id = "ATT_${System.currentTimeMillis()}",
                    employeeId = trimmedId,
                    employeeName = "Employee $trimmedId",
                    date = todayDate,
                    time = verifiedAt,
                    status = status,
                    latitude = responseJson.optDouble("lat", latitude),
                    longitude = responseJson.optDouble("lng", longitude),
                    locationName = "CCC kakinada",
                    photoUrl = responseJson.optString("photo_url", photoBase64),
                    lateDuration = calculatedLateDuration,
                    lateByMinutes = calculatedLateBy
                )
            }
        } catch (e: IOException) {
            throw ApiException("Network error: Could not reach the server to mark attendance.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error marking attendance: ${e.localizedMessage}", null)
        }
    }

    /**
     * Checks if today's attendance is already marked for the employee.
     * Hits GET /attendance/today/:empId
     */
    suspend fun getTodayStatus(empId: String): AttendanceRecord? = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        if (trimmedId.isEmpty()) return@withContext null

        val kolkataZone = TimeZone.getTimeZone("Asia/Kolkata")

        if (Constants.useMockBackend) {
            delay(300) // Non-blocking fast response
            val todayKolkata = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = kolkataZone
            }.format(Date())
            val todayLocal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            return@withContext com.example.data.mock.MockDataRepository.attendanceLogs.value.find {
                it.employeeId.equals(trimmedId, ignoreCase = true) && 
                (it.date == todayKolkata || it.date == todayLocal)
            }
        }

        // Real Mode URL
        val url = "${Constants.BASE_URL}attendance/today/$trimmedId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw ApiException("HTTP error code ${response.code} fetching today's status", response.code)
                }

                val responseJson = JSONObject(bodyString)
                val isMarked = responseJson.optBoolean("marked", false)
                if (!isMarked) return@withContext null

                val status = responseJson.optString("status", "PRESENT").uppercase()
                val verifiedAt = responseJson.optString("verified_at", "09:00:00")
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = kolkataZone
                }.format(Date())

                val calculatedLateBy = if (status == "LATE") {
                    calculateLateMinutes(verifiedAt) ?: 45
                } else null
                val calculatedLateDuration = if (calculatedLateBy != null) {
                    val h = calculatedLateBy / 60
                    val m = calculatedLateBy % 60
                    if (h > 0) "${h} hr ${m} min late" else "${m} min late"
                } else null

                return@withContext AttendanceRecord(
                    id = "ATT_REAL_TODAY",
                    employeeId = trimmedId,
                    employeeName = responseJson.optString("name", "Employee $trimmedId"),
                    date = todayDate,
                    time = verifiedAt,
                    status = status,
                    latitude = responseJson.optDouble("lat", Constants.OFFICE_LATITUDE),
                    longitude = responseJson.optDouble("lng", Constants.OFFICE_LONGITUDE),
                    locationName = "CCC kakinada",
                    lateDuration = calculatedLateDuration,
                    lateByMinutes = calculatedLateBy
                )
            }
        } catch (e: IOException) {
            throw ApiException("Could not retrieve today's attendance status from the server.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error checking status: ${e.localizedMessage}", null)
        }
    }

    /**
     * Retrieves the list of historical attendance logs for an employee.
     * Hits GET /attendance/history/:empId
     */
    suspend fun getHistory(empId: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        if (trimmedId.isEmpty()) return@withContext emptyList<AttendanceRecord>()

        if (Constants.useMockBackend) {
            delay(1200) // delay to show pull-to-refresh spinner properly
            return@withContext generateMockHistory(trimmedId)
        }

        val url = "${Constants.BASE_URL}attendance/history/$trimmedId"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw ApiException("HTTP error code ${response.code} fetching history", response.code)
                }

                val jsonArray = org.json.JSONArray(bodyString)
                val list = mutableListOf<AttendanceRecord>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val status = obj.optString("status", "PRESENT").uppercase()
                    list.add(
                        AttendanceRecord(
                            id = obj.optString("id", "ATT_HIST_$i"),
                            employeeId = trimmedId,
                            employeeName = obj.optString("employee_name", "Employee $trimmedId"),
                            date = obj.optString("date", ""),
                            time = obj.optString("time", "--:--"),
                            status = status,
                            lateDuration = obj.optString("late_duration", null),
                            latitude = obj.optDouble("lat", 0.0),
                            longitude = obj.optDouble("lng", 0.0),
                            locationName = obj.optString("location_name", "CCC kakinada"),
                            photoUrl = obj.optString("photo_url", null)
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: IOException) {
            throw ApiException("Could not retrieve attendance history due to network failure.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error fetching history: ${e.localizedMessage}", null)
        }
    }

    /**
     * Retrieves monthly summary stats and day-by-day attendance status for an employee.
     * Hits GET /attendance/summary/:empId?month=M&year=YYYY
     */
    suspend fun getMonthSummary(empId: String, month: Int, year: Int): MonthSummary = withContext(Dispatchers.IO) {
        val trimmedId = empId.trim().uppercase()
        if (trimmedId.isEmpty()) {
            return@withContext MonthSummary(0, 0, 0, emptyMap())
        }

        if (Constants.useMockBackend) {
            delay(1000)
            return@withContext generateMockMonthSummary(trimmedId, month, year)
        }

        val url = "${Constants.BASE_URL}attendance/summary/$trimmedId?month=$month&year=$year"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw ApiException("HTTP error code ${response.code} fetching monthly summary", response.code)
                }

                val responseJson = JSONObject(bodyString)
                val present = responseJson.optInt("present", 0)
                val absent = responseJson.optInt("absent", 0)
                val holiday = responseJson.optInt("holiday", 0)

                val daysObject = responseJson.optJSONObject("days") ?: JSONObject()
                val daysMap = mutableMapOf<Int, String>()
                val keys = daysObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val dayInt = key.toIntOrNull()
                    if (dayInt != null) {
                        daysMap[dayInt] = daysObject.optString(key, "none").lowercase()
                    }
                }

                return@withContext MonthSummary(
                    present = present,
                    absent = absent,
                    holiday = holiday,
                    days = daysMap
                )
            }
        } catch (e: IOException) {
            throw ApiException("Could not retrieve calendar summary due to network failure.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error fetching calendar summary: ${e.localizedMessage}", null)
        }
    }

    private fun generateMockHistory(empId: String): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        val existing = com.example.data.mock.MockDataRepository.attendanceLogs.value.filter { it.employeeId == empId }
        list.addAll(existing)
        return list.sortedByDescending { it.date }
    }

    private fun generateMockMonthSummary(empId: String, month: Int, year: Int): MonthSummary {
        val daysMap = mutableMapOf<Int, String>()
        
        val maxDays = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        
        val logs = com.example.data.mock.MockDataRepository.attendanceLogs.value.filter { it.employeeId == empId }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        var presentCount = 0
        var absentCount = 0
        var holidayCount = 0

        for (day in 1..maxDays) {
            val dateStr = String.format("%04d-%02d-%02d", year, month, day)
            val dayLog = logs.find { it.date == dateStr }
            
            val status = when (dayLog?.status) {
                "PRESENT", "LATE" -> "present"
                "ABSENT" -> "absent"
                "HOLIDAY" -> "holiday"
                else -> "none"
            }
            
            daysMap[day] = status
            
            when (status) {
                "present" -> presentCount++
                "absent" -> absentCount++
                "holiday" -> holidayCount++
            }
        }
        
        return MonthSummary(
            present = presentCount,
            absent = absentCount,
            holiday = holidayCount,
            days = daysMap
        )
    }

    suspend fun getAllAttendance(searchQuery: String? = null): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        if (Constants.useMockBackend) {
            delay(1000)
            val list = generateMockAllAttendance()
            return@withContext if (!searchQuery.isNullOrBlank()) {
                list.filter {
                    it.employeeName.contains(searchQuery, ignoreCase = true) ||
                    it.employeeId.contains(searchQuery, ignoreCase = true) ||
                    it.date.contains(searchQuery, ignoreCase = true) ||
                    it.status.contains(searchQuery, ignoreCase = true) ||
                    it.locationName.contains(searchQuery, ignoreCase = true)
                }
            } else {
                list
            }
        }

        val baseUrl = "${Constants.BASE_URL}admin/attendance"
        val url = if (!searchQuery.isNullOrBlank()) {
            "$baseUrl?search=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
        } else {
            baseUrl
        }

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw ApiException("HTTP error code ${response.code} fetching all attendance", response.code)
                }

                val jsonArray = org.json.JSONArray(bodyString)
                val list = mutableListOf<AttendanceRecord>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val status = obj.optString("status", "PRESENT").uppercase()
                    list.add(
                        AttendanceRecord(
                            id = obj.optString("id", "ATT_ALL_$i"),
                            employeeId = obj.optString("employeeId", obj.optString("empId", "")),
                            employeeName = obj.optString("employeeName", obj.optString("name", "")),
                            date = obj.optString("date", ""),
                            time = obj.optString("time", "--:--"),
                            status = status,
                            lateDuration = obj.optString("late_duration", obj.optString("lateDuration", null)),
                            latitude = obj.optDouble("lat", obj.optDouble("latitude", 0.0)),
                            longitude = obj.optDouble("lng", obj.optDouble("longitude", 0.0)),
                            locationName = obj.optString("location_name", obj.optString("locationName", "CCC kakinada")),
                            photoUrl = obj.optString("photo_url", obj.optString("photoUrl", null)),
                            lateByMinutes = if (obj.has("lateByMinutes")) obj.optInt("lateByMinutes") else null
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: IOException) {
            throw ApiException("Could not retrieve attendance records due to network failure.", null)
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Unexpected error fetching attendance records: ${e.localizedMessage}", null)
        }
    }

    private fun generateMockAllAttendance(): List<AttendanceRecord> {
        return com.example.data.mock.MockDataRepository.attendanceLogs.value
    }

    private fun calculateLateMinutes(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase()
        var hour: Int? = null
        var minute: Int? = null

        try {
            if (clean.contains("AM") || clean.contains("PM")) {
                val format = SimpleDateFormat("hh:mm a", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                val date = format.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { time = date }
                    hour = cal.get(Calendar.HOUR_OF_DAY)
                    minute = cal.get(Calendar.MINUTE)
                }
            } else {
                val parts = clean.split(":")
                if (parts.size >= 2) {
                    hour = parts[0].toIntOrNull()
                    minute = parts[1].toIntOrNull()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (hour != null && minute != null) {
            val checkInMin = hour * 60 + minute
            val officeStartMin = 10 * 60 + 30 // 10:30 AM is 630 mins
            if (checkInMin > officeStartMin) {
                return checkInMin - officeStartMin
            }
        }
        return null
    }
}

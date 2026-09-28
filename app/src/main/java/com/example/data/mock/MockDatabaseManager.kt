package com.example.data.mock

import android.content.Context
import com.example.model.AttendanceRecord
import com.example.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object MockDatabaseManager {
    var appContext: Context? = null
    
    fun saveCurrentState() {
        // MockDataRepository auto-saves, but we keep this for compatibility
    }

    fun saveMockDataToPrefs(users: List<User>, passwords: Map<String, String>) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences("mock_db_prefs", Context.MODE_PRIVATE)
        val usersArray = JSONArray()
        users.forEach { u ->
            val obj = JSONObject()
            obj.put("employeeId", u.employeeId)
            obj.put("name", u.name)
            obj.put("email", u.email)
            obj.put("phone", u.phone)
            obj.put("role", u.role)
            obj.put("joinedDate", u.joinedDate)
            obj.put("department", u.department)
            obj.put("designation", u.designation)
            obj.put("status", u.status)
            obj.put("faceRegistered", u.faceRegistered)
            obj.put("password", u.password)
            usersArray.put(obj)
        }
        
        val passObj = JSONObject()
        passwords.forEach { (k, v) -> passObj.put(k, v) }
        
        prefs.edit()
            .putString("users", usersArray.toString())
            .putString("passwords", passObj.toString())
            .apply()
    }
    
    fun loadMockData(context: Context) {
        appContext = context.applicationContext
        MockDataRepository.initialize(context)
    }

    fun backupToSdcard(users: List<User>, attendance: List<AttendanceRecord>) {
        try {
            val root = JSONObject()
            
            val usersArray = JSONArray()
            users.forEach { u ->
                val obj = JSONObject()
                obj.put("employeeId", u.employeeId)
                obj.put("name", u.name)
                obj.put("email", u.email)
                obj.put("phone", u.phone)
                obj.put("role", u.role)
                obj.put("joinedDate", u.joinedDate)
                obj.put("department", u.department)
                obj.put("designation", u.designation)
                obj.put("status", u.status)
                obj.put("faceRegistered", u.faceRegistered)
                obj.put("password", u.password)
                usersArray.put(obj)
            }
            root.put("users", usersArray)

            val attArray = JSONArray()
            attendance.forEach { a ->
                val obj = JSONObject()
                obj.put("id", a.id)
                obj.put("employeeId", a.employeeId)
                obj.put("employeeName", a.employeeName)
                obj.put("date", a.date)
                obj.put("time", a.time)
                obj.put("status", a.status)
                obj.put("lateDuration", a.lateDuration ?: "")
                obj.put("latitude", a.latitude)
                obj.put("longitude", a.longitude)
                obj.put("locationName", a.locationName)
                obj.put("photoUrl", a.photoUrl ?: "")
                obj.put("lateByMinutes", a.lateByMinutes ?: -1)
                obj.put("faceConfidence", a.faceConfidence)
                obj.put("timestamp", a.timestamp)
                attArray.put(obj)
            }
            root.put("attendance_records", attArray)

            val downloadDir = File("/sdcard/Download")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val file = File(downloadDir, "ksccl_backup.json")
            file.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreFromSdcard(context: Context): Pair<List<User>, List<AttendanceRecord>>? {
        try {
            val file = File("/sdcard/Download/ksccl_backup.json")
            if (!file.exists()) return null
            val content = file.readText()
            val root = JSONObject(content)
            
            val usersList = mutableListOf<User>()
            val usersArray = root.optJSONArray("users")
            if (usersArray != null) {
                for (i in 0 until usersArray.length()) {
                    val obj = usersArray.getJSONObject(i)
                    usersList.add(User(
                        employeeId = obj.getString("employeeId"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        phone = obj.getString("phone"),
                        role = obj.getString("role"),
                        joinedDate = obj.getString("joinedDate"),
                        department = obj.optString("department", "Technical Support"),
                        designation = obj.optString("designation", "Technical Executive"),
                        status = obj.optString("status", "ACTIVE"),
                        faceRegistered = obj.optBoolean("faceRegistered", true),
                        password = obj.optString("password", "password123")
                    ))
                }
            }

            val attList = mutableListOf<AttendanceRecord>()
            val attArray = root.optJSONArray("attendance_records")
            if (attArray != null) {
                for (i in 0 until attArray.length()) {
                    val obj = attArray.getJSONObject(i)
                    val lateBy = obj.optInt("lateByMinutes", -1)
                    attList.add(AttendanceRecord(
                        id = obj.getString("id"),
                        employeeId = obj.getString("employeeId"),
                        employeeName = obj.getString("employeeName"),
                        date = obj.getString("date"),
                        time = obj.getString("time"),
                        status = obj.getString("status"),
                        lateDuration = obj.optString("lateDuration").takeIf { it.isNotEmpty() },
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        locationName = obj.getString("locationName"),
                        photoUrl = obj.optString("photoUrl").takeIf { it.isNotEmpty() },
                        lateByMinutes = if (lateBy == -1) null else lateBy,
                        faceConfidence = obj.optDouble("faceConfidence", 0.98),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
            }
            return Pair(usersList, attList)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

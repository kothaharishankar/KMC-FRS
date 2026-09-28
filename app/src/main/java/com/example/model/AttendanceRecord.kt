package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["employeeId"])],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["employeeId"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AttendanceRecord(
    @PrimaryKey val id: String = "",
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val time: String,
    val status: String, // "PRESENT", "LATE", "ABSENT", "HOLIDAY"
    val lateDuration: String? = null, // e.g. "1H 40M"
    val latitude: Double,
    val longitude: Double,
    val locationName: String, // e.g. "KSCCL Office, Kakinada" or "Remote"
    val photoUrl: String? = null,
    val lateByMinutes: Int? = null,
    val faceConfidence: Double = 0.98,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val clean = time.trim().uppercase()
        try {
            if (clean.contains("AM") || clean.contains("PM")) {
                return clean
            }
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null) {
                    val ampm = if (h >= 12) "PM" else "AM"
                    val displayHour = when {
                        h == 0 -> 12
                        h > 12 -> h - 12
                        else -> h
                    }
                    return String.format(java.util.Locale.US, "%02d:%02d %s", displayHour, m, ampm)
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return time
    }

    fun getFormattedLateDuration(): String {
        val minutes = lateByMinutes ?: run {
            val checkInMin = getMinutesFromTime(time)
            if (checkInMin != null && checkInMin > 630) { // 10:30 AM is 630 mins
                checkInMin - 630
            } else {
                null
            }
        } ?: return lateDuration ?: ""
        
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) {
            "${h} hr ${m} min late"
        } else {
            "${m} min late"
        }
    }

    private fun getMinutesFromTime(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase()
        try {
            if (clean.contains("AM") || clean.contains("PM")) {
                val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                }
                val date = format.parse(clean)
                if (date != null) {
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata")).apply { time = date }
                    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                }
            }
        } catch (e: java.lang.Exception) {}
        try {
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null) {
                    return h * 60 + m
                }
            }
        } catch (e: java.lang.Exception) {}
        return null
    }
}

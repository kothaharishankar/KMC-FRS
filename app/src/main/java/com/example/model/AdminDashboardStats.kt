package com.example.model

data class TodayCheckin(
    val empId: String,
    val name: String,
    val time: String,
    val status: String
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
}

data class AdminDashboardStats(
    val totalEmployees: Int,
    val presentToday: Int,
    val absentToday: Int,
    val attendanceRatePercent: Double,
    val todayCheckins: List<TodayCheckin>
)

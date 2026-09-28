package com.example.model

/**
 * Monthly attendance summary mapping stats and day-by-day calendar status states.
 */
data class MonthSummary(
    val present: Int,
    val absent: Int,
    val holiday: Int,
    val days: Map<Int, String> // maps day-of-month (1..31) to status string ("present", "absent", "holiday", "none")
)

package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class User(
    @PrimaryKey val employeeId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // EMPLOYEE or ADMIN
    val joinedDate: String,
    val department: String = "Technical Support",
    val designation: String = "Technical Executive",
    val status: String = "ACTIVE",
    val faceRegistered: Boolean = true,
    val password: String = "password123"
)

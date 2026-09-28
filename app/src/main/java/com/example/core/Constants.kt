package com.example.core

object Constants {
    const val APP_NAME = "KSCCL — Facial Attendance"
    const val CLIENT_NAME = "Kakinada Smart City Corporation Ltd."
    
    // Switch to use mock backend vs actual Flask backend REST API
    var useMockBackend = true
    
    // Roles
    const val ROLE_EMPLOYEE = "EMPLOYEE"
    const val ROLE_ADMIN = "ADMIN"
    
    // Shared Preferences / DataStore Keys
    const val PREFS_NAME = "ksccl_attendance_prefs"
    const val KEY_THEME_DARK = "theme_dark_mode"
    const val KEY_JWT_TOKEN = "jwt_auth_token"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USER_ID = "user_employee_id"
    const val KEY_USER_NAME = "user_display_name"
    
    // API Endpoints (Flask backend)
    const val BASE_URL = "https://api.kscclsmartcity.gov.in/v1/" // Placeholder for backend REST API
    const val ENDPOINT_LOGIN = "auth/login"
    const val ENDPOINT_SIGNUP = "auth/signup"
    const val ENDPOINT_VERIFY_FACE = "attendance/verify-face"
    const val ENDPOINT_ATTENDANCE_LOG = "attendance/log"
    const val ENDPOINT_EMPLOYEES = "admin/employees"
    
    // Location Verification (Kakinada Smart City Office Geofence)
    // Defaulting to KSCCL Office at Kakinada: Latitude: 16.989063, Longitude: 82.247192
    const val OFFICE_LATITUDE = 16.989063
    const val OFFICE_LONGITUDE = 82.247192
    const val GEOFENCE_RADIUS_METERS = 200.0 // 200m buffer
}

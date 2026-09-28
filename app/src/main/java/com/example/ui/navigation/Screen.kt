package com.example.ui.navigation

sealed class Screen {
    object Login : Screen()
    
    // Sub-screens for Employee Role
    sealed class EmployeeTab {
        object Home : EmployeeTab()
        object History : EmployeeTab()
        object Calendar : EmployeeTab()
        object Profile : EmployeeTab()
    }
    
    // Employee container
    data class EmployeeHome(val initialTab: EmployeeTab = EmployeeTab.Home) : Screen()

    // Sub-screens for Admin Role
    sealed class AdminTab {
        object Dashboard : AdminTab()
        object Users : AdminTab()
        object Attendance : AdminTab()
        object Calendar : AdminTab()
        object Reports : AdminTab()
    }
    
    // Admin container
    data class AdminHome(val initialTab: AdminTab = AdminTab.Dashboard) : Screen()
}

import re

with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "r") as f:
    text = f.read()

old_func = """    fun tryRestoreSession() {
        android.util.Log.d("APP_DEBUG", "tryRestoreSession started")
        viewModelScope.launch {
            authStatus.value = AuthStatus.UNKNOWN
            delay(1200) // Aesthetic delay for splash branding
            android.util.Log.d("APP_DEBUG", "tryRestoreSession delay finished")
            val (storedId, storedName, storedRole) = preferenceManager.getUserSession()
            if (storedId != null && storedName != null && storedRole != null) {
                val user = User(
                    employeeId = storedId,
                    name = storedName,
                    email = "",
                    phone = "",
                    role = storedRole,
                    joinedDate = ""
                )
                _currentUser.value = user
                authStatus.value = AuthStatus.AUTHENTICATED
                isAdmin.value = (storedRole == Constants.ROLE_ADMIN)
                _currentScreen.value = if (storedRole == Constants.ROLE_ADMIN) {
                    Screen.AdminHome(Screen.AdminTab.Dashboard)
                } else {
                    Screen.EmployeeHome(Screen.EmployeeTab.Home)
                }
                if (storedRole != Constants.ROLE_ADMIN) {
                    loadTodayStatus(storedId)
                }
                showBanner("Welcome Back ${user.name}!", true)
                showPopup("Welcome Back", "Welcome Back ${user.name}!", "SUCCESS")
            } else {
                android.util.Log.d("APP_DEBUG", "Setting UNAUTHENTICATED")
                authStatus.value = AuthStatus.UNAUTHENTICATED
                isAdmin.value = false
                _currentUser.value = null
                _currentScreen.value = Screen.Login
            }
        }
    }"""

new_func = """    fun tryRestoreSession() {
        viewModelScope.launch {
            try {
                authStatus.value = AuthStatus.UNKNOWN
                delay(1200)
                val (storedId, storedName, storedRole) = preferenceManager.getUserSession()
                if (storedId != null && storedName != null && storedRole != null) {
                    val user = User(
                        employeeId = storedId,
                        name = storedName,
                        email = "",
                        phone = "",
                        role = storedRole,
                        joinedDate = ""
                    )
                    _currentUser.value = user
                    authStatus.value = AuthStatus.AUTHENTICATED
                    isAdmin.value = (storedRole == Constants.ROLE_ADMIN)
                    _currentScreen.value = if (storedRole == Constants.ROLE_ADMIN) {
                        Screen.AdminHome(Screen.AdminTab.Dashboard)
                    } else {
                        Screen.EmployeeHome(Screen.EmployeeTab.Home)
                    }
                    if (storedRole != Constants.ROLE_ADMIN) {
                        loadTodayStatus(storedId)
                    }
                    showBanner("Welcome Back ${user.name}!", true)
                } else {
                    authStatus.value = AuthStatus.UNAUTHENTICATED
                    isAdmin.value = false
                    _currentUser.value = null
                    _currentScreen.value = Screen.Login
                }
            } catch (e: Exception) {
                authStatus.value = AuthStatus.UNAUTHENTICATED
                isAdmin.value = false
                _currentUser.value = null
                _currentScreen.value = Screen.Login
            }
        }
    }"""

text = text.replace(old_func, new_func)

with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "w") as f:
    f.write(text)

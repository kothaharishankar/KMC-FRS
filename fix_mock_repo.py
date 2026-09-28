import re

with open("app/src/main/java/com/example/data/mock/MockDataRepository.kt", "r") as f:
    text = f.read()

text = text.replace("    val attendanceLogs: StateFlow<List<AttendanceRecord>> = _attendanceLogs", "    val attendanceLogs: StateFlow<List<AttendanceRecord>> = _attendanceLogs\n\n    fun setUsers(list: List<User>) {\n        _users.value = list\n    }")

with open("app/src/main/java/com/example/data/mock/MockDataRepository.kt", "w") as f:
    f.write(text)

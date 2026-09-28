import re

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "r") as f:
    text = f.read()

text = text.replace("MockDataRepository.addEmployeeFromAdminService(user.copy(employeeId = trimmedId))\n            return@withContext user.copy(employeeId = trimmedId)", "MockDataRepository.addEmployeeFromAdminService(user.copy(employeeId = trimmedId))\n            com.example.data.mock.MockDatabaseManager.saveCurrentState()\n            return@withContext user.copy(employeeId = trimmedId)")

text = text.replace("MockDataRepository.updateEmployeeFromAdminService(user.copy(employeeId = targetId))\n            return@withContext user.copy(employeeId = targetId)", "MockDataRepository.updateEmployeeFromAdminService(user.copy(employeeId = targetId))\n            com.example.data.mock.MockDatabaseManager.saveCurrentState()\n            return@withContext user.copy(employeeId = targetId)")

text = text.replace("MockDataRepository.deleteEmployeeFromAdminService(targetId)\n            return@withContext true", "MockDataRepository.deleteEmployeeFromAdminService(targetId)\n            com.example.data.mock.MockDatabaseManager.saveCurrentState()\n            return@withContext true")

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "w") as f:
    f.write(text)

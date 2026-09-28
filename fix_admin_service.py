import re

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "r") as f:
    text = f.read()

text = text.replace("mockEmployeePasswords[trimmedId] = password\n            return@withContext user.copy(employeeId = trimmedId)", "mockEmployeePasswords[trimmedId] = password\n            com.example.data.mock.MockDataRepository.addEmployeeFromAdminService(user.copy(employeeId = trimmedId))\n            return@withContext user.copy(employeeId = trimmedId)")

text = text.replace("mockEmployees[index] = user.copy(employeeId = targetId)\n            }\n            return@withContext user.copy(employeeId = targetId)", "mockEmployees[index] = user.copy(employeeId = targetId)\n            }\n            com.example.data.mock.MockDataRepository.updateEmployeeFromAdminService(user.copy(employeeId = targetId))\n            return@withContext user.copy(employeeId = targetId)")

text = text.replace("mockEmployees.removeAll { it.employeeId == targetId }\n            return@withContext true", "mockEmployees.removeAll { it.employeeId == targetId }\n            com.example.data.mock.MockDataRepository.deleteEmployeeFromAdminService(targetId)\n            return@withContext true")

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "w") as f:
    f.write(text)

import re

with open("app/src/main/java/com/example/data/remote/AuthService.kt", "r") as f:
    text = f.read()

text = text.replace("mockEmployeePasswords[trimmedId] = newPassword\n            return@withContext true", "mockEmployeePasswords[trimmedId] = newPassword\n            com.example.data.mock.MockDatabaseManager.saveCurrentState()\n            return@withContext true")

with open("app/src/main/java/com/example/data/remote/AuthService.kt", "w") as f:
    f.write(text)

import re

with open("app/src/main/java/com/example/data/mock/MockDatabaseManager.kt", "r") as f:
    text = f.read()

text = text.replace("object MockDatabaseManager {", "object MockDatabaseManager {\n    var appContext: Context? = null\n    \n    fun saveCurrentState() {\n        val ctx = appContext ?: return\n        saveMockData(ctx, com.example.data.mock.MockDataRepository.users.value, com.example.data.remote.AdminService.mockEmployeePasswords)\n    }")

text = text.replace("fun loadMockData(context: Context) {", "fun loadMockData(context: Context) {\n        appContext = context.applicationContext")

with open("app/src/main/java/com/example/data/mock/MockDatabaseManager.kt", "w") as f:
    f.write(text)

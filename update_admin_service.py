import re

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "r") as f:
    text = f.read()

def mock(name):
    if name == 'get':
        return """    suspend fun getEmployees(): List<User> = withContext(Dispatchers.IO) {
        if (Constants.useMockBackend) {
            delay(500)
            return@withContext mockEmployees.toList()
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}""" + '"'
    elif name == 'add':
        return """    suspend fun addEmployee(user: User, password: String): User = withContext(Dispatchers.IO) {
        val trimmedId = user.employeeId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            mockEmployees.add(user.copy(employeeId = trimmedId))
            mockEmployeePasswords[trimmedId] = password
            return@withContext user.copy(employeeId = trimmedId)
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}""" + '"'
    elif name == 'update':
        return """    suspend fun updateEmployee(empId: String, user: User): User = withContext(Dispatchers.IO) {
        val targetId = empId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            val index = mockEmployees.indexOfFirst { it.employeeId == targetId }
            if (index != -1) {
                mockEmployees[index] = user.copy(employeeId = targetId)
            }
            return@withContext user.copy(employeeId = targetId)
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}/$targetId""" + '"'
    elif name == 'delete':
        return """    suspend fun deleteEmployee(empId: String): Boolean = withContext(Dispatchers.IO) {
        val targetId = empId.trim().uppercase()
        if (Constants.useMockBackend) {
            delay(500)
            mockEmployees.removeAll { it.employeeId == targetId }
            return@withContext true
        }
        val url = "${Constants.BASE_URL}${Constants.ENDPOINT_EMPLOYEES}/$targetId""" + '"'

text = re.sub(r'    suspend fun getEmployees\(\): List<User> = withContext\(Dispatchers\.IO\) {\s*val url = "\$\{Constants\.BASE_URL\}\$\{Constants\.ENDPOINT_EMPLOYEES\}"', mock('get'), text)
text = re.sub(r'    suspend fun addEmployee\(user: User, password: String\): User = withContext\(Dispatchers\.IO\) {\s*val trimmedId = user\.employeeId\.trim\(\)\.uppercase\(\)\s*val url = "\$\{Constants\.BASE_URL\}\$\{Constants\.ENDPOINT_EMPLOYEES\}"', mock('add'), text)
text = re.sub(r'    suspend fun updateEmployee\(empId: String, user: User\): User = withContext\(Dispatchers\.IO\) {\s*val targetId = empId\.trim\(\)\.uppercase\(\)\s*val url = "\$\{Constants\.BASE_URL\}\$\{Constants\.ENDPOINT_EMPLOYEES\}/\$targetId"', mock('update'), text)
text = re.sub(r'    suspend fun deleteEmployee\(empId: String\): Boolean = withContext\(Dispatchers\.IO\) {\s*val targetId = empId\.trim\(\)\.uppercase\(\)\s*val url = "\$\{Constants\.BASE_URL\}\$\{Constants\.ENDPOINT_EMPLOYEES\}/\$targetId"', mock('delete'), text)

with open("app/src/main/java/com/example/data/remote/AdminService.kt", "w") as f:
    f.write(text)

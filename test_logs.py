import re

with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("fun tryRestoreSession() {", "fun tryRestoreSession() {\n        android.util.Log.d(\"APP_DEBUG\", \"tryRestoreSession started\")")
text = text.replace("val (storedId, storedName, storedRole) = preferenceManager.getUserSession()", "android.util.Log.d(\"APP_DEBUG\", \"tryRestoreSession delay finished\")\n            val (storedId, storedName, storedRole) = preferenceManager.getUserSession()")
text = text.replace("authStatus.value = AuthStatus.UNAUTHENTICATED", "android.util.Log.d(\"APP_DEBUG\", \"Setting UNAUTHENTICATED\")\n                authStatus.value = AuthStatus.UNAUTHENTICATED")

with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "w") as f:
    f.write(text)

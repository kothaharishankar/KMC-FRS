import re
with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "r") as f:
    text = f.read()
if "tryRestoreSession()" in text:
    print("Found!")

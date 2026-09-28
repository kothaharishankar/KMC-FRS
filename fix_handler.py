import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = re.sub(r'Thread\.setDefaultUncaughtExceptionHandler \{ thread, throwable ->.*?\}', '// Default exception handler removed', text, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

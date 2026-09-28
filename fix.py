with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "r") as f:
    text = f.read()

text = text.replace("import com.example.R", "import com.aistudio.kscclattendance.pxqwyz.R")

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "w") as f:
    f.write(text)

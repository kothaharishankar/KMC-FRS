import re

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "r") as f:
    text = f.read()

text = text.replace(".width(180.dp)", ".fillMaxWidth()")

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "r") as f:
    app_text = f.read()

app_text = app_text.replace("KakinadaSmartCityLogo(\n                            modifier = Modifier,", "KakinadaSmartCityLogo(\n                            modifier = Modifier.width(180.dp),")

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "w") as f:
    f.write(app_text)

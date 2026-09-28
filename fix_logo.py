import re

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "r") as f:
    text = f.read()

# find KakinadaSmartCityLogo function
regex = r"(fun KakinadaSmartCityLogo.*?)(\.fillMaxWidth\(\))"

new_text = re.sub(regex, r"\1.width(180.dp)\n                .wrapContentHeight()", text, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "w") as f:
    f.write(new_text)

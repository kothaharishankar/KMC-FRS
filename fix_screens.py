import re
with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "r") as f:
    text = f.read()

replacement = """fun KakinadaSmartCityLogo(
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF0F172A),
    subtitleColor: Color = Color(0xFF64748B),
    showText: Boolean = true
) {
    com.example.ui.components.KakinadaSmartCityComposeLogo(modifier)
}"""

# Use regex to find and replace the whole function
pattern = r"fun KakinadaSmartCityLogo\(.*?showText: Boolean = true\n\)\s*\{.*?\n\}"
text = re.sub(pattern, replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "w") as f:
    f.write(text)

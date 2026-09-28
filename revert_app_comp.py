import re

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "r") as f:
    text = f.read()

replacement = """                    // Beautiful Kakinada Smart City custom Logo graphic
                    Box(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KakinadaSmartCityLogo(
                            modifier = Modifier,
                            showText = false
                        )
                    }"""

text = re.sub(r'                    // Beautiful Kakinada Smart City custom Logo graphic\s+Box\(\s+modifier = Modifier\s+\.padding\(bottom = 12\.dp\)\s+\.background\(Color\.White, shape = RoundedCornerShape\(12\.dp\)\)\s+\.padding\(horizontal = 16\.dp, vertical = 8\.dp\),\s+contentAlignment = Alignment\.Center\s+\) \{\s+KakinadaSmartCityLogo\(\s+modifier = Modifier,\s+showText = false\s+\)\s+\}', replacement, text)

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "w") as f:
    f.write(text)

import re

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "r") as f:
    text = f.read()

# We want to replace everything from the first "                    // Beautiful Kakinada Smart City custom Logo graphic"
# all the way down to "                        color = Color.White,"
# with the correct single block.

regex = r"                    // Beautiful Kakinada Smart City custom Logo graphic.*?                        color = Color\.White,"

replacement = r"""                    // Beautiful Kakinada Smart City custom Logo graphic
                    Box(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KakinadaSmartCityLogo(
                            modifier = Modifier,
                            showText = false
                        )
                    }
                    
                    Text(
                        text = Constants.APP_NAME,
                        color = Color.White,"""

new_text = re.sub(regex, replacement, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "w") as f:
    f.write(new_text)

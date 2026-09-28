import re

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "r") as f:
    content = f.read()

# Replace the messy repeated blocks with a single clean block
bad_block_start = "                    // Beautiful Kakinada Smart City custom Logo graphic"
bad_block_end = "                    // Beautiful Kakinada Smart City custom Logo graphic in a white rounded container"
# Wait, I'll just use a regex.

# We want to replace everything between "                Column(" and "                        color = Color.White," (exclusive)
# wait, actually between:
#                 Column(
#                     modifier = Modifier
#                         .fillMaxSize()
#                         .padding(24.dp),
#                     verticalArrangement = Arrangement.Center,
#                     horizontalAlignment = Alignment.CenterHorizontally
#                 ) {
#                   ... messy stuff ...
#                         color = Color.White,

regex = r"(verticalArrangement = Arrangement.Center,\s*horizontalAlignment = Alignment.CenterHorizontally\s*\} \{)(.*?)(                        color = Color\.White,)"

replacement = r"""\1
                    // Beautiful Kakinada Smart City custom Logo graphic
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
\3"""

new_content = re.sub(regex, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/components/AppComponents.kt", "w") as f:
    f.write(new_content)

import re

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "r") as f:
    text = f.read()

replacement = """    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Kakinada Smart City Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(180.dp)
                    .wrapContentHeight()
            )
        }"""
        
text = re.sub(r'    Column\(\s+horizontalAlignment = Alignment\.CenterHorizontally,\s+modifier = modifier\.padding\(vertical = 8\.dp\)\s+\) \{\s+Image\(\s+painter = painterResource\(id = R\.drawable\.logo\),\s+contentDescription = "Kakinada Smart City Logo",\s+contentScale = ContentScale\.Fit,\s+modifier = Modifier\s+\.width\(180\.dp\)\s+\.wrapContentHeight\(\)\s+\)', replacement, text)

with open("app/src/main/java/com/example/ui/screens/EmployeeScreens.kt", "w") as f:
    f.write(text)

import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup default uncaught exception handler for fatal crash telemetry logging
        // Default exception handler removed]:", throwable)
        }

        enableEdgeToEdge()
        // removed
        
        setContent {"""
replacement = """    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("delay(1200)", "")
with open("app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt", "w") as f:
    f.write(text)

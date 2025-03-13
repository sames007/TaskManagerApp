Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "cmd /c mvnw clean javafx:run", 0, False 